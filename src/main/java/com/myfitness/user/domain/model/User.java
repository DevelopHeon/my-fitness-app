package com.myfitness.user.domain.model;

import com.myfitness.user.domain.exception.UserRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_google_subject",
                columnNames = "google_subject"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_subject", nullable = false, length = 255)
    private String googleSubject;

    @Column(length = 320)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "profile_image_url", length = 1024)
    private String profileImageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    protected User() {}

    private User(
            String googleSubject,
            String email,
            String displayName,
            String profileImageUrl,
            Instant now) {
        this.googleSubject = requireGoogleSubject(googleSubject);
        validateNow(now);
        this.email = normalize(email, 320, "이메일");
        this.displayName = normalize(displayName, 255, "표시 이름");
        this.profileImageUrl = normalize(profileImageUrl, 1024, "프로필 이미지 URL");
        this.createdAt = now;
        this.updatedAt = now;
        this.lastLoginAt = now;
    }

    public static User createFromGoogle(
            String googleSubject,
            String email,
            String displayName,
            String profileImageUrl,
            Instant now) {
        return new User(
                googleSubject,
                email,
                displayName,
                profileImageUrl,
                now);
    }

    public void recordGoogleLogin(
            String email,
            String displayName,
            String profileImageUrl,
            Instant now) {
        validateNow(now);
        this.email = normalize(email, 320, "이메일");
        this.displayName = normalize(displayName, 255, "표시 이름");
        this.profileImageUrl = normalize(profileImageUrl, 1024, "프로필 이미지 URL");
        this.updatedAt = now;
        this.lastLoginAt = now;
    }

    private static String requireGoogleSubject(String value) {
        String normalized = normalize(value, 255, "Google subject");
        if (normalized == null) {
            throw new UserRuleException("Google subject는 필수입니다.");
        }
        return normalized;
    }

    private static void validateNow(Instant now) {
        if (now == null) {
            throw new UserRuleException("사용자 변경 시각은 필수입니다.");
        }
    }

    private static String normalize(
            String value,
            int maxLength,
            String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new UserRuleException(
                    fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public String getGoogleSubject() { return googleSubject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
