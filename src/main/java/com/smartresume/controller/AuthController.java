package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final com.smartresume.service.EmailService emailService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));

        // Check if user already exists
        User existingUser = userService.findByEmail(user.getEmail());
        if (existingUser != null) {
            if (existingUser.isVerified()) {
                // Verified user exists - block signup
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            } else {
                // Unverified user exists - allow overwrite (user retrying signup)
                // Delete old unverified record to start fresh
                userService.deleteUser(existingUser);
            }
        }

        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        User saved = userService.register(user);

        // Send Verification Email - CRITICAL: Rollback if this fails
        try {
            emailService.sendVerificationEmail(saved.getEmail(), saved.getVerificationCode());
        } catch (Exception e) {
            // ROLLBACK: Delete the user we just created
            userService.deleteUser(saved);
            System.err.println("Failed to send verification email: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send verification email. Please try again.",
                    "details", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Signup successful. Please verify your email.",
                "email", saved.getEmail(),
                "requiresVerification", true));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String, String> body) {
        if (body == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password are required"));
        }

        User user = userService.findByEmail(email);
        if (user == null || !userService.checkPassword(user, password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        if (!user.isVerified()) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Email not verified",
                    "requiresVerification", true,
                    "email", user.getEmail()));
        }

        String token;
        try {
            token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }
        return ResponseEntity.ok(Map.of(
                "token", token,
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().replace("ROLE_", "").toLowerCase()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and code are required"));
        }

        if (userService.verifyUser(email, code)) {
            User user = userService.findByEmail(email);
            // Generate token immediately upon verification
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            return ResponseEntity.ok(Map.of(
                    "message", "Verification successful",
                    "token", token,
                    "name", user.getName(),
                    "role", user.getRole().replace("ROLE_", "").toLowerCase()));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification code"));
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        String newCode = userService.resendVerificationCode(email);
        if (newCode != null) {
            try {
                emailService.sendVerificationEmail(email, newCode);
                return ResponseEntity.ok(Map.of("message", "Verification code resent"));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to send email"));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("error", "User not found or already verified"));
    }
}
