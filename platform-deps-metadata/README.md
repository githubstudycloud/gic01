# Dependency policy metadata

Policy (strict):
- Prefer dependencies released within the last 36 months.
- If a dependency is older, vendor it into `platform-thirdparty-repo/` and record why, risk, and a replacement plan.

Tools (opt-in governance checks):
- Dependency age audit:
  - PowerShell: `scripts/deps-age-audit.ps1`
  - Bash: `scripts/deps-age-audit.sh`
  - Exceptions file: `platform-deps-metadata/age-exceptions.json`

This folder is the place to store human-readable metadata (and/or machine-readable manifests) about
vendored dependencies, including:
- upstream source URL
- upstream release date
- license
- known CVEs / risk acceptance
- internal patch notes (if any)
- planned replacement target and timeline
