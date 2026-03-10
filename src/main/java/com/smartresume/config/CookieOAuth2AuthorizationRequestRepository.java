package com.smartresume.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Cookie-based OAuth2 authorization request repository.
 * This avoids session dependency and works with stateless architecture.
 */
@Component
@Slf4j
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("Loading OAuth2 authorization request from cookies");
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    log.debug("Found authorization request cookie");
                    return deserialize(cookie, OAuth2AuthorizationRequest.class);
                })
                .orElseGet(() -> {
                    log.debug("No authorization request cookie found");
                    return null;
                });
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            log.debug("Deleting OAuth2 authorization request cookies");
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        log.debug("Saving OAuth2 authorization request to cookies. State: {}", authorizationRequest.getState());
        String value = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(authorizationRequest));
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value, COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isBlank()) {
            log.debug("Saving redirect_uri parameter to cookie: {}", redirectUriAfterLogin);
            addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        log.debug("Removing OAuth2 authorization request from cookies");
        OAuth2AuthorizationRequest originalRequest = this.loadAuthorizationRequest(request);
        if (originalRequest != null) {
            log.debug("Authorization request found for state: {}. Deleting cookies.", originalRequest.getState());
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        } else {
            log.debug("No authorization request found to remove.");
        }
        return originalRequest;
    }

    private static <T> T deserialize(String cookie, Class<T> cls) {
        try {
            return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie)));
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value).append("; ");
        sb.append("Path=/; ");
        sb.append("Max-Age=").append(maxAge).append("; ");
        sb.append("HttpOnly; ");
        sb.append("Secure; ");
        sb.append("SameSite=None");

        response.addHeader("Set-Cookie", sb.toString());
        log.debug("Added cookie '{}' with SameSite=None and Secure", name);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(name).append("=; ");
                    sb.append("Path=/; ");
                    sb.append("Max-Age=0; ");
                    sb.append("HttpOnly; ");
                    sb.append("Secure; ");
                    sb.append("SameSite=None");

                    response.addHeader("Set-Cookie", sb.toString());
                    log.debug("Deleted cookie '{}' by setting Max-Age=0", name);
                }
            }
        }
    }
}
