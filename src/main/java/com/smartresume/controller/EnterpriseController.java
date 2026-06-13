package com.smartresume.controller;

import com.smartresume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.smartresume.model.BatchJob;
import com.smartresume.repository.BatchJobRepository;
import com.smartresume.service.BatchProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.smartresume.model.User;
import com.smartresume.model.ResumeMeta;

import java.util.*;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@Slf4j
public class EnterpriseController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private BatchJobRepository batchJobRepository;

    @Autowired
    private BatchProcessingService batchProcessingService;
    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    /**
     * B2B Feature: Parse a JD file and return auto-fill data.
     */
    @PostMapping("/parse-jd")
    public ResponseEntity<?> parseJobDescription(@RequestParam("file") MultipartFile file) {
        try {
            // Temporarily store the file in GridFS under a dummy/system user, or just parse directly?
            // To reuse extractTextFromResume, we need to save it. But it's better to just parse it in memory if possible.
            // Since we updated ResumeService to require a saved ResumeMeta, we'll save it temporarily.
            // A cleaner B2B way:
            User systemUser = new User();
            systemUser.setId("system_enterprise");
            
            ResumeMeta jdMeta = resumeService.store(file, systemUser, "JD_DOC");
            String extractedText = resumeService.extractTextFromResume(jdMeta.getId());

            // Ask ML service to extract structured JD fields
            String url = mlServiceUrl + "/api/ml/extract-skills";
            Map<String, String> request = new HashMap<>();
            request.put("resumeText", extractedText);

            ResponseEntity<Map> response = null;
            Map<String, Object> responseBody = new HashMap<>();
            
            try {
                response = restTemplate.postForEntity(url, request, Map.class);
                if (response.getBody() != null) {
                    responseBody = response.getBody();
                }
            } catch (Exception mlEx) {
                log.warn("ML Service unavailable for JD parse, falling back to heuristics: {}", mlEx.getMessage());
            }
            
            List<String> techSkills = (List<String>) responseBody.getOrDefault("technicalSkills", Collections.emptyList());
            List<String> softSkills = (List<String>) responseBody.getOrDefault("softSkills", Collections.emptyList());
            
            String allSkills = String.join(", ", techSkills);
            if (!softSkills.isEmpty()) {
                allSkills += (allSkills.isEmpty() ? "" : ", ") + String.join(", ", softSkills);
            }

            // Heuristically extract a title from the first non-empty line
            String[] lines = extractedText.split("\n");
            String title = "Parsed Job Title";
            for (String line : lines) {
                String t = line.trim();
                if (!t.isEmpty() && !t.toLowerCase().contains("company:") && !t.toLowerCase().contains("location:")) {
                    title = t;
                    break;
                }
            }
            if (title.length() > 100) title = title.substring(0, 100);

            String company = extractRegex(extractedText, "(?i)Company:\\s*(.+)");
            String location = extractRegex(extractedText, "(?i)Location:\\s*(.+)");
            String jobType = extractRegex(extractedText, "(?i)Job Type:\\s*(.+)");
            String salary = extractRegex(extractedText, "(?i)Salary Range:\\s*(.+)");

            String description = "";
            String reqs = "";
            int descIdx = extractedText.toLowerCase().indexOf("job description");
            if (descIdx == -1) descIdx = extractedText.toLowerCase().indexOf("role summary");
            
            int reqIdx = extractedText.toLowerCase().indexOf("requirements");
            if (reqIdx == -1) reqIdx = extractedText.toLowerCase().indexOf("qualifications");
            if (reqIdx == -1) reqIdx = extractedText.toLowerCase().indexOf("skills");
            
            if (descIdx != -1 && reqIdx != -1 && reqIdx > descIdx) {
                int descEndLine = extractedText.indexOf("\n", descIdx);
                int reqEndLine = extractedText.indexOf("\n", reqIdx);
                description = extractedText.substring(descEndLine != -1 ? descEndLine : descIdx + 15, reqIdx).trim();
                reqs = extractedText.substring(reqEndLine != -1 ? reqEndLine : reqIdx + 12).trim();
            } else if (descIdx != -1) {
                int descEndLine = extractedText.indexOf("\n", descIdx);
                description = extractedText.substring(descEndLine != -1 ? descEndLine : descIdx + 15).trim();
                reqs = allSkills;
            } else if (reqIdx != -1) {
                int reqEndLine = extractedText.indexOf("\n", reqIdx);
                description = extractedText.substring(0, reqIdx).trim();
                reqs = extractedText.substring(reqEndLine != -1 ? reqEndLine : reqIdx + 12).trim();
            } else {
                description = extractedText;
                reqs = allSkills;
            }

            if ((reqs == null || reqs.isEmpty()) && (allSkills == null || allSkills.isEmpty())) {
                java.util.List<String> foundSkills = new java.util.ArrayList<>();
                String[] techKeywords = {
                    "java", "python", "react", "javascript", "spring", "node", "aws", "sql", "html", "css", 
                    "c++", "angular", "docker", "kubernetes", "git", "typescript", "c#", "ruby", "go", 
                    "mongodb", "redis", "postgresql", "mysql", "azure", "gcp", "linux", "jenkins", "ci/cd", 
                    "machine learning", "tensorflow", "pytorch", "django", "flask", "vue", "next.js", 
                    "express", "hibernate", "graphql", "rest api", "microservices", "agile", "scrum"
                };
                for (String kw : techKeywords) {
                    if (extractedText.toLowerCase().contains(kw) || extractedText.toLowerCase().contains(kw.replace(" ", ""))) {
                        if (kw.equals("aws") || kw.equals("sql") || kw.equals("gcp") || kw.equals("ci/cd")) {
                            foundSkills.add(kw.toUpperCase());
                        } else if (kw.equals("c++") || kw.equals("c#") || kw.equals("html") || kw.equals("css")) {
                            foundSkills.add(kw.toUpperCase());
                        } else {
                            foundSkills.add(kw.substring(0, 1).toUpperCase() + kw.substring(1));
                        }
                    }
                }
                reqs = String.join(", ", foundSkills);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("jobTitle", title);
            result.put("company", company);
            result.put("location", location);
            result.put("jobType", jobType);
            result.put("salary", salary);
            result.put("description", description);
            result.put("requirements", reqs.isEmpty() ? allSkills : reqs);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to parse JD file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * B2B Feature: Many-to-1 Batch Resume Screening
     */
    @PostMapping("/batch-screen")
    public ResponseEntity<?> batchScreen(
            @RequestParam("jdFile") MultipartFile jdFile,
            @RequestParam("resumes") MultipartFile[] resumes) {
        
        try {
            User systemUser = new User();
            systemUser.setId("system_enterprise");

            // Create Batch Job
            BatchJob job = new BatchJob();
            job.setStatus("PROCESSING");
            job.setTotalResumes(resumes.length);
            job.setProcessedResumes(0);
            batchJobRepository.save(job);

            // 1. Parse JD synchronously to extract text
            ResumeMeta jdMeta = resumeService.store(jdFile, systemUser, "JD_DOC");
            String jdText = resumeService.extractTextFromResume(jdMeta.getId());

            // 2. Store all resumes synchronously to prevent temp file destruction
            List<String> resumeIds = new ArrayList<>();
            Map<String, String> candidateNames = new HashMap<>();

            for (MultipartFile resumeFile : resumes) {
                ResumeMeta resMeta = resumeService.store(resumeFile, systemUser, "CANDIDATE_RESUME");
                String filename = resumeFile.getOriginalFilename();
                String candidateName = filename != null ? filename.replaceAll("(?i)\\.(pdf|txt|png|jpg|jpeg|doc|docx)$", "") : "Unknown Candidate";
                candidateNames.put(resMeta.getId(), candidateName);
                resumeIds.add(resMeta.getId());
            }

            // 3. Fire the Async Background Worker
            batchProcessingService.processBatch(job.getId(), jdText, resumeIds, candidateNames);

            // 4. Return immediately to the frontend
            return ResponseEntity.accepted().body(Map.of(
                "batchId", job.getId(),
                "status", "PROCESSING",
                "message", "Batch uploaded successfully. Background processing started."
            ));

        } catch (Exception e) {
            log.error("Batch screening initialization failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * B2B Feature: Many-to-Many Placement Cell Bulk Matching
     */
    @PostMapping("/placement-screen")
    public ResponseEntity<?> placementScreen(
            @RequestParam("jdFiles") MultipartFile[] jdFiles,
            @RequestParam("resumes") MultipartFile[] resumes) {
        
        try {
            User systemUser = new User();
            systemUser.setId("system_enterprise");

            BatchJob job = new BatchJob();
            job.setStatus("PROCESSING");
            job.setTotalResumes(resumes.length);
            job.setTotalJds(jdFiles.length);
            job.setProcessedResumes(0);
            batchJobRepository.save(job);

            List<Map<String, String>> tempJdFiles = new ArrayList<>();
            for (MultipartFile jdFile : jdFiles) {
                java.io.File tempFile = java.io.File.createTempFile("jd_", ".tmp");
                jdFile.transferTo(tempFile);
                
                String filename = jdFile.getOriginalFilename();
                String jdName = filename != null ? filename.replaceAll("(?i)\\.(pdf|txt|png|jpg|jpeg|doc|docx)$", "") : "Unknown JD";
                
                Map<String, String> jdMap = new HashMap<>();
                jdMap.put("path", tempFile.getAbsolutePath());
                jdMap.put("originalFilename", filename);
                jdMap.put("contentType", jdFile.getContentType());
                jdMap.put("name", jdName);
                tempJdFiles.add(jdMap);
            }

            List<Map<String, String>> tempResumeFiles = new ArrayList<>();
            for (MultipartFile resumeFile : resumes) {
                java.io.File tempFile = java.io.File.createTempFile("res_", ".tmp");
                resumeFile.transferTo(tempFile);
                
                String filename = resumeFile.getOriginalFilename();
                String candidateName = filename != null ? filename.replaceAll("(?i)\\.(pdf|txt|png|jpg|jpeg|doc|docx)$", "") : "Unknown Candidate";
                
                Map<String, String> resMap = new HashMap<>();
                resMap.put("path", tempFile.getAbsolutePath());
                resMap.put("originalFilename", filename);
                resMap.put("contentType", resumeFile.getContentType());
                resMap.put("name", candidateName);
                tempResumeFiles.add(resMap);
            }

            batchProcessingService.processPlacementBatchFromDisk(job.getId(), tempJdFiles, tempResumeFiles, systemUser);

            return ResponseEntity.accepted().body(Map.of(
                "batchId", job.getId(),
                "status", "PROCESSING",
                "message", "Placement batch uploaded successfully. Matrix processing started."
            ));

        } catch (Exception e) {
            log.error("Placement screening initialization failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<?> getBatchStatus(@PathVariable String batchId) {
        BatchJob job = batchJobRepository.findById(batchId).orElse(null);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Batch not found"));
        }
        return ResponseEntity.ok(job);
    }

    private String extractRegex(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
}
