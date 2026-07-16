# AeroJudge Device SD Card Setup

These instructions are for current AeroJudge Device hardware:

- serial numbers above `DPG-110`
- PCB revision `3.61`
- Raspberry Pi 4B

The supported installation path uses the prepared AeroJudge base image. 

## Install a Device

1. Download the current AeroJudge Device image from:

   <https://aero-judge.com/device-images.php>

2. Use Raspberry Pi Imager to write the downloaded image to the SD card.

3. Insert the card into the AeroJudge Device and boot it.

4. Connect to the device as user `judge`, then run:

   ```sh
   cd /home/judge
   ./build_aerojudge.sh
   ```

The launcher downloads and runs the current production `judge_setup.sh`. The
setup asks for device-specific settings, installs the application and services,
validates the result, and offers to reboot.

## Test an Unreleased Setup Script

To test the setup code from another repository or branch, override both source
endpoints before running the launcher:

```sh
cd /home/judge
export AEROJUDGE_RAW_BASE_URL="https://raw.githubusercontent.com/<owner>/<repo>/refs/heads/<branch>"
export AEROJUDGE_RELEASE_API_URL="https://api.github.com/repos/<owner>/<repo>/releases/latest"
./build_aerojudge.sh
```

For this repository's `test/fresh-device-setup` branch:

```sh
cd /home/judge
export AEROJUDGE_RAW_BASE_URL="https://raw.githubusercontent.com/dpgarceau/TEMP_aj-device-app_migration/refs/heads/test/fresh-device-setup"
export AEROJUDGE_RELEASE_API_URL="https://api.github.com/repos/dpgarceau/TEMP_aj-device-app_migration/releases/latest"
./build_aerojudge.sh
```

The launcher exports those values before running `judge_setup.sh`, so the setup
script, update fetcher, and release installer use the same test endpoints.
The release endpoint must contain a `volume_service.zip` built from the same
PCB 3.61 implementation; changing only the raw branch URL does not change
release assets.

## What Setup Installs

The setup script:

- verifies Bullseye, 32-bit, and user `judge`
- refuses to run if existing AeroJudge install state is found
- expands the root filesystem to use the available SD card space
- installs required packages
- installs the AeroJudge boot configuration
- creates `/var/opt/judge/settings.json`
- creates `/var/opt/judge/pilots/scores`
- installs `judge.service`, `kiosk.service`, and `fetch_update.sh`
- installs and enables `audio-hardware.service` and `volume.service`
- installs the AeroJudge desktop image
- hides the desktop Wastebasket
- disables PackageKit desktop update prompts
- fetches and installs release assets
- validates services, release assets, audio assets, runtime directories, and
  the PCB 3.61+ boot configuration
- asks whether to reboot

## Settings

Fresh setup writes settings directly to:

```text
/var/opt/judge/settings.json
```

The old flow used `/boot/settings.json`; do not use that location for current
fresh-device setup.

Setup currently prompts for `seasonYear` because existing release tags do not
encode the season year. Future `vYY.MAJOR.MINOR` release tags should allow setup
to derive it and validate it against figure-package metadata.

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
systemctl is-active judge.service kiosk.service audio-hardware.service volume.service
systemctl is-enabled judge.service kiosk.service audio-hardware.service volume.service
curl -s http://localhost:8080/actuator/health
amixer -c sndrpihifiberry get Digital
raspi-gpio get 8
pactl info | grep -E 'Server Name|Default Sink'
journalctl -u audio-hardware.service -u volume.service -n 80 --no-pager
ls -ld /var/opt/judge/pilots /var/opt/judge/pilots/scores
cat /var/opt/judge/settings.json
```

Then validate on assembled hardware:

- kiosk opens AeroJudge at `newcomp.html`
- another machine can load `http://<pi-ip>:8080`
- Score connectivity and test sync work
- GPIO scoring buttons perform the expected actions
- volume thumbwheel changes volume
- audio is audible through the device amplifier
- shutdown/poweroff behavior works

## Known Follow-Ups

- `attempt_auto_sync_scores` is currently re-added to `settings.json` by the
  app after startup. That is an app storage bug, not a setup-script setting.
- `seasonYear` should become release-derived once `vYY.MAJOR.MINOR` release
  tags and figure-package metadata are in place.
