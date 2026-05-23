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

# For AeroJudge setup instruction please take a look here
## https://github.com/AeroJudge/aerojudge-device-app/tree/main/scripts


# For developer environment please look below

## Requirements
1. VSCode
2. Docker Desktop
3. Score =>v4.70 with services enabled and started
4. Java 17 or later (Java 21 works fine)

## RUN AeroJudge Device App
1. Open in a dev container
2. Open new terminal
3. Edit the /var/opt/judge/settings.json file with correct host_ip and port that Score is running on. The file is mounted from the .devcontainer/judge folder and can be modified there instead.
```
sudo vi /var/opt/judge/settings.json

eg.
{
  "judge_id":1,
  "line_number":1,
  "score_host":"192.168.8.100",
  "score_http_port":80,
  "language":"en"
}
```
4. Running the app in the dev container
``` 
cd /workspace
mvn spring-boot:run
```
5. Connecting to the dev container can be done in your local browser http://locahost:8080

## Build AeroJudge Device App
1. To build the jar file to be deployed to the PI-SCORE unit.
```
cd judge/
./mvnw clean package
```
Binary {build}.jar located in judge/target folder needs to be copied to the device and extracted in /var/opt/judge/bin
Also {build}-figures.zip file needs to be copied to the device and extracted in /var/opt/judge

## RUN AeroJudge Device App as a docker container (very similar to running in device)
1. Build AeroJudge Device App using above instructions
2. Right-click Dockerfile in root folder and choose Build Image... and name tag aerojudge-device-app:latest
3. In terminal run: docker run -p 8080:8080 aerojudge-device-app:latest
4. Connecting to the image can be done in your local browser http://locahost:8080
