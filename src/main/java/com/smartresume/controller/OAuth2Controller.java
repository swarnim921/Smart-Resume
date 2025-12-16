package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Google OAuth2 Success Handler
     * Called after successful Google authentication
     */
    @GetMapping("/google/success")
    public void googleLoginSuccess(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String userType,
            HttpServletResponse response) throws IOException {

        if (principal == null) {
            response.sendRedirect("http://localhost:5500/smart_resume_signin_page.html?error=auth_failed");
            return;
        }

        try {
            // Extract user info from Google
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");

            log.info("Google OAuth2 login for email: {}", email);

            // Check if user exists
            User user = userService.findByEmail(email);

            if (user == null) {
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setPassword(""); // No password for OAuth users

                // Set role based on userType parameter (candidate or recruiter)
                if ("recruiter".equalsIgnoreCase(userType)) {
                    user.setRole("ROLE_RECRUITER");
                } else {
                    user.setRole("ROLE_USER"); // Default to candidate
                }

                user = userService.register(user);
                log.info("Created new user via Google OAuth: {}", email);
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail());

            // Determine user type for frontend
            String role = "candidate";
            if (user.getRole().equals("ROLE_ADMIN")) {
                role = "admin";
            } else if (user.getRole().equals("ROLE_RECRUITER")) {
                role = "recruiter";
            }

            // Redirect to frontend with credentials as URL parameters
            String redirectUrl = String.format(
                    "http://localhost:5500/smart_resume_signin_page.html?token=%s&name=%s&email=%s&role=%s",
                    URLEncoder.encode(token, "UTF-8"),
                    URLEncoder.encode(user.getName(), "UTF-8"),
                    URLEncoder.encode(user.getEmail(), "UTF-8"),
                    role);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing Google OAuth login", e);
            response.sendRedirect("http://localhost:5500/smart_resume_signin_page.html?error=processing_failed");
        }
    }

    /**
     * OAuth2 Failure Handler
     */
    @GetMapping("/failure")
    public void loginFailure(
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        log.warn("OAuth2 login failed: {}", error);
        response.sendRedirect("http://localhost:5500/smart_resume_signin_page.html?error=oauth_failed");
    }
}
