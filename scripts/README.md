# AeroJudge Device SD Card Setup

These instructions are for current AeroJudge Device hardware only:

- serial numbers above `DPG-100`
- PCB revision `3.5x` or newer
- Raspberry Pi OS Bullseye 32-bit Desktop
- user `judge`

The previous scripts README is preserved as `scripts/README_legacy.md`.

## Current Status

The fresh setup script has been bench-tested from the
`test/fresh-device-setup` branch on a Bullseye 32-bit device.

Validated:

- setup script completed through reboot
- kiosk opened AeroJudge at `newcomp.html`
- `judge.service`, `kiosk.service`, and `volume.service` were active/enabled
- `http://<pi-ip>:8080` loaded from another machine
- app health returned `{"status":"UP"}`
- Score connectivity and test sync worked
- root/system `PCM` mixer was available with `sudo amixer get PCM`
- volume service monitored TS43 input devices
- GPIO scoring buttons
- volume thumbwheel physical behavior
- audio through the device amp
- shutdown/poweroff behavior

## Preferred Production Direction

The setup process should move to an AeroJudge-prepared Bullseye base image.
That image should be stored outside the repo, with a download link and SHA256
recorded here when available.

The base image should include:

- known-good Raspberry Pi OS Bullseye 32-bit Desktop
- `judge` user and password
- SSH enabled
- WiFi configured for `AeroJudgeNET`
- desktop update prompts disabled
- desktop Wastebasket hidden
- `/home/judge/build_aerojudge.sh`, a small launcher that fetches and runs
  the current `judge_setup.sh`

The base image should not include app install state:

- no `/var/opt/judge`
- no `/var/opt/volume_service`
- no `/home/judge/.judge_last_release`
- no `/boot/config.txt.before-aerojudge`

This keeps OS/bootstrap work repeatable while still allowing `judge_setup.sh`
to pull the latest setup logic, boot config, services, `judge.jar`, figures, and
volume service from the release flow.

## Current Manual/Test Path

Until the prepared base image exists, start from the official Raspberry Pi OS
Bullseye 32-bit Desktop image for Raspberry Pi hardware.

Important: do not use the "Raspberry Pi Desktop" image marked compatible with
PC and Mac. That is not the Raspberry Pi device image.

The known tested image was:

```text
2025-05-06-raspios-bullseye-armhf.img.xz
```

Official download path used during testing:

```text
https://downloads.raspberrypi.com/raspios_oldstable_armhf/images/raspios_oldstable_armhf-2025-05-07/2025-05-06-raspios-bullseye-armhf.img.xz
```

Configure first-boot access so the device can be reached by SSH as:

```text
user: judge
password: P@ssword1234$
WiFi SSID: AeroJudgeNET
WiFi password: 2Pr1v@TE
```

Current devices have a screen but no keyboard, so first-boot access must be
solved before the setup script can run. A repeatable Phase 0 helper or base
image is still needed.

## Run Setup

Production branch:

```sh
cd /home/judge
./build_aerojudge.sh
```

Test branch:

```sh
cd /home/judge
export AEROJUDGE_RAW_BASE_URL="https://raw.githubusercontent.com/<owner>/<repo>/refs/heads/<branch>"
export AEROJUDGE_RELEASE_API_URL="https://api.github.com/repos/<owner>/<repo>/releases/latest"
./build_aerojudge.sh
```

The launcher exports those values before running `judge_setup.sh`, so the setup
script, update fetcher, and release installer all use the same test endpoints.

The setup script will:

- verify Bullseye, 32-bit, and user `judge`
- refuse to run if existing AeroJudge install state is found
- expand the root filesystem to use the available SD card space
- install required packages
- install the AeroJudge boot config to `/boot/config.txt`
- create `/var/opt/judge/settings.json`
- create `/var/opt/judge/pilots/scores`
- install `judge.service`, `kiosk.service`, and `fetch_update.sh`
- install the AeroJudge desktop image
- hide the desktop Wastebasket
- disable PackageKit desktop update prompts
- fetch and install release assets
- validate services, release assets, audio assets, runtime directories, and the
  root/system `PCM` mixer
- ask whether to reboot

## Settings

Fresh setup writes settings directly to:

```text
/var/opt/judge/settings.json
```

The old flow used `/boot/settings.json`. Do not use that location for current
fresh device setup.

Current setup prompts for `seasonYear` because existing release tags do not yet
encode the season year. Future releases are expected to use `vYY.MAJOR.MINOR`
tags, for example `v26.1.0`; once that versioning is active, setup should derive
`seasonYear` from the release tag and validate it against figure package
metadata instead of prompting.

Default setup values:

- `judge_id`: `1`
- `line_number`: `1`
- `score_host`: `192.168.8.100`
- `score_http_port`: `80`
- `language`: `en`
- `score_poll_timeout`: `2`
- `score_timeout`: `10`

## Post-Reboot Validation

Run:

```sh
systemctl is-active judge.service kiosk.service volume.service
systemctl is-enabled judge.service kiosk.service volume.service
curl -s http://localhost:8080/actuator/health
sudo amixer get PCM
journalctl -u volume.service -n 40 --no-pager
ls -ld /var/opt/judge/pilots /var/opt/judge/pilots/scores
cat /var/opt/judge/settings.json
```

Then validate on assembled device hardware:

- kiosk opens AeroJudge at `newcomp.html`
- another machine can load `http://<pi-ip>:8080`
- Score connectivity and test sync work
- GPIO scoring buttons perform the expected actions
- volume thumbwheel changes volume
- audio is audible through the device amp
- shutdown/poweroff behavior works

## Known Follow-Ups

- First-boot access needs a repeatable base-image or Phase 0 provisioning helper.
- `attempt_auto_sync_scores` is currently re-added to `settings.json` by the
  app after startup. That is an app storage bug, not a setup-script setting.
- Season year should become release-derived once `vYY.MAJOR.MINOR` release
  tags and figure package metadata are in place.
