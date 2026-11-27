package com.myfitness.user.application.port.in;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public interface GoogleLoginPort {
    UserProfileView login(LoginInput input);

    @NullMarked
    record LoginInput(
            String googleSubject,
            @Nullable String email,
            @Nullable String displayName,
            @Nullable String profileImageUrl
    ) {}
}
