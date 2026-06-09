package com.smartresume.service;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    // Injected Spring bean — shared singleton, BCrypt strength set once in SecurityConfig
    private final PasswordEncoder passwordEncoder;

    public User register(User user) {
        log.debug("UserService.register: Received user with role={}", user.getRole());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAuthProvider("LOCAL");
        if (user.getRole() == null)
            user.setRole("ROLE_USER");

        log.debug("UserService.register: Saving user with role={}", user.getRole());

        // Generate verification code
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
        user.setVerified(false);

        return userRepository.save(user);
    }

    /**
     * Create or update OAuth user (Google, etc.)
     * OAuth users are pre-verified since email ownership is guaranteed by OAuth
     * provider.
     * This method MUST be used for OAuth flows to prevent TTL auto-deletion.
     *
     * @param email    User email from OAuth provider
     * @param name     User name from OAuth provider
     * @param role     User role (ROLE_USER or ROLE_RECRUITER)
     * @param provider OAuth provider (GOOGLE, LINKEDIN)
     * @return Saved user entity
     */
    public User createOrUpdateOAuthUser(String email, String name, String role, String provider) {
        User user = findByEmail(email);
        boolean isNewUser = (user == null);

        log.debug("createOrUpdateOAuthUser: email={}, isNewUser={}, requestedRole={}", email, isNewUser, role);

        if (isNewUser) {
            user = new User();
            user.setEmail(email);
            user.setPassword("");
            user.setAuthProvider(provider);
            user.setRole(role);
            log.debug("Creating NEW OAuth user with role: {}", role);
        } else {
            log.debug("Existing user found with role: {}", user.getRole());

            // Allow role updates for OAuth users
            if (!user.getRole().equals(role)) {
                log.debug("Role mismatch — updating from {} to {}", user.getRole(), role);
                user.setRole(role);
            } else {
                log.debug("Role matches, no update needed: {}", role);
            }
        }

        user.setName(name);

        // CRITICAL: OAuth users are pre-verified
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null); // No TTL - prevents auto-deletion

        User savedUser = userRepository.save(user);
        log.debug("Saved OAuth user: email={}, finalRole={}", savedUser.getEmail(), savedUser.getRole());

        return savedUser;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public boolean verifyUser(String email, String code) {
        User user = findByEmail(email);
        if (user == null || user.isVerified())
            return false;

        if (user.getVerificationCode() != null
                && user.getVerificationCode().equals(code)
                && user.getVerificationCodeExpiresAt().isAfter(java.time.LocalDateTime.now())) {

            user.setVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public String resendVerificationCode(String email) {
        User user = findByEmail(email);
        if (user == null || user.isVerified())
            return null;

        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);
        return code;
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }
}
