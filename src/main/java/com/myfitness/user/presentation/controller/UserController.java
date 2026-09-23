package com.myfitness.user.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.user.application.port.in.CurrentUserQuery;
import com.myfitness.user.presentation.dto.response.UserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserQuery currentUserQuery;

    public UserController(CurrentUserQuery currentUserQuery) {
        this.currentUserQuery = currentUserQuery;
    }

    @GetMapping("/me")
    public UserProfileResponse me(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return UserProfileResponse.from(
                currentUserQuery.getCurrentUser(userId));
    }
}
