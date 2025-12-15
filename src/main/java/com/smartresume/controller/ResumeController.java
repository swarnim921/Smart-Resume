package com.smartresume.controller;

import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.UserRepository;
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

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

        private final ResumeService resumeService;
        private final UserRepository userRepository;

        @PostMapping("/upload")
        public ResponseEntity<?> upload(@RequestParam("resume") MultipartFile file, Authentication auth)
                        throws Exception {

                // Validation
                if (file == null || file.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Resume file is required"));
                }

                // Validate file type (PDF only)
                String contentType = file.getContentType();
                if (contentType == null || !contentType.equals("application/pdf")) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are allowed"));
                }

                // Validate file size (max 20MB)
                long maxSize = 20 * 1024 * 1024; // 20MB in bytes
                if (file.getSize() > maxSize) {
                        return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 20MB limit"));
                }

                // Validate filename
                String filename = file.getOriginalFilename();
                if (filename == null || filename.isBlank()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename"));
                }

                String email = auth.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                ResumeMeta meta = resumeService.store(file, user);

                return ResponseEntity.ok(
                                Map.of(
                                                "resumeId", meta.getId(),
                                                "gridFsId", meta.getGridFsId(),
                                                "filename", meta.getFilename(),
                                                "contentType", meta.getContentType()));
        }

        @GetMapping("/{id}/download")
        public ResponseEntity<?> download(@PathVariable String id) throws Exception {

                ResumeMeta meta = resumeService.findById(id)
                                .orElseThrow(() -> new NoSuchElementException("Resume not found"));

                GridFsResource resource = resumeService.getFileResourceByGridId(meta.getGridFsId());

                if (resource == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(meta.getContentType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + meta.getFilename() + "\"")
                                .body(new InputStreamResource(resource.getInputStream()));
        }
}
