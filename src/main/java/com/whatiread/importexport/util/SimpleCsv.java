package com.whatiread.importexport.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal RFC-style CSV parser (no external deps).
 */
public final class SimpleCsv {

    private SimpleCsv() {
    }

    public static List<Map<String, String>> parse(String content) {
        List<String[]> rows = parseRows(content);
        if (rows.isEmpty()) {
            return List.of();
        }
        String[] headers = rows.getFirst();
        List<Map<String, String>> records = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] values = rows.get(i);
            if (values.length == 0 || (values.length == 1 && values[0].isBlank())) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            for (int c = 0; c < headers.length; c++) {
                String key = headers[c].trim();
                String value = c < values.length ? values[c].trim() : "";
                row.put(key, value);
                row.put(key.toLowerCase(Locale.ROOT), value);
            }
            records.add(row);
        }
        return records;
    }

    public static String get(Map<String, String> row, String... names) {
        for (String name : names) {
            String value = row.get(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
            value = row.get(name.toLowerCase(Locale.ROOT));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static List<String[]> parseRows(String content) {
        List<String[]> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                rows.add(current.toArray(String[]::new));
                current = new ArrayList<>();
            } else if (ch != '\r') {
                field.append(ch);
            }
        }
        if (!field.isEmpty() || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current.toArray(String[]::new));
        }
        return rows;
    }
}
