# AeroJudge Device App

## Project Direction

This repository is being prepared to become the AeroJudge Device App: the judging application for current official AeroJudge Device hardware.

The forward path has two main goals:

1. Rename and re-version the application as AeroJudge Device App / AeroJudge Device so it is clearly distinct from other AeroJudge projects.
2. Align setup scripts, device configuration, and hardware support with the current physical AeroJudge Device product.

Changes will be made through focused branches and reviewed before merging to `main`. The intent is to avoid broad live edits on `main` and keep identity, release, setup, and hardware changes separated enough to review safely.

## Project Origin

AeroJudge Device App is a continuation of the former IMAC Judge App maintained at https://github.com/IMAC-ORG/imac-judge-app.

The AeroJudge repository starts from a clean import after package, build, script, and branding updates. Its transition point from the legacy repository is commit `8afe8c603a5140964fed9d7897ab8b53aea4d9b5` (`v2.1.2-rc3-38-g8afe8c6`).

## Legacy IMAC-ORG Archive Plan

The legacy IMAC-ORG code path should be preserved as an archive or mirror so it is not lost if the upstream repository changes or disappears.

That archive is for preservation and reference. This repository's `main` branch should not actively maintain legacy IMAC-ORG derived hardware support unless a specific future decision changes that policy.

Legacy builders and older hardware should continue to use the legacy IMAC-ORG code path. Current AeroJudge Device App work should target current official AeroJudge Device hardware.

## Device Setup

Current AeroJudge Device setup instructions are maintained in
[`scripts/README.md`](scripts/README.md). The current production setup path uses
`scripts/judge_setup.sh` for AeroJudge Device serial `DPG-110` and above with
PCB revision `3.61+`.


## Developer Environment

## Requirements

1. VSCode
2. Docker Desktop
3. Score >= v4.71 with services enabled and started
4. Java 17 or later (Java 21 works fine)

## Run AeroJudge Device App Locally

1. Open in a dev container
2. Open new terminal
3. Edit `/var/opt/judge/settings.json` with the correct Score host and port.
   The file is mounted from `.devcontainer/judge` and can be modified there
   instead.

```sh
sudo vi /var/opt/judge/settings.json

eg.
{
  "judge_id":1,
  "line_number":1,
  "score_host":"192.168.8.100",
  "score_http_port":80,
  "language":"en"
  "seasonYear":"26"
}
```

4. Run the app:

```sh
cd judge
./mvnw spring-boot:run
```

5. Open <http://localhost:8080> in a local browser.

## Build AeroJudge Device App

To build the application package:

```sh
cd judge
./mvnw clean package
```

Local Maven builds create a versioned JAR in `judge/target/`. Device installs
and release assets must use the runtime filename `judge.jar`.

For a manual device copy, rename the built JAR on the device:

```sh
cp judge/target/judge-26.1.0.jar /var/opt/judge/bin/judge.jar
```

GitHub release automation publishes stable asset names: `judge.jar`,
`figures.zip`, and `volume_service.zip`.

## Run AeroJudge Device App In Docker

1. Build the application package:

   ```sh
   cd judge
   ./mvnw clean package
   cd ..
   ```

2. Build and run the Docker image:

   ```sh
   docker build -t aerojudge-device-app:latest .
   docker run -p 8080:8080 aerojudge-device-app:latest
   ```

3. Open <http://localhost:8080> in a local browser.
