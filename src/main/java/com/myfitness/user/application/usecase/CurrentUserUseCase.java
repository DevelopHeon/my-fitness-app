package com.myfitness.user.application.usecase;

import com.myfitness.user.application.usecase.response.UserProfileResult;

public interface CurrentUserUseCase {
    UserProfileResult getCurrentUser(Long userId);
}
