package com.myfitness.user.application.result;

import com.myfitness.user.domain.model.User;
import org.jspecify.annotations.Nullable;

public record UserProfileResult(
        Long id,
        @Nullable String email,
        @Nullable String displayName,
        @Nullable String profileImageUrl
) {
    public static UserProfileResult from(User user) {
        return new UserProfileResult(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileImageUrl());
    }
}
