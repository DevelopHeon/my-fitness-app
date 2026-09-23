package com.myfitness.test.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestSecurity {
    private TestSecurity() {}

    public static RequestPostProcessor authenticatedUser(Long userId) {
        TestUserPrincipal principal = new TestUserPrincipal(userId);
        return request -> csrf().postProcessRequest(
                user(principal).postProcessRequest(request));
    }

    private record TestUserPrincipal(Long userId)
            implements UserDetails {

        TestUserPrincipal {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException(
                        "테스트 사용자 ID는 필수입니다.");
            }
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getUsername() {
            return "test-user-" + userId;
        }
    }
}
