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
        if (userService.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error","Email already exists"));
        }
        User saved = userService.register(user);
        String token = jwtUtil.generateToken(saved.getEmail());
        return ResponseEntity.ok(Map.of("id", saved.getId(), "email", saved.getEmail(), "token", token));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");
        User user = userService.findByEmail(email);
        if (user == null || !userService.checkPassword(user, password)) {
            return ResponseEntity.status(401).body(Map.of("error","Invalid credentials"));
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
