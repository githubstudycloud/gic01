# Namespace renaming (groupId / base package)

Pre-release constraint: this repo uses a placeholder namespace (`com.test.platform`).
Before publishing, you can rename coordinates and packages in one shot.

## Rename groupId and Java base package

From repo root:

```powershell
./scripts/rename-namespace.ps1 -NewGroupId com.yourco.platform
```

By default the script also renames the Java base package to the same value.

If you want different values (not recommended), specify `-NewBasePackage`:

```powershell
./scripts/rename-namespace.ps1 -NewGroupId com.yourco.platform -NewBasePackage com.yourco.foundation
```

## What the script changes

- `groupId` in `pom.xml` (root only; modules inherit)
- All occurrences in `pom.xml`, `*.java`, `*.md`, `*.properties`, `*.yml`, `*.yaml`, `*.toml`, `*.gradle*`, `*.kts`
- Moves Java source folders under `src/main/java` and `src/test/java` from the old package path to the new package path

