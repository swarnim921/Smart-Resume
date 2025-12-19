package com.smartresume.config;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("OAuth2 Success Handler triggered!");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Get role from cookie (Spring Security OAuth2 doesn't preserve custom state)
        String roleFromCookie = "candidate"; // default
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();

        log.info("🔍 OAuth cookies received: {}", cookies != null ? cookies.length : 0);
        log.info("🔍 Request URI: {}", request.getRequestURI());
        log.info("🔍 Request URL: {}", request.getRequestURL());
        log.info("🔍 Server Name: {}", request.getServerName());

        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                log.info("🍪 Cookie found: name='{}', value='{}', domain='{}', path='{}', secure={}, httpOnly={}",
                        cookie.getName(),
                        cookie.getValue(),
                        cookie.getDomain(),
                        cookie.getPath(),
                        cookie.getSecure(),
                        cookie.isHttpOnly());

                if ("oauth_role".equals(cookie.getName())) {
                    roleFromCookie = cookie.getValue();
                    log.info("✅ OAuth role from cookie: {}", roleFromCookie);
                    break;
                }
            }

            // Log if oauth_role cookie was NOT found
            boolean foundOAuthRole = false;
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("oauth_role".equals(cookie.getName())) {
                    foundOAuthRole = true;
                    break;
                }
            }
            if (!foundOAuthRole) {
                log.warn("⚠️ oauth_role cookie NOT found among {} cookies - will default to candidate", cookies.length);
            }
        } else {
            log.warn("⚠️ No cookies found - role will default to candidate");
        }

        // Check if user exists
        User user = userService.findByEmail(email);
        log.info("🔍 User lookup: email='{}', exists={}, existingRole={}",
                email,
                user != null,
                user != null ? user.getRole() : "N/A");

        // Determine role from cookie
        String roleToSet = "recruiter".equalsIgnoreCase(roleFromCookie) ? "ROLE_RECRUITER" : "ROLE_USER";
        log.info("🔍 Role mapping: cookie='{}' → roleToSet='{}'", roleFromCookie, roleToSet);

        // CRITICAL FIX: Use dedicated OAuth method instead of register()
        // OAuth users are pre-verified and must NOT have OTP codes or TTL expiry
        user = userService.createOrUpdateOAuthUser(email, name, roleToSet);
        log.info("✅ OAuth user created/updated with role: {} (email: {})", user.getRole(), email);

        // Generate JWT token with role claim included
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("🔍 JWT generated with role claim: {}", user.getRole());

        // Determine role for frontend
        String role = user.getRole().equals("ROLE_RECRUITER") ? "recruiter" : "candidate";
        log.info("🔍 Frontend role mapping: {} → '{}'", user.getRole(), role);

        // Redirect to HTTPS success page with token
        String redirectUrl = String.format(
                "https://www.talentsynctech.in/oauth-success.html?token=%s&name=%s&email=%s&role=%s",
                URLEncoder.encode(token, "UTF-8"),
                URLEncoder.encode(user.getName(), "UTF-8"),
                URLEncoder.encode(user.getEmail(), "UTF-8"),
                role);

        log.info("🚀 Redirecting to: oauth-success.html with role={}", role);
        response.sendRedirect(redirectUrl);
    }
}
