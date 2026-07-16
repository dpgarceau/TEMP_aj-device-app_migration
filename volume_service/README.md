# AeroJudge PCB 3.61+ Audio and Volume Services

This directory contains the hardware-specific audio stack for AeroJudge PCB
3.61+:

- PCM5122 DAC through the `hifiberry-dacplus` driver;
- TPA6132A2 amplifier enable on active-high GPIO8;
- Same Sky TS43 volume-down and volume-up contacts.

PCB 3.5 is not supported by this stack.

## Runtime Files

- Hardware helper: `/usr/local/sbin/aerojudge-audio-hardware`
- Hardware unit: `/etc/systemd/system/audio-hardware.service`
- Volume daemon: `/var/opt/volume_service/volume.py`
- Volume unit: `/etc/systemd/system/volume.service`
- Feedback tones: `/var/opt/volume_service/tone-*.wav`
- GPIO and DAC overlays: `/boot/config.txt`

The root-owned hardware helper is installed outside the `judge`-writable
volume directory.

## Hardware Lifecycle

`audio-hardware.service` keeps GPIO8 low while it waits for ALSA card
`sndrpihifiberry` and mixer `Digital`.

It checks every 3 seconds for up to 90 seconds. After an unsuccessful attempt,
it waits 30 seconds and retries until the DAC becomes available. On success it:

1. sets and unmutes `Digital` at 80%;
2. verifies the mixer;
3. raises and verifies GPIO8;
4. leaves GPIO8 high throughout normal operation.

On service stop or orderly shutdown it lowers GPIO8. GPIO8 is not toggled
between spoken clips.

## TS43 Volume Behavior

The volume daemon runs as `judge`, which already belongs to the `input` and
`audio` groups. Linux gpio-key overlays produce:

| Keycode | Function |
|---------|----------|
| 114 | Volume down |
| 115 | Volume up |

The configured PCB 3.61+ behavior is:

- startup: 80%;
- production minimum: 65%;
- maximum: 100%;
- normal step: 1%;
- hold/repeat behavior retained from the previous TS43 user experience.

The 65% minimum passed listening validation and is final unless future user
feedback requests a change.

## Audio Feedback

After each applied step, the daemon plays `tone-up.wav` or `tone-down.wav`.
The tone is played after changing the mixer, so its loudness represents the new
level. Directional feedback is retained at the configured limits.

Feedback uses the `judge` user's PulseAudio server:

```text
XDG_RUNTIME_DIR=/run/user/1000
PULSE_SERVER=unix:/run/user/1000/pulse/native
```

This allows feedback and Chromium speech to mix through the same HiFiBerry
sink. A feedback failure is logged but does not undo the mixer change.

## Dependencies

Fresh setup installs:

```sh
sudo apt install alsa-utils python3-evdev evtest
```

## Diagnostics

```sh
systemctl status audio-hardware.service volume.service
systemctl is-enabled audio-hardware.service volume.service
amixer -c sndrpihifiberry get Digital
raspi-gpio get 8
pactl info | grep -E 'Server Name|Default Sink'
journalctl -u audio-hardware.service -u volume.service -n 80 --no-pager
```

Inspect TS43 input events:

```sh
sudo evtest
```

Listening-dependent tone and Chromium mixing tests must be performed with a
listener available. Stop the hardware service to disable the amplifier:

```sh
sudo systemctl stop audio-hardware.service
```

## Installation and Updates

Fresh device setup installs and enables this stack but does not start it before
the required reboot. Existing `/var/opt/volume_service` installations are not
automatically replaced by application updates. Hardware-aware audio updates
require a separate compatibility design.

The old rotary encoder implementation remains in `volume_service_legacy/` for
reference only.
