package com.myfitness.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.user.application.dto.request.GoogleLoginCommand;
import com.myfitness.user.application.exception.UserNotFoundException;
import com.myfitness.user.application.port.out.UserRepositoryPort;
import com.myfitness.user.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserApplicationServiceTest {
    private static final Instant NOW =
            Instant.parse("2026-09-23T03:00:00Z");
    private static final Clock CLOCK =
            Clock.fixed(NOW, ZoneOffset.UTC);

    private final UserRepositoryPort repository =
            mock(UserRepositoryPort.class);
    private final UserApplicationService service =
            new UserApplicationService(repository, CLOCK);

    @Test
    void createsNewUserForFirstGoogleLogin() {
        GoogleLoginCommand command = new GoogleLoginCommand(
                "google-subject-1",
                "user@example.com",
                "User",
                "https://example.com/profile.png");
        when(repository.findByGoogleSubject("google-subject-1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.login(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getGoogleSubject()).isEqualTo("google-subject-1");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void reusesExistingUserAndRefreshesProfile() {
        User existing = User.createFromGoogle(
                "google-subject-1",
                "old@example.com",
                "Old",
                null,
                NOW.minusSeconds(3600));
        when(repository.findByGoogleSubject("google-subject-1"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.login(new GoogleLoginCommand(
                "google-subject-1",
                "new@example.com",
                "New",
                null));

        assertThat(existing.getGoogleSubject()).isEqualTo("google-subject-1");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getDisplayName()).isEqualTo("New");
        assertThat(existing.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void throwsWhenCurrentUserDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
