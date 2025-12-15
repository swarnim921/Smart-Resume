package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Candidate Signup - Creates user with ROLE_USER (for job seekers)
     */
    @PostMapping("/signup/candidate")
    public ResponseEntity<?> signupCandidate(@RequestBody User user) {
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        if (user.getName() == null || user.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));

        if (userService.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // Force role to ROLE_USER for candidates
        user.setRole("ROLE_USER");
        User saved = userService.register(user);

        String token;
        try {
            token = jwtUtil.generateToken(saved.getEmail(), saved.getRole());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "role", "candidate",
                "token", token));
    }

    /**
     * Recruiter Signup - Creates user with ROLE_RECRUITER (for recruiters/HR)
     */
    @PostMapping("/signup/recruiter")
    public ResponseEntity<?> signupRecruiter(@RequestBody User user) {
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        if (user.getName() == null || user.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));

        if (userService.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // Force role to ROLE_RECRUITER for recruiters
        user.setRole("ROLE_RECRUITER");
        User saved = userService.register(user);

        String token;
        try {
            token = jwtUtil.generateToken(saved.getEmail(), saved.getRole());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "role", "recruiter",
                "token", token));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String, String> body) {
        log.info("Signin attempt received");
        if (body == null) {
            log.warn("Request body is null");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        }
        String email = body.get("email");
        String password = body.get("password");
        log.info("Signin attempt for email: {}", email);
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Missing email or password in request");
            return ResponseEntity.badRequest().body(Map.of("error", "email and password are required"));
        }

        User user = userService.findByEmail(email);
        log.info("User found for email {}: {}", email, user != null ? "yes" : "no");
        if (user == null) {
            log.warn("User not found for email: {}", email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        boolean passwordMatches = userService.checkPassword(user, password);
        log.info("Password check for user {}: {}", email, passwordMatches ? "matches" : "does not match");
        if (!passwordMatches) {
            log.warn("Invalid password for user: {}", email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token;
        try {
            token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            log.info("Token generated successfully for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to generate token for user: {}", email, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }

        // Determine user type for frontend
        String userType = "candidate"; // default
        if (user.getRole().equals("ROLE_ADMIN")) {
            userType = "admin";
        } else if (user.getRole().equals("ROLE_RECRUITER")) {
            userType = "recruiter";
        }

        return ResponseEntity.ok(Map.of(
                "token", token,
                "name", user.getName(),
                "email", user.getEmail(),
                "role", userType));
    }
}
