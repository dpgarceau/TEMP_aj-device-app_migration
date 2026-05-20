# Legacy Rotary Volume Service

This directory preserves the old rotary-encoder-based volume service for legacy boards and transition reference.

New AeroJudge Device PCB v3.5x / v3.51 hardware uses the active service in `volume_service/`, which listens to `gpio-key` events from the TS43 thumbwheel switch.

Do not package this directory as the default volume service for new AeroJudge Device App releases.
