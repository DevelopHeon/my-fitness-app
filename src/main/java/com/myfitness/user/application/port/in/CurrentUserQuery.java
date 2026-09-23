package com.myfitness.user.application.port.in;

import com.myfitness.user.application.result.UserProfileResult;

public interface CurrentUserQuery {
    UserProfileResult getCurrentUser(Long userId);
}
