# AeroJudge TS43 Volume Service

This is the active volume service for official AeroJudge Device PCB v3.5x / v3.51 hardware.

The board uses a Same Sky TS43 thumbwheel switch. GPIO contacts are configured as Linux `gpio-key` overlays in `/boot/config.txt`; this service listens for the resulting `button@...` key events with `evdev` and controls ALSA volume with `amixer`.

## Runtime Files

- Service script: `/var/opt/volume_service/volume.py`
- systemd unit: `/etc/systemd/system/volume.service`
- GPIO mapping: `/boot/config.txt`
- Audio feedback tones: `/var/opt/volume_service/tone-*.wav`

## Keycodes

| Keycode | Function |
|---------|----------|
| 114 | Volume down |
| 115 | Volume up |
| 159 | Volume down fast, when enabled |
| 160 | Volume up fast, when enabled |
| 113 | Mute toggle, when enabled |

The current production setup enables slow volume down/up only. Fast volume and mute GPIO lines are present but commented in `scripts/device_setup.sh` until those controls are approved for the production default.

## Audio Feedback

The service plays short WAV tones with `aplay` after volume button presses because the kiosk UI does not show a volume level indicator. At the minimum and maximum, it repeats the same directional tone instead of using a separate limit tone.

| File | Purpose |
|------|---------|
| `tone-up.wav` | Volume increased |
| `tone-down.wav` | Volume decreased |
The files are packaged with the service and can be tested directly:

```bash
aplay /var/opt/volume_service/tone-up.wav
aplay /var/opt/volume_service/tone-down.wav
```

## Dependencies

Fresh AeroJudge Device setup installs:

```bash
sudo apt install python3-evdev
```

`amixer` is provided by ALSA utilities on the target Raspberry Pi OS image. The service runs as root and expects the root/system mixer control to be named `PCM`.
The configured range is 80-100%. The dedicated PCB audio amp path is effectively inaudible below 80% during device testing, and the root PCM mixer reports 100% as its hardware maximum.
At boot, the service waits briefly for this mixer to appear and systemd restarts it after transient startup failures.

## Diagnostics

Verify input events:

```bash
sudo apt install evtest
sudo evtest
```

Verify audio control:

```bash
sudo amixer scontrols
sudo amixer get 'PCM'
```

Follow service logs:

```bash
sudo journalctl -u volume -f
```

## Legacy Service

The old rotary encoder service is preserved in `volume_service_legacy/`. It is not packaged as the active service for new AeroJudge Device App releases.
