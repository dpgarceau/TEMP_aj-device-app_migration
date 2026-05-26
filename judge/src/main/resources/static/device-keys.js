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

    function digitFromEvent(event) {
        if (!event) {
            return null;
        }

        if (event.code && event.code.indexOf('Numpad') === 0) {
            return event.code.substring('Numpad'.length);
        }

        if (event.code && event.code.indexOf('Digit') === 0) {
            return event.code.substring('Digit'.length);
        }

        if (/^[0-9]$/.test(event.key)) {
            return event.key;
        }

        if (event.which >= 48 && event.which <= 57) {
            return String(event.which - 48);
        }

        if (event.which >= 96 && event.which <= 105) {
            return String(event.which - 96);
        }

        return null;
    }

    window.AeroJudgeDeviceKeys = {
        actionFromEvent: function (event) {
            // Judge buttons are edge-triggered: holding a GPIO key must not repeat score/navigation actions.
            if (event && event.repeat) {
                return null;
            }

            return digitActions[digitFromEvent(event)] || null;
        }
    };
})(window);
