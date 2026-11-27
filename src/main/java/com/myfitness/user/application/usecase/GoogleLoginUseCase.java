package com.myfitness.user.application.usecase;

import com.myfitness.user.application.usecase.request.GoogleLoginCommand;
import com.myfitness.user.application.usecase.response.UserProfileResult;

public interface GoogleLoginUseCase {
    UserProfileResult login(GoogleLoginCommand command);
}
