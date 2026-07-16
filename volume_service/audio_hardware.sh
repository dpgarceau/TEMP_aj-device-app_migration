#!/bin/sh

set -eu

GPIO=8
ALSA_CARD="sndrpihifiberry"
MIXER_CONTROL="Digital"
STARTUP_VOLUME="80%"
PROBE_COUNT=30
PROBE_INTERVAL=3
RETRY_INTERVAL=30

keep_amplifier_enabled=0

log() {
    printf '%s\n' "audio-hardware: $*"
}

gpio_state() {
    /usr/bin/raspi-gpio get "$GPIO"
}

drive_amplifier_low() {
    /usr/bin/raspi-gpio set "$GPIO" op dl
    state=$(gpio_state)
    case "$state" in
        *"level=0"*"func=OUTPUT"*)
            log "GPIO$GPIO amplifier enable is output-low"
            ;;
        *)
            log "ERROR: failed to verify GPIO$GPIO output-low: $state" >&2
            return 1
            ;;
    esac
}

drive_amplifier_high() {
    /usr/bin/raspi-gpio set "$GPIO" op dh
    state=$(gpio_state)
    case "$state" in
        *"level=1"*"func=OUTPUT"*)
            log "GPIO$GPIO amplifier enable is output-high"
            ;;
        *)
            log "ERROR: failed to verify GPIO$GPIO output-high: $state" >&2
            return 1
            ;;
    esac
}

dac_is_ready() {
    /usr/bin/amixer -c "$ALSA_CARD" get "$MIXER_CONTROL" >/dev/null 2>&1
}

initialize_mixer() {
    /usr/bin/amixer -c "$ALSA_CARD" sset "$MIXER_CONTROL" "$STARTUP_VOLUME" unmute >/dev/null
    state=$(/usr/bin/amixer -c "$ALSA_CARD" get "$MIXER_CONTROL")

    case "$state" in
        *"[$STARTUP_VOLUME]"*"[on]"*)
            log "verified $ALSA_CARD/$MIXER_CONTROL at $STARTUP_VOLUME and unmuted"
            ;;
        *)
            log "ERROR: failed to verify $ALSA_CARD/$MIXER_CONTROL at $STARTUP_VOLUME and unmuted" >&2
            printf '%s\n' "$state" >&2
            return 1
            ;;
    esac
}

cleanup() {
    if [ "$keep_amplifier_enabled" -ne 1 ]; then
        drive_amplifier_low >/dev/null 2>&1 || true
    fi
}

handle_signal() {
    keep_amplifier_enabled=0
    cleanup
    trap - EXIT HUP INT TERM
    exit 1
}

start_hardware() {
    trap cleanup EXIT
    trap handle_signal HUP INT TERM
    drive_amplifier_low

    attempt=1
    while :; do
        log "DAC initialization attempt $attempt started"

        probe=1
        while [ "$probe" -le "$PROBE_COUNT" ]; do
            if dac_is_ready; then
                log "DAC mixer became available on probe $probe/$PROBE_COUNT"
                initialize_mixer
                drive_amplifier_high
                log "audio hardware initialized"
                keep_amplifier_enabled=1
                return 0
            fi

            if [ "$probe" -lt "$PROBE_COUNT" ]; then
                sleep "$PROBE_INTERVAL"
            fi
            probe=$((probe + 1))
        done

        log "ERROR: DAC unavailable after $PROBE_COUNT probes; retrying in ${RETRY_INTERVAL}s" >&2
        sleep "$RETRY_INTERVAL"
        attempt=$((attempt + 1))
    done
}

stop_hardware() {
    drive_amplifier_low
    log "audio hardware disabled"
}

case "${1:-}" in
    start)
        start_hardware
        ;;
    stop)
        stop_hardware
        ;;
    *)
        printf 'Usage: %s {start|stop}\n' "$0" >&2
        exit 2
        ;;
esac
