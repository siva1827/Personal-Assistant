package com.siva.assistant.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DeviceDtos {

    private DeviceDtos() {
    }

    public record RegisterRequest(@NotBlank @Size(max = 512) String token, @NotNull DevicePlatform platform) {
    }

    public record UnregisterRequest(@NotBlank @Size(max = 512) String token) {
    }
}
