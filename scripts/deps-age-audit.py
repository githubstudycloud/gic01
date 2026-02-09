#!/usr/bin/env python3
"""
Dependency age policy check for the platform repo.

Policy goal (strict design, but opt-in execution):
- Prefer third-party dependencies released within the last N years (default: 3).
- If older, vendor the artifact into platform-thirdparty-repo/ (file-based Maven repo),
  so the platform can be self-maintained / air-gapped when needed.

This script is intentionally lightweight (stdlib-only) and repository-local.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
import urllib.request
from email.utils import parsedate_to_datetime
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")

# Example line (the tail after scope may include module info / ANSI codes):
#   org.slf4j:slf4j-api:jar:2.0.17:compile -- module ...
COORD_RE = re.compile(
    r"^\s*"
    r"(?P<group>[A-Za-z0-9_.-]+):"
    r"(?P<artifact>[A-Za-z0-9_.-]+):"
    r"(?P<type>[A-Za-z0-9_.-]+):"
    r"(?P<version>[A-Za-z0-9_.+-]+):"
    r"(?P<scope>[A-Za-z0-9_.-]+)"
)


@dataclass(frozen=True, order=True)
class Gav:
    group: str
    artifact: str
    version: str

    @property
    def ga(self) -> str:
        return f"{self.group}:{self.artifact}"

    @property
    def gav(self) -> str:
        return f"{self.group}:{self.artifact}:{self.version}"


def _read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, obj: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _strip_ansi(s: str) -> str:
    return ANSI_RE.sub("", s)


def _run(cmd: list[str], cwd: Path) -> None:
    # Stream output directly so failures are obvious.
    if os.name == "nt" and (not cmd or not cmd[0].lower().endswith(".exe")):
        # Maven is commonly installed as mvn.cmd on Windows; CreateProcess cannot execute .cmd directly.
        cmd = ["cmd.exe", "/c", *cmd]
    proc = subprocess.run(cmd, cwd=str(cwd))
    if proc.returncode != 0:
        raise SystemExit(proc.returncode)


def _central_timestamp_ms(gav: Gav, cache: dict[str, int | None], timeout_s: int = 15) -> int | None:
    key = gav.gav
    if key in cache and cache[key] is not None:
        return cache[key]

    # Prefer Maven Central's artifact HTTP metadata (Last-Modified of the POM).
    # This avoids relying on search index freshness.
    group_path = "/".join(gav.group.split("."))
    pom_url = (
        "https://repo1.maven.org/maven2/"
        f"{group_path}/{gav.artifact}/{gav.version}/{gav.artifact}-{gav.version}.pom"
    )
    req = urllib.request.Request(
        pom_url, method="HEAD", headers={"User-Agent": "platform-deps-age-audit/1.0"}
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout_s) as resp:
            last_modified = resp.headers.get("Last-Modified")
            if not last_modified:
                cache[key] = None
                return None
            ts = int(parsedate_to_datetime(last_modified).timestamp() * 1000)
    except Exception:
        cache[key] = None
        return None

    cache[key] = ts
    # Be polite; this can be run in CI too.
    time.sleep(0.05)
    return ts


def _vendor_paths(vendor_repo: Path, gav: Gav) -> tuple[Path, Path]:
    group_path = Path(*gav.group.split("."))
    base = vendor_repo / group_path / gav.artifact / gav.version
    jar = base / f"{gav.artifact}-{gav.version}.jar"
    pom = base / f"{gav.artifact}-{gav.version}.pom"
    return jar, pom


def _load_exceptions(path: Path, today: dt.date) -> tuple[set[str], set[str]]:
    if not path.exists():
        return set(), set()

    allowed_ga: set[str] = set()
    allowed_gav: set[str] = set()

    data = _read_json(path)
    if isinstance(data, dict):
        # Support a few shapes to keep the file ergonomic.
        if isinstance(data.get("allowedGa"), list):
            allowed_ga.update(str(x) for x in data["allowedGa"])
        if isinstance(data.get("allowedGav"), list):
            allowed_gav.update(str(x) for x in data["allowedGav"])
        allowed = data.get("allowed")
        if isinstance(allowed, list):
            for item in allowed:
                if isinstance(item, str):
                    if item.count(":") == 1:
                        allowed_ga.add(item)
                    elif item.count(":") == 2:
                        allowed_gav.add(item)
                elif isinstance(item, dict):
                    until = item.get("until")
                    if isinstance(until, str):
                        try:
                            until_d = dt.date.fromisoformat(until)
                            if today > until_d:
                                continue
                        except ValueError:
                            pass
                    ga = item.get("ga")
                    gav = item.get("gav")
                    if isinstance(ga, str) and ga.count(":") == 1:
                        allowed_ga.add(ga)
                    if isinstance(gav, str) and gav.count(":") == 2:
                        allowed_gav.add(gav)
    elif isinstance(data, list):
        for item in data:
            item_s = str(item)
            if item_s.count(":") == 1:
                allowed_ga.add(item_s)
            elif item_s.count(":") == 2:
                allowed_gav.add(item_s)

    return allowed_ga, allowed_gav


def _find_local_repo(mvn_cmd: str, repo_root: Path) -> Path:
    # Ask Maven for its localRepository; avoids guessing per machine.
    cmd = [
        mvn_cmd,
        "-q",
        "help:evaluate",
        "-Dexpression=settings.localRepository",
        "-DforceStdout",
    ]
    if os.name == "nt" and not mvn_cmd.lower().endswith(".exe"):
        cmd = ["cmd.exe", "/c", *cmd]
    proc = subprocess.run(cmd, cwd=str(repo_root), capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or "failed to detect Maven localRepository")
    return Path(proc.stdout.strip()).expanduser()


def _local_repo_artifacts(local_repo: Path, gav: Gav) -> tuple[Path, Path]:
    group_path = Path(*gav.group.split("."))
    base = local_repo / group_path / gav.artifact / gav.version
    jar = base / f"{gav.artifact}-{gav.version}.jar"
    pom = base / f"{gav.artifact}-{gav.version}.pom"
    return jar, pom


def _collect_deps(repo_root: Path, deps_file_name: str) -> set[Gav]:
    deps: set[Gav] = set()
    for p in repo_root.rglob(deps_file_name):
        if p.parent.name != "target":
            continue
        try:
            content = p.read_text(encoding="utf-8", errors="ignore").splitlines()
        except OSError:
            continue
        for raw in content:
            line = _strip_ansi(raw).strip()
            m = COORD_RE.match(line)
            if not m:
                continue
            if m.group("type") != "jar":
                continue
            deps.add(Gav(m.group("group"), m.group("artifact"), m.group("version")))
    return deps


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent

    parser = argparse.ArgumentParser(description="Check the platform dependency age policy (3-year rule).")
    parser.add_argument("--mvn", default="mvn", help="Maven command (default: mvn)")
    parser.add_argument("--max-age-years", type=int, default=3, help="Maximum dependency age in years (default: 3)")
    parser.add_argument(
        "--include-transitive",
        action="store_true",
        help="Check transitive dependencies too (default: direct dependencies only)",
    )
    parser.add_argument(
        "--vendor-repo",
        default=str(repo_root / "platform-thirdparty-repo"),
        help="Vendor repo path (default: platform-thirdparty-repo)",
    )
    parser.add_argument(
        "--exceptions",
        default=str(repo_root / "platform-deps-metadata" / "age-exceptions.json"),
        help="Exceptions file (default: platform-deps-metadata/age-exceptions.json)",
    )
    parser.add_argument(
        "--cache",
        default=str(repo_root / "target" / "maven-central-timestamps.json"),
        help="Central timestamp cache path (default: target/maven-central-timestamps.json)",
    )
    parser.add_argument(
        "--skip-group-prefix",
        default="com.test.platform",
        help="Skip checking dependencies with this groupId prefix (default: com.test.platform)",
    )
    parser.add_argument(
        "--vendor",
        action="store_true",
        help="Auto-vendor violating dependencies from local ~/.m2 into platform-thirdparty-repo/",
    )
    args = parser.parse_args()

    today = dt.date.today()
    max_age_days = args.max_age_years * 365

    vendor_repo = Path(args.vendor_repo).resolve()
    exceptions_path = Path(args.exceptions).resolve()
    cache_path = Path(args.cache).resolve()

    allowed_ga, allowed_gav = _load_exceptions(exceptions_path, today)
    cache: dict[str, int | None] = {}
    if cache_path.exists():
        try:
            cache_raw = _read_json(cache_path)
            if isinstance(cache_raw, dict):
                for k, v in cache_raw.items():
                    cache[k] = int(v) if v is not None else None
        except Exception:
            cache = {}

    deps_file_name = "platform-deps-age-audit.txt"
    output_rel = f"target/{deps_file_name}"

    # Clean old outputs (stale inputs are worse than slow execution).
    for p in repo_root.rglob(deps_file_name):
        if p.parent.name == "target":
            try:
                p.unlink()
            except OSError:
                pass

    mvn_cmd: list[str] = [
        args.mvn,
        "-q",
        "-DskipTests",
        "-Dstyle.color=never",
        "package",
        "dependency:list",
        "-DincludeScope=runtime",
        f"-DexcludeTransitive={'false' if args.include_transitive else 'true'}",
        "-DexcludeReactor=false",
        f"-DoutputFile={output_rel}",
    ]
    _run(mvn_cmd, repo_root)

    deps_all = _collect_deps(repo_root, deps_file_name)
    deps = {d for d in deps_all if not d.group.startswith(args.skip_group_prefix)}

    if not deps:
        print("No third-party dependencies found to check.")
        return 0

    local_repo: Path | None = None
    if args.vendor:
        local_repo = _find_local_repo(args.mvn, repo_root)

    violations: list[str] = []
    unknown: list[str] = []
    vendored: list[str] = []

    for gav in sorted(deps):
        if gav.ga in allowed_ga or gav.gav in allowed_gav:
            continue

        ts = _central_timestamp_ms(gav, cache)
        if ts is None:
            unknown.append(gav.gav)
            continue

        release_date = dt.datetime.fromtimestamp(ts / 1000, tz=dt.timezone.utc).date()
        age_days = (today - release_date).days
        if age_days <= max_age_days:
            continue

        jar_dst, pom_dst = _vendor_paths(vendor_repo, gav)
        if jar_dst.exists() and pom_dst.exists():
            vendored.append(f"{gav.gav} (vendored)")
            continue

        if args.vendor and local_repo is not None:
            jar_src, pom_src = _local_repo_artifacts(local_repo, gav)
            if not jar_src.exists() or not pom_src.exists():
                violations.append(
                    f"{gav.gav} released {release_date.isoformat()} ({age_days}d) - "
                    f"NOT in local repo ({jar_src.name}/{pom_src.name})"
                )
                continue
            jar_dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(jar_src, jar_dst)
            shutil.copy2(pom_src, pom_dst)
            vendored.append(f"{gav.gav} (auto-vendored)")
            continue

        violations.append(
            f"{gav.gav} released {release_date.isoformat()} ({age_days}d) - vendor missing: "
            f"{os.path.relpath(jar_dst, repo_root)}"
        )

    _write_json(cache_path, cache)

    print("")
    print(f"Checked third-party deps: {len(deps)}")
    print(f"Max age: {args.max_age_years} years ({max_age_days} days)")
    if args.include_transitive:
        print("Mode: transitive (full closure)")
    else:
        print("Mode: direct (excludeTransitive=true)")

    if vendored:
        print("")
        print("Vendored:")
        for line in vendored:
            print(f"- {line}")

    if unknown:
        print("")
        print("Unknown on Maven Central (manual check required):")
        for line in unknown:
            print(f"- {line}")

    if violations:
        print("")
        print("Violations (older than policy and not vendored):")
        for line in violations:
            print(f"- {line}")
        print("")
        print("To vendor one, copy jar+pom into platform-thirdparty-repo/ using the same Maven layout,")
        print("or re-run with: --vendor")
        return 2

    if unknown:
        # Unknown artifacts should be treated as policy failures until explicitly accepted.
        print("")
        print("Failing due to unknown release dates. Add exceptions or vendor + record metadata.")
        return 3

    print("")
    print("OK: dependency age policy satisfied.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
