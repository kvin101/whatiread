package com.whatiread.shared.util;

import com.whatiread.identity.domain.User;

public final class DisplayNames {

    private DisplayNames() {
    }

    public static String format(User user) {
        String name = user.getDisplayName();
        if (name != null) {
            name = name.trim();
        }
        return name == null || name.isBlank() ? "Reader" : name;
    }
}
