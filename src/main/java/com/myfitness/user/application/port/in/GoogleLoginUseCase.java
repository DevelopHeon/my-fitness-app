package com.myfitness.user.application.port.in;

import com.myfitness.user.application.dto.request.GoogleLoginCommand;
import com.myfitness.user.application.dto.response.UserProfileResult;

public interface GoogleLoginUseCase {
    UserProfileResult login(GoogleLoginCommand command);
}
