# Legacy Rotary Volume Service

This is the previous rotary encoder volume service. It is preserved for older legacy hardware and rollback, but it is not the active service for TS43 thumbwheel devices.

The active service lives in `volume_service/`.

## Runtime Assumptions

- Rotary encoder on BCM GPIO 15 and 18
- Python `RPi.GPIO`
- ALSA `PCM` mixer control
- systemd unit name: `volume.service`
