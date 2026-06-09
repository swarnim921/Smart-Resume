package com.smartresume.controller;

import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
import com.smartresume.service.MLIntegrationService;
import com.smartresume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;
    private final UserRepository userRepository;
    private final MLIntegrationService mlIntegrationService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", defaultValue = "RESUME") String fileType,
            Authentication auth) throws Exception {
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        ResumeMeta meta = resumeService.store(file, user, fileType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("resumeId", meta.getId());
        response.put("gridFsId", meta.getGridFsId());
        response.put("fileName", meta.getFilename());

        if ("RESUME".equalsIgnoreCase(fileType)) {
            try {
                String resumeText = resumeService.extractTextFromResume(meta.getId());
                Map<String, Object> mlData = mlIntegrationService.extractSkills(resumeText);
                if (mlData != null) {
                    response.put("extractedData", mlData);
                } else {
                    // Fallback heuristics if Python ML service is offline or unreachable
                    Map<String, Object> fallback = new HashMap<>();
                    java.util.List<String> foundSkills = new java.util.ArrayList<>();
                    String lower = resumeText.toLowerCase();
                    String[] techKeywords = {"java", "python", "react", "javascript", "spring", "node", "aws", "sql", "html", "css", "c++", "angular", "docker", "kubernetes", "git"};
                    for (String kw : techKeywords) {
                        if (lower.contains(kw)) foundSkills.add(kw.substring(0, 1).toUpperCase() + kw.substring(1));
                    }
                    fallback.put("technicalSkills", foundSkills);
                    fallback.put("softSkills", new java.util.ArrayList<>());
                    
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\+?\\s*years?").matcher(lower);
                    if (m.find()) {
                        fallback.put("experience", m.group(1) + " years");
                    } else {
                        fallback.put("experience", "Not specified");
                    }
                    response.put("extractedData", fallback);
                }
            } catch (Exception e) {
                // Log and ignore to not fail the upload just because extraction failed
                e.printStackTrace();
            }
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable String id) throws Exception {
        ResumeMeta meta = resumeService.findById(id).orElseThrow(() -> new NoSuchElementException("Resume not found"));
        GridFsResource resource = resumeService.getFileResourceByGridId(meta.getGridFsId());
        if (resource == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFilename() + "\"")
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
