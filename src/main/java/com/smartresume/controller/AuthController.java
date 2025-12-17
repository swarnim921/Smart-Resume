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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (user.getPassword() == null || user.getPassword().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));

        if (userService.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        User saved = userService.register(user);
        String token;
        try {
            token = jwtUtil.generateToken(saved.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }

        return ResponseEntity.ok(Map.of("id", saved.getId(), "email", saved.getEmail(), "token", token));
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
        String token;
        try {
            token = jwtUtil.generateToken(user.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }
        return ResponseEntity.ok(Map.of(
                "token", token,
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().replace("ROLE_", "").toLowerCase()));
    }
}
