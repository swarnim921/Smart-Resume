package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/update-role")
    public ResponseEntity<?> updateUserRole(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        String newRole = request.get("role");

        if (email == null || newRole == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Email and role are required"));
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "User not found"));
        }

        // Update role
        String roleToSet = newRole.equalsIgnoreCase("recruiter") ? "ROLE_RECRUITER" : "ROLE_USER";
        user.setRole(roleToSet);
        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Role updated successfully",
                "email", email,
                "newRole", roleToSet));
    }
}
