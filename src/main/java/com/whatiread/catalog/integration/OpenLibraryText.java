package com.whatiread.catalog.integration;

import java.util.Map;

public final class OpenLibraryText {

    private OpenLibraryText() {
    }

    public static String extract(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : text.trim();
        }
        if (value instanceof Map<?, ?> map) {
            Object nested = map.get("value");
            if (nested instanceof String text) {
                return text.isBlank() ? null : text.trim();
            }
        }
        return null;
    }

    public static Integer parsePublishYear(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(text.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
