package com.smartresume.service;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(User user) {
        // Log incoming role
        System.out.println("UserService.register: Received user with role=" + user.getRole());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null)
            user.setRole("ROLE_USER");

        // Log final role before saving
        System.out.println("UserService.register: Saving user with role=" + user.getRole());

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
     * @param email User email from OAuth provider
     * @param name  User name from OAuth provider
     * @param role  User role (ROLE_USER or ROLE_RECRUITER)
     * @return Saved user entity
     */
    public User createOrUpdateOAuthUser(String email, String name, String role) {
        User user = findByEmail(email);
        boolean isNewUser = (user == null);

        System.out.println(
                "🔍 createOrUpdateOAuthUser: email=" + email + ", isNewUser=" + isNewUser + ", requestedRole=" + role);

        if (isNewUser) {
            // Create new OAuth user
            user = new User();
            user.setEmail(email);
            user.setPassword(""); // OAuth users don't have passwords
            user.setRole(role); // Set role ONLY for new users
            System.out.println("✅ Creating NEW OAuth user with role: " + role);
        } else {
            System.out.println("🔍 Existing user found with role: " + user.getRole() + " (will be preserved)");
        }

        // Update user details (name can change, but preserve existing role)
        user.setName(name);
        // DO NOT overwrite role for existing users - they may have signed up as
        // recruiter

        // CRITICAL: OAuth users are pre-verified
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null); // No TTL - prevents auto-deletion

        User savedUser = userRepository.save(user);
        System.out.println("💾 Saved OAuth user: email=" + savedUser.getEmail() + ", finalRole=" + savedUser.getRole());

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
