package com.myfitness.user.application.port.in;

import com.myfitness.user.application.command.GoogleLoginCommand;
import com.myfitness.user.application.result.UserProfileResult;

public interface GoogleLoginUseCase {
    UserProfileResult login(GoogleLoginCommand command);
}
