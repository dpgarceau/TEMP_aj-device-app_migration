# AeroJudge TS43 Volume Service

This is the active volume service for official AeroJudge Device PCB v3.5x / v3.51 hardware.

The board uses a Same Sky TS43 thumbwheel switch. GPIO contacts are configured as Linux `gpio-key` overlays in `/boot/config.txt`; this service listens for the resulting key events with `evdev` and controls ALSA volume with `amixer`.

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

The service plays short WAV tones with `aplay` after volume changes because the kiosk UI does not show a volume level indicator.

| File | Purpose |
|------|---------|
| `tone-up.wav` | Volume increased |
| `tone-down.wav` | Volume decreased |
| `tone-limit.wav` | Volume already at minimum or maximum |

The files are packaged with the service and can be tested directly:

```bash
aplay /var/opt/volume_service/tone-up.wav
aplay /var/opt/volume_service/tone-down.wav
aplay /var/opt/volume_service/tone-limit.wav
```

## Dependencies

Fresh AeroJudge Device setup installs:

```bash
sudo apt install python3-evdev
```

`amixer` is provided by ALSA utilities on the target Raspberry Pi OS image. The service expects the mixer control to be named `Master`.

## Diagnostics

Verify input events:

```bash
sudo apt install evtest
sudo evtest
```

Verify audio control:

```bash
amixer get 'Master'
```

Follow service logs:

```bash
sudo journalctl -u volume -f
```

## Legacy Service

The old rotary encoder service is preserved in `volume_service_legacy/`. It is not packaged as the active service for new AeroJudge Device App releases.
