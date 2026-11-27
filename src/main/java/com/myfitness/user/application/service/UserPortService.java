package com.myfitness.user.application.service;

import com.myfitness.user.application.usecase.request.GoogleLoginCommand;
import com.myfitness.user.application.usecase.response.UserProfileResult;
import com.myfitness.user.application.port.in.CurrentUserQuery;
import com.myfitness.user.application.port.in.GoogleLoginPort;
import com.myfitness.user.application.port.in.UserProfileView;
import com.myfitness.user.application.usecase.CurrentUserUseCase;
import com.myfitness.user.application.usecase.GoogleLoginUseCase;
import org.springframework.stereotype.Service;

@Service
public class UserPortService implements CurrentUserQuery, GoogleLoginPort {
    private final CurrentUserUseCase users;
    private final GoogleLoginUseCase login;

    public UserPortService(CurrentUserUseCase users, GoogleLoginUseCase login) {
        this.users = users;
        this.login = login;
    }

    @Override
    public UserProfileView login(GoogleLoginPort.LoginInput input) {
        return toView(login.login(new GoogleLoginCommand(
                input.googleSubject(), input.email(), input.displayName(), input.profileImageUrl())));
    }

    @Override
    public UserProfileView getCurrentUser(Long userId) {
        return toView(users.getCurrentUser(userId));
    }

    private static UserProfileView toView(UserProfileResult user) {
        return new UserProfileView(user.id(), user.email(), user.displayName(), user.profileImageUrl());
    }
}
