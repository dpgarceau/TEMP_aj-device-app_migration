(function (window) {
    'use strict';

    var digitActions = {
        '0': 'caller',
        '1': 'not-observed',
        '2': 'minus-half',
        '3': 'minus-one',
        '4': 'back',
        '5': 'zero',
        '6': 'next',
        '7': 'break',
        '8': 'plus-half',
        '9': 'plus-one'
    };
    var keyActions = {
        '.': 'deadline'
    };
    var pressedKeys = {};

    function eventSource(event) {
        return event && event.originalEvent ? event.originalEvent : event;
    }

    function legacyKeyCode(event) {
        return event.which || event.keyCode;
    }

    function keyFromEvent(event) {
        if (!event) {
            return null;
        }
        var source = eventSource(event);
        var keyCode = legacyKeyCode(event);

        if (source.code && /^Numpad[0-9]$/.test(source.code)) {
            return source.code.substring('Numpad'.length);
        }

        if (source.code && /^Digit[0-9]$/.test(source.code)) {
            return source.code.substring('Digit'.length);
        }

        if (/^[0-9]$/.test(source.key)) {
            return source.key;
        }

        if (keyCode >= 48 && keyCode <= 57) {
            return String(keyCode - 48);
        }

        if (keyCode >= 96 && keyCode <= 105) {
            return String(keyCode - 96);
        }

        if (source.code === 'Period' || source.code === 'NumpadDecimal' || source.key === '.') {
            return '.';
        }

        if (keyCode === 190 || keyCode === 110) {
            return '.';
        }

        return null;
    }

    function releaseFromEvent(event) {
        var key = keyFromEvent(event);
        if (key) {
            delete pressedKeys[key];
        }
    }

    window.addEventListener('keyup', releaseFromEvent, true);
    window.addEventListener('blur', function () {
        pressedKeys = {};
    });

    window.AeroJudgeDeviceKeys = {
        actionFromEvent: function (event) {
            var key = keyFromEvent(event);
            var source = eventSource(event);
            var action = digitActions[key] || keyActions[key];

            // Judge buttons are edge-triggered: holding a GPIO key must not repeat score/navigation actions.
            if (!action || pressedKeys[key] || (source && source.repeat)) {
                return null;
            }

            pressedKeys[key] = true;
            return action;
        }
    };
})(window);
