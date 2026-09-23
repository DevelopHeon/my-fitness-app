package com.myfitness.test.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import jakarta.servlet.http.Cookie;
import java.util.Collection;
import java.util.List;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestSecurity {
    private TestSecurity() {}

    public static RequestPostProcessor authenticatedUser(Long userId) {
        TestUserPrincipal principal = new TestUserPrincipal(userId);
        return request -> {
            user(principal).postProcessRequest(request);

            CsrfTokenRepository repository =
                    WebTestUtils.getCsrfTokenRepository(request);
            MockHttpServletResponse response =
                    new MockHttpServletResponse();
            CsrfToken token = repository
                    .loadDeferredToken(request, response)
                    .get();

            Cookie[] generatedCookies = response.getCookies();
            if (generatedCookies.length > 0) {
                Cookie[] existingCookies = request.getCookies();
                if (existingCookies == null || existingCookies.length == 0) {
                    request.setCookies(generatedCookies);
                } else {
                    Cookie[] merged = new Cookie[
                            existingCookies.length + generatedCookies.length];
                    System.arraycopy(
                            existingCookies,
                            0,
                            merged,
                            0,
                            existingCookies.length);
                    System.arraycopy(
                            generatedCookies,
                            0,
                            merged,
                            existingCookies.length,
                            generatedCookies.length);
                    request.setCookies(merged);
                }
            }

            request.addHeader(token.getHeaderName(), token.getToken());
            return request;
        };
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
