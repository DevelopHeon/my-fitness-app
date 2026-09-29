package com.myfitness.user.application.port.in;

import com.myfitness.user.application.dto.response.UserProfileResult;

public interface CurrentUserQuery {
    UserProfileResult getCurrentUser(Long userId);
}
