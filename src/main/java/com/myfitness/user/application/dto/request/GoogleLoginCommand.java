package com.myfitness.user.application.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record GoogleLoginCommand(
        String googleSubject,
        @Nullable String email,
        @Nullable String displayName,
        @Nullable String profileImageUrl
) {}
