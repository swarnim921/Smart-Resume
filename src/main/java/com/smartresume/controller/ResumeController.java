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
                Map<String, Object> mlData = null;
                try {
                    mlData = mlIntegrationService.extractSkills(resumeText);
                } catch (Exception mlEx) {
                    System.err.println("ML Service failed or unreachable. Falling back to Java parser: " + mlEx.getMessage());
                }
                
                Map<String, Object> extracted = (mlData != null) ? mlData : new HashMap<>();
                
                String lower = resumeText.toLowerCase();
                
                // 1. Comprehensive Skills Extraction
                java.util.List<String> foundSkills = new java.util.ArrayList<>();
                String[] techKeywords = {
                    "java", "python", "react", "javascript", "spring", "node", "aws", "sql", "html", "css", 
                    "c++", "angular", "docker", "kubernetes", "git", "typescript", "c#", "ruby", "go", 
                    "mongodb", "redis", "postgresql", "mysql", "azure", "gcp", "linux", "jenkins", "ci/cd", 
                    "machine learning", "tensorflow", "pytorch", "django", "flask", "vue", "next.js", 
                    "express", "hibernate", "graphql", "rest api", "microservices", "agile", "scrum"
                };
                for (String kw : techKeywords) {
                    if (lower.contains(kw) || lower.contains(kw.replace(" ", ""))) {
                        if (kw.equals("aws") || kw.equals("sql") || kw.equals("gcp") || kw.equals("ci/cd")) {
                            foundSkills.add(kw.toUpperCase());
                        } else if (kw.equals("c++") || kw.equals("c#") || kw.equals("html") || kw.equals("css")) {
                            foundSkills.add(kw.toUpperCase());
                        } else {
                            foundSkills.add(kw.substring(0, 1).toUpperCase() + kw.substring(1));
                        }
                    }
                }
                // Override ML skills if Java found more
                if (!extracted.containsKey("technicalSkills") || foundSkills.size() > 2) {
                    extracted.put("technicalSkills", foundSkills);
                }
                
                // 2. Experience Extraction
                java.util.regex.Matcher mExp = java.util.regex.Pattern.compile("(\\d+)\\+?\\s*years?").matcher(lower);
                if (mExp.find()) {
                    extracted.put("experience", mExp.group(1) + " years");
                } else if (!extracted.containsKey("experience")) {
                    extracted.put("experience", "Not specified");
                }
                
                // 3. GitHub & LinkedIn Links
                java.util.regex.Matcher mGit = java.util.regex.Pattern.compile("github\\.com/([\\w-]+)").matcher(lower);
                if (mGit.find()) {
                    extracted.put("github", "https://github.com/" + mGit.group(1));
                }
                
                java.util.regex.Matcher mLink = java.util.regex.Pattern.compile("linkedin\\.com/in/([\\w-]+)").matcher(lower);
                if (mLink.find()) {
                    extracted.put("linkedin", "https://linkedin.com/in/" + mLink.group(1));
                }
                
                // 4. Education Extraction
                java.util.List<String> foundEd = new java.util.ArrayList<>();
                if (lower.contains("b.tech") || lower.contains("bachelor of technology") || lower.contains("b.e.")) foundEd.add("B.Tech / B.E.");
                else if (lower.contains("b.s.") || lower.contains("bachelor of science")) foundEd.add("B.S.");
                else if (lower.contains("b.a.") || lower.contains("bachelor of arts")) foundEd.add("B.A.");
                else if (lower.contains("bachelor") || lower.contains("degree")) foundEd.add("Bachelor's Degree");
                
                if (lower.contains("m.tech") || lower.contains("master of technology")) foundEd.add("M.Tech");
                else if (lower.contains("m.s.") || lower.contains("master of science")) foundEd.add("M.S.");
                else if (lower.contains("master")) foundEd.add("Master's Degree");
                
                java.util.regex.Matcher mUni = java.util.regex.Pattern.compile("([a-zA-Z\\s]+(?:university|college|institute|academy))", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(resumeText);
                String institution = "University / College";
                if (mUni.find()) {
                    String uni = mUni.group(1).trim();
                    if (uni.length() < 60) {
                        institution = uni.replaceAll("\\n", " ").trim();
                    }
                }
                if (foundEd.isEmpty() && !institution.equals("University / College")) {
                    foundEd.add("Degree"); // If we found a university but no degree name
                }
                if (!foundEd.isEmpty() || !institution.equals("University / College")) {
                    extracted.put("educationDegree", foundEd.isEmpty() ? "Degree" : foundEd.get(0));
                    extracted.put("educationInstitution", institution);
                }
                
                // 5. Projects
                java.util.List<String> foundProj = new java.util.ArrayList<>();
                java.util.regex.Matcher mProj = java.util.regex.Pattern.compile("(?i)project[s]?:?\\s*([\\s\\S]{10,300}?)(?=\\n\\n|education|experience|skills|$)").matcher(resumeText);
                if (mProj.find()) {
                    String pText = mProj.group(1).trim();
                    String[] pLines = pText.split("\\n");
                    for (String pl : pLines) {
                        String clean = pl.replaceAll("^[-*•]\\s*", "").trim();
                        if (clean.length() > 15 && clean.length() < 120) {
                            foundProj.add(clean);
                        }
                    }
                }
                if (foundProj.isEmpty()) {
                    // Aggressive fallback for projects
                    java.util.regex.Matcher mProj2 = java.util.regex.Pattern.compile("(?i)(?:built|developed|created|designed|implemented).*?([a-zA-Z0-9 ]{15,100})").matcher(resumeText.replaceAll("\\n", " "));
                    if (mProj2.find()) {
                        foundProj.add(mProj2.group().trim());
                    } else if (mGit.find(0)) {
                        foundProj.add("Personal Project (See GitHub: " + mGit.group(1) + ")");
                    }
                }
                extracted.put("projects", foundProj);
                
                // 6. Professional Experience Blocks
                java.util.List<Map<String, String>> expBlocks = new java.util.ArrayList<>();
                String[] commonRoles = {"Software Engineer", "Developer", "Intern", "Internship", "Manager", "Analyst", "Data Scientist", "Consultant", "Engineer", "Designer", "Lead"};
                for (String r : commonRoles) {
                    if (lower.contains(r.toLowerCase())) {
                        Map<String, String> eb = new HashMap<>();
                        String displayRole = r.equals("Internship") ? "Intern" : r;
                        eb.put("role", displayRole);
                        eb.put("comp", "Company / Organization");
                        eb.put("start", "2020");
                        eb.put("end", "Present");
                        eb.put("desc", "Worked in a professional capacity as a " + displayRole + ".");
                        expBlocks.add(eb);
                        if (expBlocks.size() >= 3) break; // Extract up to 3 blocks
                    }
                }
                extracted.put("experienceBlocks", expBlocks);
                
                // If explicit years wasn't found, estimate from experience blocks
                if (!extracted.containsKey("experience") || extracted.get("experience").equals("Not specified")) {
                    if (expBlocks.size() > 0) {
                        extracted.put("experience", expBlocks.size() + " years");
                    } else {
                        extracted.put("experience", "0 years");
                    }
                }

                response.put("extractedData", extracted);
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
