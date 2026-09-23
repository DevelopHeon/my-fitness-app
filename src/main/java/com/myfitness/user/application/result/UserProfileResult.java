package com.myfitness.user.application.result;

import com.myfitness.user.domain.model.User;

public record UserProfileResult(
        Long id,
        String email,
        String displayName,
        String profileImageUrl
) {
    public static UserProfileResult from(User user) {
        return new UserProfileResult(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileImageUrl());
    }
}
