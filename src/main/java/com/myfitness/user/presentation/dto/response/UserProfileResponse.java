package com.myfitness.user.presentation.dto.response;

import com.myfitness.user.application.dto.response.UserProfileResult;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String profileImageUrl
) {
    public static UserProfileResponse from(
            UserProfileResult result) {
        return new UserProfileResponse(
                result.id(),
                result.email(),
                result.displayName(),
                result.profileImageUrl());
    }
}
