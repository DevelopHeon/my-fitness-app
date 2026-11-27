package com.myfitness.user.application.port.in;

public interface CurrentUserQuery {
    UserProfileView getCurrentUser(Long userId);
}
