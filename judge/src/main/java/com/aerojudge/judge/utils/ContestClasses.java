package com.aerojudge.judge.utils;

import java.util.Arrays;
import java.util.List;

/**
 * IMAC contest class ordering. Single source of truth for the order classes
 * appear in admin UI and detection output.
 *
 * <p><b>Migration anchor:</b> when the score server starts returning classes
 * in a defined order rather than relying on this hardcoded list, change
 * {@link #ORDER} from a {@code static final} constant to a runtime accessor
 * (likely backed by {@code CompService}). Every consumer picks up the new
 * source automatically.
 *
 * <p>FREESTYLE is intentionally absent from {@link #ORDER}; it sorts to the
 * end via {@link #orderIndex(String)} alongside null and unknown values.
 */
public final class ContestClasses {

    public static final List<String> ORDER = Arrays.asList(
        "BASIC", "SPORTSMAN", "INTERMEDIATE", "ADVANCED", "UNLIMITED", "INVITATIONAL"
    );

    private ContestClasses() {
    }

    /**
     * Case-insensitive position lookup. Returns {@code ORDER.size()} for null,
     * unknown, or FREESTYLE so they sort to the end.
     */
    public static int orderIndex(String className) {
        if (className == null) {
            return ORDER.size();
        }
        int idx = ORDER.indexOf(className.toUpperCase());
        return idx == -1 ? ORDER.size() : idx;
    }
}
