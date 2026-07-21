# Migration Note

This repository begins from a clean import of the IMAC Judge App codebase after
the AeroJudge Device App rebrand and package migration.

Original legacy repository:

```text
https://github.com/IMAC-ORG/imac-judge-app
```

Transition point:

```text
8afe8c603a5140964fed9d7897ab8b53aea4d9b5
v2.1.2-rc3-38-g8afe8c6
```

The legacy repository may continue independently after this transition point.

Primary identity migration changes:

- Java package changed from `co.za.imac.judge` to `com.aerojudge.judge`.
- Maven coordinates changed to the AeroJudge namespace.
- Repository references changed to `AeroJudge/aerojudge-device-app`.
- User-facing product naming standardized on `AeroJudge Device App` for the
  application/repository and `AeroJudge Device` for the runtime device product.
- Legacy IMAC repository history is intentionally not copied into the final
  AeroJudge repository so the new ecosystem starts with clean history.

Versioning, release automation, current hardware setup, device config, volume
service behavior, and legacy archive/mirror policy are handled in separate
reviewed migration tracks.
