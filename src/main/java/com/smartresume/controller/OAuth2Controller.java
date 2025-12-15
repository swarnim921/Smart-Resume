package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> googleLoginSuccess(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String userType) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
        }

        try {
            // Extract user info from Google
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");
            String googleId = principal.getAttribute("sub");

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
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

            // Determine user type for frontend
            String role = "candidate";
            if (user.getRole().equals("ROLE_ADMIN")) {
                role = "admin";
            } else if (user.getRole().equals("ROLE_RECRUITER")) {
                role = "recruiter";
            }

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", role,
                    "authMethod", "google"));

        } catch (Exception e) {
            log.error("Error processing Google OAuth login", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process Google login"));
        }
    }

    /**
     * OAuth2 Failure Handler
     */
    @GetMapping("/failure")
    public ResponseEntity<?> loginFailure(@RequestParam(required = false) String error) {
        log.warn("OAuth2 login failed: {}", error);
        return ResponseEntity.status(401).body(Map.of(
                "error", "OAuth2 authentication failed",
                "details", error != null ? error : "Unknown error"));
    }
}
