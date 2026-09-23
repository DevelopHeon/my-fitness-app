package com.myfitness.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.user.application.command.GoogleLoginCommand;
import com.myfitness.user.application.port.in.GoogleLoginUseCase;
import com.myfitness.user.application.result.UserProfileResult;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleOidcUserServiceTest {

    private final GoogleLoginUseCase loginUseCase =
            mock(GoogleLoginUseCase.class);
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate =
            mock(OAuth2UserService.class);
    private final GoogleOidcUserService service =
            new GoogleOidcUserService(loginUseCase, delegate);

    @Test
    void mapsGoogleClaimsToInternalUserAndPrincipal() throws Exception {
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser googleUser = googleUser();
        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(loginUseCase.login(any(GoogleLoginCommand.class)))
                .thenReturn(new UserProfileResult(
                        42L,
                        "user@example.com",
                        "Fitness User",
                        "https://example.com/profile.png"));

        OidcUser loaded = service.loadUser(request);

        ArgumentCaptor<GoogleLoginCommand> command =
                ArgumentCaptor.forClass(GoogleLoginCommand.class);
        verify(loginUseCase).login(command.capture());
        assertThat(command.getValue().googleSubject())
                .isEqualTo("google-subject-42");
        assertThat(command.getValue().email())
                .isEqualTo("user@example.com");
        assertThat(command.getValue().displayName())
                .isEqualTo("Fitness User");

        assertThat(loaded)
                .isInstanceOf(AuthenticatedOidcUser.class);
        assertThat(((AuthenticatedOidcUser) loaded).getUserId())
                .isEqualTo(42L);
        assertThat(loaded.getSubject())
                .isEqualTo("google-subject-42");
    }

    @Test
    void authenticatedPrincipalIsSerializableForJdbcSession()
            throws Exception {
        AuthenticatedOidcUser principal =
                new AuthenticatedOidcUser(42L, googleUser());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output =
                     new ObjectOutputStream(bytes)) {
            output.writeObject(principal);
        }

        assertThat(bytes.size()).isPositive();
    }

    private static OidcUser googleUser() {
        Instant now = Instant.parse("2026-09-23T05:00:00Z");
        Map<String, Object> claims = Map.of(
                "sub", "google-subject-42",
                "email", "user@example.com",
                "name", "Fitness User",
                "picture", "https://example.com/profile.png");

        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                now,
                now.plusSeconds(3600),
                claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        OidcUser user = mock(OidcUser.class);
        when(user.getAuthorities()).thenReturn(List.of());
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getUserInfo()).thenReturn(userInfo);
        when(user.getSubject()).thenReturn("google-subject-42");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getFullName()).thenReturn("Fitness User");
        when(user.getPicture())
                .thenReturn("https://example.com/profile.png");
        return user;
    }
}
