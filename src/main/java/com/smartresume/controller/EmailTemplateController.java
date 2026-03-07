package com.smartresume.controller;

import com.smartresume.model.HiringStage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller that generates smart email template suggestions for custom hiring
 * pipeline stages.
 * Recruiter provides stage name + job info → gets a professional template they
 * can edit/replace.
 */
@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    /**
     * Generate an email template suggestion based on stage name and job details.
     * POST /api/email-templates/generate
     * Body: { stageName, jobTitle, companyName, stageOrder }
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Map<String, String>> generateTemplate(@RequestBody Map<String, String> request) {
        String stageName = request.getOrDefault("stageName", "").toLowerCase();
        String jobTitle  = request.getOrDefault("jobTitle", "{{jobTitle}}");
        String company   = request.getOrDefault("companyName", "our company");
        String order     = request.getOrDefault("stageOrder", "");
        String rawName   = request.getOrDefault("stageName", "Round " + order);

        String subject;
        String body;

        if (matches(stageName, "apti", "reasoning", "verbal", "english", "cognitive", "psycho")) {
            subject = "📝 Assessment Invitation — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

Thank you for applying for the position of {{jobTitle}} at """ + company + """.

We are pleased to invite you to the next step in our hiring process — the Online Assessment for {{stageName}}.

📋 Assessment Details:
• Type: Aptitude & Reasoning Test
• Duration: 45–60 minutes
• Format: Online (link will be shared separately)
• Topics: Logical Reasoning, Quantitative Aptitude, Verbal Ability

Please ensure you complete the test in a quiet environment with a stable internet connection.

{{recruiterNotes}}

We look forward to seeing your performance!

Warm regards,
Recruitment Team
""" + company;

        } else if (matches(stageName, "cod", "hack", "leet", "algorithm", "dsa", "programming", "technical test")) {
            subject = "💻 Coding Challenge — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

Congratulations on progressing to the {{stageName}} for the role of {{jobTitle}} at """ + company + """.

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
""" + company;

        } else if (matches(stageName, "technical", "tech interview", "tech round", "design")) {
            subject = "🔧 Technical Interview Scheduled — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

We are excited to invite you to the {{stageName}} for the position of {{jobTitle}} at """ + company + """.

🔧 Interview Details:
• Format: Video Call / In-Person
• Duration: 60–90 minutes
• Topics: Technical concepts, problem-solving, system design
• Platform: Google Meet / Zoom (details to follow)

What to expect:
- Core technical questions relevant to the role
- Live coding or whiteboard exercise
- Discussion of your past projects and experience

{{recruiterNotes}}

Please reply to this email to confirm your availability or suggest an alternative time.

Warm regards,
Recruitment Team
""" + company;

        } else if (matches(stageName, "hr", "culture", "people", "behavioural", "behavioral", "soft")) {
            subject = "🤝 HR Interview Invitation — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

Congratulations on making it to the {{stageName}} for the role of {{jobTitle}} at """ + company + """.

We would like to schedule an HR Discussion to learn more about you and share more about our culture.

🤝 Interview Details:
• Format: Video Call
• Duration: 30–45 minutes
• Focus: Your background, career goals, team fit, and company culture

Topics we may discuss:
- Your motivation for joining us
- Work style and collaboration approach
- Career aspirations and growth areas
- Compensation expectations (if applicable)

{{recruiterNotes}}

This is a great opportunity for us to get to know each other better!

Warm regards,
Recruitment Team
""" + company;

        } else if (matches(stageName, "managerial", "manager", "director", "panel", "leadership")) {
            subject = "👔 Managerial Round — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

We are pleased to invite you to the {{stageName}} for the position of {{jobTitle}} at """ + company + """.

👔 Round Details:
• Format: Video Call / In-Person
• Duration: 45–60 minutes
• Participants: Hiring Manager / Senior Leadership

What to expect:
- Discussion of strategic thinking and leadership abilities
- Situational and behavioural questions
- Your approach to team dynamics and problem-solving at scale

{{recruiterNotes}}

We look forward to speaking with you!

Warm regards,
Recruitment Team
""" + company;

        } else if (matches(stageName, "final", "last", "cxo", "cto", "ceo", "founder")) {
            subject = "🏁 Final Round — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

Excellent news! You have made it to the Final Round of our selection process for {{jobTitle}} at """ + company + """.

🏁 Final Round Details:
• Format: In-Person / Video Call
• Duration: 60 minutes
• Participants: Senior Leadership / Decision Makers

This is the last step before we make our decision. We are very excited to meet you.

{{recruiterNotes}}

Please confirm your availability at your earliest convenience.

Warm regards,
Recruitment Team
""" + company;

        } else if (matches(stageName, "group", "gd", "discussion")) {
            subject = "🗣 Group Discussion — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

We are pleased to invite you to a Group Discussion as part of the selection process for {{jobTitle}} at """ + company + """.

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
""" + company;

        } else {
            // Generic professional template
            subject = "📋 " + rawName + " — " + jobTitle + " | " + company;
            body = """
Dear {{candidateName}},

Congratulations on progressing to the next stage of our hiring process for {{jobTitle}} at """ + company + """.

You have been invited to: {{stageName}}

📋 Details:
• Format: To be confirmed
• Duration: To be confirmed

We will share further details about the timing and format shortly.

{{recruiterNotes}}

Please reply to this email if you have any questions.

Warm regards,
Recruitment Team
""" + company;
        }

        return ResponseEntity.ok(Map.of("subject", subject, "body", body.trim()));
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
