# Migration Note

AeroJudge Device App is a continuation of the former IMAC Judge App and PI-SCORE projects.

Original legacy repository:
https://github.com/IMAC-ORG/imac-judge-app

Legacy transition point:
`8afe8c603a5140964fed9d7897ab8b53aea4d9b5`
(`v2.1.2-rc3-38-g8afe8c6`)

This repository starts from a clean import after the AeroJudge Device App
package, build, script, and branding migration. The legacy repository history
was intentionally not copied into this repository to keep the new project
history readable.

## Project Direction

The AeroJudge Device App line gives the project room to support
AeroJudge-specific device hardware, including custom PCB revisions, while
keeping the legacy IMAC Judge App repository available for existing open 
source builders and earlier device setups.

The intent is to let the new AeroJudge device platform evolve without 
disrupting builders who depend on the legacy repository, scripts, hardware 
assumptions, or release flow.

## Primary Migration Changes

- Java package changed from `co.za.imac.judge` to `com.aerojudge.judge`.
- Maven coordinates changed to the AeroJudge namespace.
- Device setup and update scripts changed to fetch from `AeroJudge/aerojudge-device-app`.
- Runtime display naming changed to `AeroJudge Device`.
- Repository and build documentation changed to `AeroJudge Device App`.

## Versioning

AeroJudge Device App uses calendar-based release numbers:

```text
YY.MAJOR.MINOR
```

The first AeroJudge Device App release is `26.1.0`.

- `YY` is the two-digit release year.
- `MAJOR` is incremented for major AeroJudge Device App releases within that year.
- `MINOR` is incremented for smaller fixes or updates within that major release.

GitHub release tags should use the same version with a leading `v`, such as
`v26.1.0`.

The legacy IMAC Judge App repository may continue independently after this
transition point.
