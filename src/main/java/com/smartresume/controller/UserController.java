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
                .map(u -> {
                    java.util.Map<String, Object> profile = new java.util.HashMap<>();
                    profile.put("name", nvl(u.getName()));
                    profile.put("email", nvl(u.getEmail()));
                    profile.put("bio", nvl(u.getBio()));
                    profile.put("linkedinUrl", nvl(u.getLinkedinUrl()));
                    profile.put("githubUrl", nvl(u.getGithubUrl()));
                    profile.put("location", nvl(u.getLocation()));
                    profile.put("skills", u.getSkills() != null ? u.getSkills() : List.of());
                    profile.put("yearsOfExperience", u.getYearsOfExperience() != null ? u.getYearsOfExperience() : 0);
                    profile.put("role", nvl(u.getRole()));
                    profile.put("education", u.getEducation() != null ? u.getEducation() : List.of());
                    profile.put("projects", u.getProjects() != null ? u.getProjects() : List.of());
                    profile.put("experienceList", u.getExperienceList() != null ? u.getExperienceList() : List.of());
                    profile.put("certifications", u.getCertifications() != null ? u.getCertifications() : List.of());
                    profile.put("achievements", u.getAchievements() != null ? u.getAchievements() : List.of());
                    profile.put("preferredRoles", u.getPreferredRoles() != null ? u.getPreferredRoles() : List.of());
                    profile.put("preferredLocations", u.getPreferredLocations() != null ? u.getPreferredLocations() : List.of());
                    profile.put("languages", u.getLanguages() != null ? u.getLanguages() : List.of());
                    profile.put("availability", nvl(u.getAvailability()));
                    profile.put("profileCompletedAt", u.getProfileCompletedAt() != null ? u.getProfileCompletedAt().toString() : "");
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("name")) user.setName((String) body.get("name"));
            if (body.containsKey("bio")) user.setBio((String) body.get("bio"));
            if (body.containsKey("linkedinUrl")) user.setLinkedinUrl((String) body.get("linkedinUrl"));
            if (body.containsKey("githubUrl")) user.setGithubUrl((String) body.get("githubUrl"));
            if (body.containsKey("location")) user.setLocation((String) body.get("location"));
            if (body.containsKey("availability")) user.setAvailability((String) body.get("availability"));
            if (body.containsKey("yearsOfExperience") && body.get("yearsOfExperience") != null) {
                user.setYearsOfExperience(((Number) body.get("yearsOfExperience")).intValue());
            }

            if (body.containsKey("skills")) user.setSkills(castToStringList(body.get("skills")));
            if (body.containsKey("certifications")) user.setCertifications(castToStringList(body.get("certifications")));
            if (body.containsKey("achievements")) user.setAchievements(castToStringList(body.get("achievements")));
            if (body.containsKey("preferredRoles")) user.setPreferredRoles(castToStringList(body.get("preferredRoles")));
            if (body.containsKey("preferredLocations")) user.setPreferredLocations(castToStringList(body.get("preferredLocations")));
            if (body.containsKey("languages")) user.setLanguages(castToStringList(body.get("languages")));

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            if (body.containsKey("education")) {
                user.setEducation(mapper.convertValue(body.get("education"), new com.fasterxml.jackson.core.type.TypeReference<List<com.smartresume.model.Education>>() {}));
            }
            if (body.containsKey("projects")) {
                user.setProjects(mapper.convertValue(body.get("projects"), new com.fasterxml.jackson.core.type.TypeReference<List<com.smartresume.model.Project>>() {}));
            }
            if (body.containsKey("experienceList")) {
                user.setExperienceList(mapper.convertValue(body.get("experienceList"), new com.fasterxml.jackson.core.type.TypeReference<List<com.smartresume.model.Experience>>() {}));
            }

            user.setProfileCompletedAt(LocalDateTime.now());
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/profile/education")
    public ResponseEntity<?> addEducation(@RequestBody com.smartresume.model.Education edu, Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(u -> {
            if (u.getEducation() == null) u.setEducation(new java.util.ArrayList<>());
            u.getEducation().add(edu);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Education added"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/profile/projects")
    public ResponseEntity<?> addProject(@RequestBody com.smartresume.model.Project proj, Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(u -> {
            if (u.getProjects() == null) u.setProjects(new java.util.ArrayList<>());
            u.getProjects().add(proj);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Project added"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/profile/experience")
    public ResponseEntity<?> addExperience(@RequestBody com.smartresume.model.Experience exp, Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(u -> {
            if (u.getExperienceList() == null) u.setExperienceList(new java.util.ArrayList<>());
            u.getExperienceList().add(exp);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Experience added"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private List<String> castToStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream()
                    .filter(s -> s instanceof String)
                    .map(s -> (String) s)
                    .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
