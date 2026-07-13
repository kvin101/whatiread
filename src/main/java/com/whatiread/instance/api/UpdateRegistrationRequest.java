package com.whatiread.instance.api;

import jakarta.validation.constraints.NotNull;

public record UpdateRegistrationRequest(@NotNull Boolean enabled) {
}
