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

        // Get role from OAuth state parameter (industry standard approach)
        String roleFromState = null;
        String state = request.getParameter("state");

        if (state != null && !state.isEmpty()) {
            try {
                // Decode Base64 state parameter
                String decoded = new String(java.util.Base64.getDecoder().decode(state));
                // Parse JSON to extract role
                if (decoded.contains("\"role\"")) {
                    // Simple JSON parsing (for production, use Jackson or Gson)
                    roleFromState = decoded.substring(decoded.indexOf("\"role\":\"") + 8);
                    roleFromState = roleFromState.substring(0, roleFromState.indexOf("\""));
                }
                log.info("OAuth state decoded - role: {}", roleFromState);
            } catch (Exception e) {
                log.error("Failed to decode OAuth state parameter", e);
            }
        }

        // Check if user exists, create if not
        User user = userService.findByEmail(email);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(""); // OAuth users don't have passwords

            // Set role based on state parameter
            if ("recruiter".equalsIgnoreCase(roleFromState)) {
                user.setRole("ROLE_RECRUITER");
            } else {
                user.setRole("ROLE_USER");
            }

            user = userService.register(user);
            log.info("Created new OAuth user with role: {}", user.getRole());
        } else {
            // Update role if state parameter is provided and different from current role
            if (roleFromState != null && !roleFromState.isEmpty()) {
                String newRole = "recruiter".equalsIgnoreCase(roleFromState) ? "ROLE_RECRUITER" : "ROLE_USER";
                if (!newRole.equals(user.getRole())) {
                    user.setRole(newRole);
                    user = userService.register(user); // Save updated role
                    log.info("Updated existing user role to: {}", user.getRole());
                }
            }
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Determine role for frontend
        String role = user.getRole().equals("ROLE_RECRUITER") ? "recruiter" : "candidate";

        // Redirect to HTTPS success page with token
        String redirectUrl = String.format(
                "https://www.talentsynctech.in/oauth-success.html?token=%s&name=%s&email=%s&role=%s",
                URLEncoder.encode(token, "UTF-8"),
                URLEncoder.encode(user.getName(), "UTF-8"),
                URLEncoder.encode(user.getEmail(), "UTF-8"),
                role);

        response.sendRedirect(redirectUrl);
    }
}
