# AeroJudge Release Versioning

AeroJudge Device App uses release versions in this format:

```text
YY.MAJOR.MINOR
```

GitHub release tags use the same version with a leading `v`:

```text
v26.1.0
```

The release tag, GitHub release name, and packaged JAR implementation version
must match.

## Daily Auto Releases

Merges to `main` can create automatic releases when they change releasable
paths:

```text
pom.xml
judge/
figures/
volume_service/
```

The root `pom.xml` selects the active release line. For example:

```xml
<version>26.1.0</version>
```

selects the `v26.1.x` release line.

The tag workflow finds the latest existing tag in that line and increments the
last number:

```text
v26.1.0 -> v26.1.1 -> v26.1.2
```

If no tag exists for that release line, the workflow creates the exact version
from `pom.xml`:

```text
pom.xml version 26.1.0 -> first tag v26.1.0
```

## Changing Release Lines

Humans choose year and major release-line changes. The workflow does not guess
whether a change should be a new year or a new major line.

To start the next yearly line, update the root `pom.xml` and `judge/pom.xml`
base versions before merging to `main`:

```xml
<version>27.1.0</version>
```

The next releasable merge starts the new line:

```text
v27.1.0
```

To start another major line within the same year, use the same process:

```xml
<version>26.2.0</version>
```

The next releasable merge starts:

```text
v26.2.0
```

## Build-Time Version Alignment

The release build rewrites the Maven versions in the checked-out workflow copy
to match the release tag before packaging.

For example, if `pom.xml` selects the `26.1.x` line and the auto-generated tag
is:

```text
v26.1.3
```

the build sets Maven to:

```text
26.1.3
```

before running `mvn clean package`. This keeps the packaged JAR version,
`/api/info`, the GitHub tag, and the GitHub release aligned.

This rewrite happens only inside the GitHub Actions workspace. It does not
commit version changes back to `main`.

## Manual Releases

The `Create Release` workflow can still be run manually with a specific tag,
but the tag must already exist and must use the same stable version format:

```text
vYY.MAJOR.MINOR
```

Examples:

```text
v26.1.0
v26.2.0
v27.1.0
```
