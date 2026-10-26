package com.siva.assistant.user;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.user.UserDtos.UpdateProfileRequest;
import com.siva.assistant.user.UserDtos.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(long userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse update(long userId, UpdateProfileRequest request) {
        User user = find(userId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.dietaryPreference() != null) {
            user.setDietaryPreference(request.dietaryPreference());
        }
        if (request.allergies() != null) {
            user.setAllergies(request.allergies());
        }
        return UserResponse.from(user);
    }

    private User find(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
