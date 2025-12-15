package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSetupController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // PUBLIC ENDPOINT — only for very first admin creation
    @PostMapping("/create-initial")
    public ResponseEntity<?> createInitialAdmin(@RequestBody User adminRequest) {

        // Check if admin already exists
        boolean adminExists = userRepository.findByRole("ADMIN").isPresent();

        if (adminExists) {
            return ResponseEntity.badRequest().body(
                    "Admin already exists — cannot create another admin using this endpoint."
            );
        }

        // Create admin
        User admin = new User();
        admin.setName(adminRequest.getName());
        admin.setEmail(adminRequest.getEmail());
        admin.setPassword(passwordEncoder.encode(adminRequest.getPassword()));
        admin.setRole("ADMIN");

        userRepository.save(admin);

        return ResponseEntity.ok("Initial admin created successfully!");
    }
}
