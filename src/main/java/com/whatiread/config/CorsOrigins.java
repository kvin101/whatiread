package com.whatiread.config;

import java.util.Arrays;
import java.util.List;

public final class CorsOrigins {

    private CorsOrigins() {
    }

    public static List<String> split(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
