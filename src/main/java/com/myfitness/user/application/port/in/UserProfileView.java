package com.myfitness.user.application.port.in;

import org.jspecify.annotations.Nullable;

public record UserProfileView(
        Long id,
        @Nullable String email,
        @Nullable String displayName,
        @Nullable String profileImageUrl
) {}
