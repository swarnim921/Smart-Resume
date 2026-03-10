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
        log.info("Loading OAuth2 authorization request from cookies");
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    log.info("Found authorization request cookie: {} bytes", cookie.length());
                    OAuth2AuthorizationRequest authRequest = deserialize(cookie, OAuth2AuthorizationRequest.class);
                    if (authRequest != null) {
                        log.info("Loaded auth request: state={}, redirectUri={}, nonce={}",
                                authRequest.getState(), authRequest.getRedirectUri(),
                                authRequest.getAttribute("nonce"));
                    }
                    return authRequest;
                })
                .orElseGet(() -> {
                    log.info("No authorization request cookie found");
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

        log.info("Saving OAuth2 authorization request to cookies. State: {}", authorizationRequest.getState());
        String value = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SerializationUtils.serialize(authorizationRequest));
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value, COOKIE_EXPIRE_SECONDS);
        log.info("Saved auth request: state={}, nonce={}", authorizationRequest.getState(),
                authorizationRequest.getAttribute("nonce"));

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
            log.error("Failed to deserialize OAuth2 cookie. Size: {}, Exception: {}",
                    cookie != null ? cookie.length() : "null", e.getMessage());
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
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax") // Lax is safer for top-level redirects and more widely supported
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Added cookie '{}' with SameSite=Lax and Secure (size: {})", name, value.length());
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        org.springframework.http.ResponseCookie deleteCookie = org.springframework.http.ResponseCookie
                .from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());
        log.debug("Deleted cookie '{}' by setting Max-Age=0 with SameSite=Lax", name);
    }
}
