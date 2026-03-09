package com.smartresume.controller;

import com.smartresume.model.Application;
import com.smartresume.model.Job;
import com.smartresume.model.HiringStage;
import com.smartresume.repository.ApplicationRepository;
import com.smartresume.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller that generates smart email template suggestions for custom hiring
 * pipeline stages and provides email preview before sending.
 */
@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    /**
     * Preview the email that would be sent for a given status update — without
     * sending it.
     * POST /api/email-templates/preview
     */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Map<String, String>> previewEmail(@RequestBody Map<String, String> req) {
        String appId = req.get("appId");
        String status = req.getOrDefault("status", "");
        String notes = req.getOrDefault("notes", "");
        String stageName = req.getOrDefault("stageName", status);
        String interviewDateTime = req.getOrDefault("interviewDateTime", "");

        try {
            Application app = applicationRepository.findById(appId).orElse(null);
            Job job = app != null ? jobRepository.findById(app.getJobId()).orElse(null) : null;
            String candidateName = app != null ? app.getCandidateName() : "{{candidateName}}";
            String jobTitle = job != null ? job.getTitle() : "the position";
            String company = job != null ? (job.getCompany() != null ? job.getCompany() : "our company")
                    : "our company";
            String notesText = (notes != null && !notes.isEmpty()) ? notes : "";

            String subject = null;
            String body = null;

            // 1. Check if there's a custom template for this stage in the job pipeline
            if (job != null && job.getHiringPipeline() != null) {
                for (HiringStage hs : job.getHiringPipeline()) {
                    if (status.equalsIgnoreCase(hs.getStageKey()) || stageName.equalsIgnoreCase(hs.getStageName())) {
                        if (hs.isCustomTemplate() && hs.getEmailSubject() != null && hs.getEmailBody() != null) {
                            subject = hs.getEmailSubject();
                            body = hs.getEmailBody();
                            break;
                        }
                    }
                }
            }

            // 2. If no custom template, use the hardcoded system defaults
            if (subject == null || body == null) {
                if ("INTERVIEW".equals(status) && !interviewDateTime.isEmpty()) {
                    subject = "📅 Interview Invitation — {{jobTitle}} | {{companyName}}";
                    body = "Dear {{candidateName}},\n\nWe are delighted to invite you for an interview for \"{{jobTitle}}\" at {{companyName}}.\n\n📅 Date/Time: {{interviewDateTime}}\n\nA calendar invite (.ics) will be attached to the email.\n\n{{recruiterNotes}}\n\nPlease confirm your availability.\n\nBest regards,\nRecruitment Team, {{companyName}}";
                } else {
                    Map<String, String[]> templates = Map.of(
                            "UNDER_REVIEW", new String[] {
                                    "✅ Application Update — {{jobTitle}}",
                                    "Dear {{candidateName}},\n\nYour application for {{jobTitle}} at {{companyName}} has passed our initial screening and is now UNDER REVIEW by our recruiter.\n\n{{recruiterNotes}}\n\nWe'll be in touch with next steps.\n\nBest regards,\nRecruitment Team"
                            },
                            "SHORTLISTED", new String[] {
                                    "⭐ You've been Shortlisted — {{jobTitle}}",
                                    "Dear {{candidateName}},\n\nCongratulations! You have been SHORTLISTED for {{jobTitle}} at {{companyName}}.\n\n{{recruiterNotes}}\n\nWe will share next steps shortly.\n\nBest regards,\nRecruitment Team"
                            },
                            "OFFERED", new String[] {
                                    "🎉 Job Offer — {{jobTitle}}",
                                    "Dear {{candidateName}},\n\nWe are delighted to extend an offer for {{jobTitle}} at {{companyName}}!\n\n{{recruiterNotes}}\n\nOur HR team will reach out with the offer letter.\n\nBest regards,\nRecruitment Team"
                            },
                            "REJECTED", new String[] {
                                    "Application Update — {{jobTitle}}",
                                    "Dear {{candidateName}},\n\nThank you for your interest in {{jobTitle}} at {{companyName}}. After careful consideration, we have decided to move forward with other candidates.\n\n{{recruiterNotes}}\n\nWe encourage you to apply to future openings.\n\nBest regards,\nRecruitment Team"
                            },
                            "ATS_REJECTED", new String[] {
                                    "Application Update — {{jobTitle}}",
                                    "Dear {{candidateName}},\n\nThank you for applying for {{jobTitle}} at {{companyName}}. Our automated system indicates your current profile does not closely match this role's requirements.\n\n{{recruiterNotes}}\n\nWe encourage you to update your resume and apply to future roles.\n\nBest regards,\nRecruitment Team"
                            });

                    String[] tmpl = templates.get(status);
                    if (tmpl != null) {
                        subject = tmpl[0];
                        body = tmpl[1];
                    } else {
                        subject = "📋 Application Update — {{jobTitle}}";
                        body = "Dear {{candidateName}},\n\nYour application status for {{jobTitle}} at {{companyName}} has been updated to: {{stageName}}.\n\n{{recruiterNotes}}\n\nBest regards,\nRecruitment Team";
                    }
                }
            }

            // 3. Perform substitution for both custom and default templates
            subject = substitute(subject, candidateName, jobTitle, stageName, company, notesText, interviewDateTime);
            body = substitute(body, candidateName, jobTitle, stageName, company, notesText, interviewDateTime);

            return ResponseEntity.ok(Map.of("subject", subject, "body", body));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("subject", "Application Update", "body",
                    "Status update email will be sent to the candidate."));
        }
    }

    private String substitute(String template, String name, String title, String stage, String company, String notes,
            String dt) {
        if (template == null)
            return "";
        return template
                .replace("{{candidateName}}", name != null ? name : "")
                .replace("{{jobTitle}}", title != null ? title : "")
                .replace("{{stageName}}", stage != null ? stage : "")
                .replace("{{companyName}}", company != null ? company : "")
                .replace("{{interviewDateTime}}", dt != null ? dt : "")
                .replace("{{recruiterNotes}}",
                        (notes != null && !notes.isEmpty()) ? "\n\nNote from recruiter: " + notes : "");
    }return ResponseEntity.ok(Map.of("subject",subject,"body",body));}catch(

    Exception e)
    {
        return ResponseEntity.ok(Map.of("subject", "Application Update", "body",
                "Status update email will be sent to the candidate."));
    }
    }

    /**
     * Generate an email template suggestion based on stage name and job details.
     * POST /api/email-templates/generate
     * Body: { stageName, jobTitle, companyName, stageOrder }
     */

    @PostMapping("/generate")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Map<String, String>> generateTemplate(@RequestBody Map<String, String> request) {
        String stageName = request.getOrDefault("stageName", "").toLowerCase();
        String jobTitle = request.getOrDefault("jobTitle", "{{jobTitle}}");
        String company = request.getOrDefault("companyName", "our company");
        String order = request.getOrDefault("stageOrder", "");
        String rawName = request.getOrDefault("stageName", "Round " + order);

        String subject;
        String body;

        if (matches(stageName, "apti", "reasoning", "verbal", "english", "cognitive", "psycho")) {
            subject = "📝 Assessment Invitation — " + jobTitle + " | " + company;
            body = """
                    Dear {{candidateName}},

                    Thank you for applying for the position of {{jobTitle}} at {{companyName}}.

                    We are pleased to invite you to the next step in our hiring process — the Online Assessment for {{stageName}}.

                    📋 Assessment Details:
                    • Type: Aptitude & Reasoning Test
                    • Duration: 45–60 minutes
                    • Format: Online (link will be shared separately)
                    • Topics: Logical Reasoning, Quantitative Aptitude, Verbal Ability

                    Next Steps:
                    Please complete this assessment within the next 48 hours. A separate link will be sent to your email shortly.

                    Best of luck!

                    Warm regards,
                    Recruitment Team
                    {{companyName}}""";

        } else if (matches(stageName, "machine", "take-home", "assignment", "project", "task")) {
            subject = "📂 Take-Home Assignment — " + jobTitle + " | " + company;
            body = """
                    Hi {{candidateName}},

                    We're impressed with your profile and would like to see your skills in action!

                    We have assigned a take-home project for the {{stageName}} stage. This task is designed to simulate the kind of work you'll do at {{companyName}}.

                    📁 Assignment Details:
                    • Task: [Brief description of the project]
                    • Submission Deadline: [Date/Time]
                    • Format: GitHub Repository / Zip File

                    Please review the attached instructions carefully. We look forward to seeing your performance!

                    Warm regards,
                    Recruitment Team
                    {{companyName}}""";

        } else if (matches(stageName, "cod", "hack", "leet", "algorithm", "dsa", "programming", "technical test")) {
            subject = "💻 Coding Challenge — " + jobTitle + " | " + company;
            body = """
                    Dear {{candidateName}},

                    Congratulations on progressing to the {{stageName}} for the role of {{jobTitle}} at {{companyName}}.

                    We would like to invite you to complete an online coding challenge as part of our evaluation process.

                    💻 Challenge Details:
                    • Type: Coding / Data Structures & Algorithms
                    • Duration: 90 minutes
                    • Platform: HackerRank / LeetCode (link will be shared)
                    • Languages: Your choice of programming language

                    Tips:
                    - Read all problems before starting
                    - Focus on correctness first, then optimise
                    - Edge cases matter!

                    {{recruiterNotes}}

                    Best of luck! We look forward to reviewing your solutions.

                    Warm regards,
                    Recruitment Team
                    {{companyName}}""";

        } else if (matches(stageName, "technical", "tech interview", "tech round", "design")) {
            subject = "🔧 Technical Interview Scheduled — " + jobTitle + " | " + company;
            body = """
                    Dear {{candidateName}},

                    We are excited to invite you to the {{stageName}} for the position of {{jobTitle}} at {{companyName}}.

                    This technical round will focus on your core expertise, problem-solving abilities, and relevant experience.

                    📅 Interview Details:
                    • Date: [Date]
                    • Time: [Time]
                    • Mode: Video Call (Google Meet/Zoom)
                    • Duration: 60 minutes

                    We look forward to a great conversation!

                    Best regards,
                    Technical Hiring Team
                    {{companyName}}""";

        } else if (matches(stageName, "hr", "culture", "manager", "behavioral", "final", "fit")) {
            subject = "🤝 Discussion Invitation — " + jobTitle + " | " + company;
            body = """
                    Hi {{candidateName}},

                    It has been a pleasure getting to know you through the previous rounds. We would like to invite you for a {{stageName}} with our team.

                    This conversation will be an opportunity for us to discuss the role in more detail and for you to learn more about the team culture at {{companyName}}.

                    📅 Details:
                    • Date: [Date]
                    • Time: [Time]
                    • Mode: [In-person / Virtual]

                    {{recruiterNotes}}

                    See you soon!

                    Warmly,
                    Recruitment Team
                    {{companyName}}""";

        } else if (matches(stageName, "group", "gd", "discussion")) {
            subject = "🗣 Group Discussion — " + jobTitle + " | " + company;
            body = """
                    Dear {{candidateName}},

                    We are pleased to invite you to a Group Discussion as part of the selection process for {{jobTitle}} at {{companyName}}.

                    🗣 Group Discussion Details:
                    • Format: Video Call / In-Person
                    • Duration: 30–45 minutes
                    • Topics: Current affairs / Industry-related topics (announced on the day)

                    Tips:
                    - Listen actively to other participants
                    - Support your points with facts
                    - Be confident but respectful of others' opinions

                    {{recruiterNotes}}

                    We look forward to seeing how you engage and collaborate!

                    Warm regards,
                    Recruitment Team
                    {{companyName}}""";

        } else {
            subject = "📌 Update on your application: " + rawName + " — " + company;
            body = """
                    Dear {{candidateName}},

                    We are pleased to inform you that your application for {{jobTitle}} has moved to the next stage: {{stageName}}.

                    At {{companyName}}, we value talent and are eager to proceed with your evaluation.

                    Next Steps:
                    Our recruitment team will reach out to you shortly with specific instructions for this round.

                    Thank you for your patience and interest in {{companyName}}.

                    Best regards,
                    Talent Acquisition
                    {{companyName}}""";
        }

        body = body.replace("{{companyName}}", company);

        return ResponseEntity.ok(Map.of(
                "subject", subject,
                "body", body));
    }

    /** Check if the stage name contains any of the given keywords */
    private boolean matches(String stageName, String... keywords) {
        for (String kw : keywords) {
            if (stageName.contains(kw))
                return true;
        }
        return false;
    }
}
