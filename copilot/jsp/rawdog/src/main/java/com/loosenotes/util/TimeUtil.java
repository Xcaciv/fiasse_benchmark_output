package com.loosenotes.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeUtil() {
    }

    public static String format(LocalDateTime timestamp) {
        return timestamp == null ? "-" : timestamp.format(DISPLAY_FORMAT);
    }
}
