package com.myfitness.user.infrastructure.security;

import com.myfitness.user.application.dto.request.GoogleLoginCommand;
import com.myfitness.user.application.dto.response.UserProfileResult;
import com.myfitness.user.application.port.in.GoogleLoginUseCase;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class GoogleOidcUserService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final GoogleLoginUseCase googleLoginUseCase;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Autowired
    public GoogleOidcUserService(
            GoogleLoginUseCase googleLoginUseCase) {
        this(googleLoginUseCase, new OidcUserService());
    }

    GoogleOidcUserService(
            GoogleLoginUseCase googleLoginUseCase,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.googleLoginUseCase = googleLoginUseCase;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = requireOidcUser(delegate.loadUser(userRequest));
        String subject = requireSubject(oidcUser);

        UserProfileResult user = googleLoginUseCase.login(
                new GoogleLoginCommand(
                        subject,
                        oidcUser.getEmail(),
                        oidcUser.getFullName(),
                        oidcUser.getPicture()));

        return new AuthenticatedOidcUser(user.id(), oidcUser);
    }

    private static OidcUser requireOidcUser(@Nullable OidcUser oidcUser) {
        if (oidcUser == null) {
            throw invalidUserInfo(
                    "Google OIDC 사용자 정보가 비어 있습니다.");
        }
        return oidcUser;
    }

    private static String requireSubject(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        if (subject == null || subject.isBlank()) {
            throw invalidUserInfo(
                    "Google OIDC 응답에 sub claim이 없습니다.");
        }
        return subject;
    }

    private static OAuth2AuthenticationException invalidUserInfo(
            String message) {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info_response"),
                message);
    }
}
