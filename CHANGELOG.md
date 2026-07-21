# Changelog

This changelog begins with the AeroJudge Device App at v26.1.0

The previous changelog was not consistently maintained and has been preserved
as `CHANGELOG_legacy.md`. Do not reconstruct old release history here from
memory. Historical project context is documented in `MIGRATION.md` and the
legacy repository.

## Unreleased

- Prepared AeroJudge Device App identity, package namespace, Maven coordinates,
  setup documentation, and release automation for the new repository load.
- Fixed the local Docker image build path so the container runs the packaged
  app as `judge.jar`, matching device runtime behavior.
- Fixed release workflow handoff so non-release pushes do not create failed
  release runs.
