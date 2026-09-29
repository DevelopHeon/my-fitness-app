package com.myfitness.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.user.application.dto.request.GoogleLoginCommand;
import com.myfitness.user.application.dto.response.UserProfileResult;
import com.myfitness.user.application.port.in.GoogleLoginUseCase;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
    void allowsOptionalGoogleProfileClaimsToBeMissing() {
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser googleUser = googleUser(
                "google-subject-42",
                null,
                null,
                null);
        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(loginUseCase.login(any(GoogleLoginCommand.class)))
                .thenReturn(new UserProfileResult(
                        42L,
                        null,
                        null,
                        null));

        service.loadUser(request);

        ArgumentCaptor<GoogleLoginCommand> command =
                ArgumentCaptor.forClass(GoogleLoginCommand.class);
        verify(loginUseCase).login(command.capture());
        assertThat(command.getValue().googleSubject())
                .isEqualTo("google-subject-42");
        assertThat(command.getValue().email()).isNull();
        assertThat(command.getValue().displayName()).isNull();
        assertThat(command.getValue().profileImageUrl()).isNull();
    }

    @Test
    void rejectsOidcUserWithoutSubject() {
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser googleUser = googleUser(
                null,
                "user@example.com",
                "Fitness User",
                null);
        when(delegate.loadUser(request)).thenReturn(googleUser);

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("sub claim");

        verify(loginUseCase, never())
                .login(any(GoogleLoginCommand.class));
    }

    @Test
    void rejectsNullOidcUserFromDelegate() {
        OidcUserRequest request = mock(OidcUserRequest.class);
        when(delegate.loadUser(request)).thenReturn(null);

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("사용자 정보가 비어");

        verify(loginUseCase, never())
                .login(any(GoogleLoginCommand.class));
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
        return googleUser(
                "google-subject-42",
                "user@example.com",
                "Fitness User",
                "https://example.com/profile.png");
    }

    private static OidcUser googleUser(
            String subject,
            String email,
            String fullName,
            String picture) {
        Instant now = Instant.parse("2026-09-23T05:00:00Z");
        Map<String, Object> claims = Map.of(
                "sub", "google-subject-42");

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
        when(user.getSubject()).thenReturn(subject);
        when(user.getEmail()).thenReturn(email);
        when(user.getFullName()).thenReturn(fullName);
        when(user.getPicture()).thenReturn(picture);
        return user;
    }
}
