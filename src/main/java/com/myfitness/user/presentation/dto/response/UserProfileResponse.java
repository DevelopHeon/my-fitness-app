package com.myfitness.user.presentation.dto.response;

import com.myfitness.user.application.port.in.UserProfileView;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String profileImageUrl
) {
    public static UserProfileResponse from(
            UserProfileView result) {
        return new UserProfileResponse(
                result.id(),
                result.email(),
                result.displayName(),
                result.profileImageUrl());
    }
}
