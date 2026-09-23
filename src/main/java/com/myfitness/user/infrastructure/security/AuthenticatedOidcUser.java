package com.myfitness.user.infrastructure.security;

import java.io.Serial;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class AuthenticatedOidcUser extends DefaultOidcUser {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;

    AuthenticatedOidcUser(
            Long userId,
            OidcUser delegate) {
        super(
                delegate.getAuthorities(),
                delegate.getIdToken(),
                delegate.getUserInfo());
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "인증 사용자 ID는 필수입니다.");
        }
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
