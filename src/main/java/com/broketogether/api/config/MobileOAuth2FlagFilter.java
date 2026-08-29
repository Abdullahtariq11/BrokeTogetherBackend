package com.broketogether.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Marks the OAuth2 login session as mobile-initiated so {@link OAuth2SuccessHandler}
 * knows to redirect back into the app instead of the web frontend. The mobile client
 * hits /oauth2/authorization/google?mobile=true; Spring's OAuth2AuthorizationRequestRedirectFilter
 * (and the Google round trip after it) both rely on the same HttpSession, so a flag
 * stashed here survives until the success handler reads it.
 */
@Component
public class MobileOAuth2FlagFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "oauth2_mobile_redirect";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if ("/oauth2/authorization/google".equals(request.getRequestURI())
                && "true".equals(request.getParameter("mobile"))) {
            request.getSession(true).setAttribute(SESSION_ATTRIBUTE, Boolean.TRUE);
        }
        filterChain.doFilter(request, response);
    }
}
