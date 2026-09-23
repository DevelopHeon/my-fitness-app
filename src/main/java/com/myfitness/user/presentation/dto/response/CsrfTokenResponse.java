package com.myfitness.user.presentation.dto.response;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String cookieName
) {
    private static final String SPA_CSRF_COOKIE_NAME = "XSRF-TOKEN";

    public static CsrfTokenResponse from(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                SPA_CSRF_COOKIE_NAME);
    }
}
