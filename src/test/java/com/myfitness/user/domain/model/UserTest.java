package com.myfitness.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.user.domain.exception.UserRuleException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void createsGoogleUserUsingSubjectAsExternalIdentity() {
        Instant now = Instant.parse("2026-09-23T00:00:00Z");

        User user = User.createFromGoogle(
                "google-subject-1",
                " user@example.com ",
                " User ",
                " https://example.com/profile.png ",
                now);

        assertThat(user.getGoogleSubject()).isEqualTo("google-subject-1");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getDisplayName()).isEqualTo("User");
        assertThat(user.getProfileImageUrl())
                .isEqualTo("https://example.com/profile.png");
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    void updatesProfileWithoutChangingGoogleSubject() {
        Instant createdAt = Instant.parse("2026-09-23T00:00:00Z");
        Instant loggedInAt = Instant.parse("2026-09-23T01:00:00Z");
        User user = User.createFromGoogle(
                "google-subject-1",
                "old@example.com",
                "Old",
                null,
                createdAt);

        user.recordGoogleLogin(
                "new@example.com",
                "New",
                "https://example.com/new.png",
                loggedInAt);

        assertThat(user.getGoogleSubject()).isEqualTo("google-subject-1");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getDisplayName()).isEqualTo("New");
        assertThat(user.getUpdatedAt()).isEqualTo(loggedInAt);
        assertThat(user.getLastLoginAt()).isEqualTo(loggedInAt);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsBlankGoogleSubject() {
        assertThatThrownBy(() -> User.createFromGoogle(
                " ",
                "user@example.com",
                "User",
                null,
                Instant.parse("2026-09-23T00:00:00Z")))
                .isInstanceOf(UserRuleException.class)
                .hasMessage("Google subject는 필수입니다.");
    }
}
