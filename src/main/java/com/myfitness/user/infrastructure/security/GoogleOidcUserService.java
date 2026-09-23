package com.myfitness.user.infrastructure.security;

import com.myfitness.user.application.command.GoogleLoginCommand;
import com.myfitness.user.application.port.in.GoogleLoginUseCase;
import com.myfitness.user.application.result.UserProfileResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
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
        OidcUser oidcUser = delegate.loadUser(userRequest);
        UserProfileResult user = googleLoginUseCase.login(
                new GoogleLoginCommand(
                        oidcUser.getSubject(),
                        oidcUser.getEmail(),
                        oidcUser.getFullName(),
                        oidcUser.getPicture()));

        return new AuthenticatedOidcUser(user.id(), oidcUser);
    }
}
