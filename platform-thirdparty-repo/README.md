# platform-thirdparty-repo

This directory is a file-based Maven repository used to vendor dependencies that violate the "3-year"
age policy (or require internal patching / air-gapped builds).

Recommended layout: standard Maven repository structure:

```
platform-thirdparty-repo/
  com/example/foo/1.2.3/foo-1.2.3.jar
  com/example/foo/1.2.3/foo-1.2.3.pom
  ...
```

The build config adds this repo first (via `platform-parent`) so vendored artifacts win resolution.

