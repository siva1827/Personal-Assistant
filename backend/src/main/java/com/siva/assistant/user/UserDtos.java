package com.siva.assistant.user;

import jakarta.validation.constraints.Size;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String email,
            String displayName,
            DietaryPreference dietaryPreference,
            List<String> allergies) {

        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                    user.getDietaryPreference(), user.getAllergies());
        }
    }

    /** Fields left null are not changed. */
    public record UpdateProfileRequest(
            @Size(max = 100) String displayName,
            DietaryPreference dietaryPreference,
            List<@Size(max = 50) String> allergies) {
    }
}
