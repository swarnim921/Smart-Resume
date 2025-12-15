package com.smartresume.service;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) user.setRole("USER");
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        log.info("Looking up user by email: {}", email);
        User user = userRepository.findByEmail(email).orElse(null);
        log.info("User lookup result for {}: {}", email, user != null ? "found" : "not found");
        return user;
    }

    public boolean checkPassword(User user, String rawPassword) {
        log.info("Checking password for user: {}", user.getEmail());
        boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
        log.info("Password check result: {}", matches ? "matches" : "does not match");
        return matches;
    }
}
