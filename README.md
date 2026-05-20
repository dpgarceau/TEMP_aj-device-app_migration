# AeroJudge Device App

# For AeroJudge Device App setup instructions, please take a look here
## https://github.com/AeroJudge/aerojudge-device-app/tree/main/scripts

## Project Origin

AeroJudge Device App is a continuation of the former IMAC Judge App maintained at https://github.com/IMAC-ORG/imac-judge-app.

This repository starts from a clean migration path after package, build, script, and branding updates. Its transition point from the legacy repository is commit `8afe8c603a5140964fed9d7897ab8b53aea4d9b5` (`v2.1.2-rc3-38-g8afe8c6`).


# For developer environment please look below

## Requirements
1. VSCode
2. Docker Desktop
3. Score =>v4.70 with services enabled and started
4. Java 17 or later (Java 21 works fine)

## RUN aerojudge-device-app
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

## Build aerojudge-device-app
1. To build the jar file to be deployed to the AeroJudge device.
```
cd judge/
./mvnw clean package
```
Binary {build}.jar located in judge/target folder needs to be copied to the device and extracted in /var/opt/judge/bin
Also {build}-figures.zip file needs to be copied to the device and extracted in /var/opt/judge

## RUN aerojudge-device-app as a docker container (very similar to running in device)
1. Build aerojudge-device-app using above instructions
2. Right-click Dockerfile in root folder and choose Build Image... and name tag aerojudge-device-app:latest
3. In terminal run: docker run -p 8080:8080 aerojudge-device-app:latest
4. Connecting to the image can be done in your local browser http://locahost:8080
