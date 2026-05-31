# AeroJudge Device SD Card Build

These instructions describe the current supported fresh SD card path for
AeroJudge Device App devices.

The current device setup path is Raspberry Pi OS Bullseye using
`scripts/judge_setup.sh`.

This setup is for AeroJudge devices with serial numbers above `DPG-100` and PCB
revision `3.5x` or newer.

For the previous project README, see `README_legacy.md`.

## Build A Fresh Device Card

1. Flash Raspberry Pi OS 32-bit Desktop Bullseye with Raspberry Pi Imager.

2. In Raspberry Pi Imager advanced settings, configure:

   - Username: `judge`
   - Password: `P@ssword1234$`
   - SSH: enabled
   - WiFi SSID: `AeroJudgeNET`
   - WiFi password: `2Pr1v@TE`
   - Locale, keyboard, and timezone for the device location

3. Boot the Raspberry Pi from the new SD card.

4. SSH into the Pi as `judge`.

5. Download and run the AeroJudge setup script:

   ```sh
   cd /home/judge
   wget -O judge_setup.sh https://raw.githubusercontent.com/AeroJudge/aerojudge-device-app/main/scripts/judge_setup.sh
   chmod +x judge_setup.sh
   ./judge_setup.sh
   ```

6. Confirm the hardware target when prompted.

7. Enter the season year when prompted.

   The setup script defaults these values:

   - Judge ID: `1`
   - Flight Line: `1`
   - Score IP: `192.168.8.100`
   - Score Port: `80`

8. Let the setup script complete. It installs system packages, applies the
   AeroJudge boot configuration, installs the service files, and runs the update
   fetcher to install the current release assets.

9. When prompted, choose whether to reboot immediately. A reboot is required
   before the boot configuration and hardware services are fully active.

## Testing A Branch Before Merge

When testing an unmerged setup branch, download the script from that branch and
point the script at the same raw branch URL:

```sh
cd /home/judge
export AEROJUDGE_RAW_BASE_URL="https://raw.githubusercontent.com/<owner>/<repo>/<branch>"
wget -O judge_setup.sh "$AEROJUDGE_RAW_BASE_URL/scripts/judge_setup.sh"
chmod +x judge_setup.sh
./judge_setup.sh
```

## What The Setup Script Installs

The current `scripts/judge_setup.sh` flow:

- verifies the install is running on Bullseye, 32-bit, as user `judge`
- exits if existing AeroJudge install state is found
- installs required OS packages, including `python3-evdev` and `evtest`
- preserves `/boot/config.txt` as `/boot/config.txt.before-aerojudge`
- installs the AeroJudge boot configuration to `/boot/config.txt`
- creates `/var/opt/judge/settings.json`
- installs `judge.service`
- installs `kiosk.service`
- installs `/home/judge/fetch_update.sh`
- installs the AeroJudge desktop image
- runs `/home/judge/fetch_update.sh --download-only`
- runs `/home/judge/fetch_update.sh --install`
- validates required release assets, audio assets, and service enablement

The update flow installs release assets into `/var/opt/judge`, including:

- `judge.jar`
- `figures.zip`
- `volume_service.zip`

## Validation Checklist

After the reboot, validate the card on the actual device hardware:

- AeroJudge opens in the local kiosk.
- `http://<pi-ip>:8080` loads from another machine on the network.
- `/var/opt/judge/settings.json` contains the expected judge, line, Score, and
  season settings.
- GPIO buttons perform the correct scoring actions.
- Volume thumbwheel controls audio volume.
- Audio playback works.
- Shutdown and poweroff behavior works.
- Score connectivity works.
- A test sync to Score succeeds.

## Notes

- This process is for the current Bullseye-based device setup.
- For the previous project README, see `README_legacy.md`.
- The older manual setup sections in `scripts/README.md` are stale and should
  not be used as the primary source for a fresh card.
- Settings moved from `/boot/settings.json` to
  `/var/opt/judge/settings.json`. Some values can be adjusted through admin
  screens; full editing is available over SSH.
