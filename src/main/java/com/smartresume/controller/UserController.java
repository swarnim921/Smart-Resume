package com.smartresume.controller;

import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles candidate profile read/update.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(u -> ResponseEntity.ok(Map.of(
                        "name", nvl(u.getName()),
                        "email", nvl(u.getEmail()),
                        "bio", nvl(u.getBio()),
                        "linkedinUrl", nvl(u.getLinkedinUrl()),
                        "githubUrl", nvl(u.getGithubUrl()),
                        "location", nvl(u.getLocation()),
                        "skills", u.getSkills() != null ? u.getSkills() : List.of(),
                        "yearsOfExperience", u.getYearsOfExperience() != null ? u.getYearsOfExperience() : 0,
                        "role", nvl(u.getRole()),
                        "profileCompletedAt",
                        u.getProfileCompletedAt() != null ? u.getProfileCompletedAt().toString() : "")))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("name"))
                user.setName((String) body.get("name"));
            if (body.containsKey("bio"))
                user.setBio((String) body.get("bio"));
            if (body.containsKey("linkedinUrl"))
                user.setLinkedinUrl((String) body.get("linkedinUrl"));
            if (body.containsKey("githubUrl"))
                user.setGithubUrl((String) body.get("githubUrl"));
            if (body.containsKey("location"))
                user.setLocation((String) body.get("location"));
            if (body.containsKey("yearsOfExperience") && body.get("yearsOfExperience") != null) {
                user.setYearsOfExperience(((Number) body.get("yearsOfExperience")).intValue());
            }
            if (body.containsKey("skills")) {
                Object skillsRaw = body.get("skills");
                if (skillsRaw instanceof List<?> list) {
                    user.setSkills(list.stream()
                            .filter(s -> s instanceof String)
                            .map(s -> (String) s)
                            .collect(java.util.stream.Collectors.toList()));
                }
            }
            user.setProfileCompletedAt(LocalDateTime.now());
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
