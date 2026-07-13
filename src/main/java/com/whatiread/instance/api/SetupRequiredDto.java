package com.whatiread.instance.api;

public record SetupRequiredDto(
        boolean setupRequired,
        boolean registrationEnabled
) {
}
