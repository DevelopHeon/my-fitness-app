package com.myfitness.user.application.command;

public record GoogleLoginCommand(
        String googleSubject,
        String email,
        String displayName,
        String profileImageUrl
) {}
