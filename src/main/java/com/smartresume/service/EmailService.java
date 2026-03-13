package com.smartresume.service;

import com.smartresume.model.HiringStage;
import com.smartresume.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.*;

@Service
public class EmailService {

        private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

        @Autowired
        private RestTemplate restTemplate;

        @Value("${resend.api.key}")
        private String resendApiKey;

        @Value("${resend.from.email}")
        private String fromEmail;

        private static final String RESEND_API_URL = "https://api.resend.com/emails";

        private void sendViaResend(String to, String subject, String body, List<Map<String, String>> attachments) {
                try {
                        Map<String, Object> payload = new HashMap<>();
                        String sender = (fromEmail != null && !fromEmail.trim().isEmpty()) ? fromEmail.trim()
                                        : "onboarding@resend.dev";

                        payload.put("from", "TalentSync <" + sender + ">");
                        payload.put("to", Collections.singletonList(to));
                        payload.put("subject", subject);

                        if (body != null && body.contains("<html") || body.contains("<div")) {
                                payload.put("html", body);
                        } else {
                                payload.put("text", body);
                        }

                        if (attachments != null && !attachments.isEmpty()) {
                                payload.put("attachments", attachments);
                        }

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set("Authorization", "Bearer " + resendApiKey);

                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                        restTemplate.postForEntity(RESEND_API_URL, entity, String.class);
                        logger.info("✅ Email sent via Resend API to: {}", to);
                } catch (HttpStatusCodeException e) {
                        String errorBody = e.getResponseBodyAsString();
                        logger.error("❌ Resend API Error ({}): {}", e.getStatusCode(), errorBody);
                        throw new RuntimeException("Resend API error: " + errorBody);
                } catch (Exception e) {
                        logger.error("❌ Failed to send email via Resend API to {}: {}", to, e.getMessage());
                        throw new RuntimeException("Email sending failed: " + e.getMessage());
                }
        }

        public void sendApplicationConfirmationEmail(String toEmail, String candidateName, String jobTitle, String company) {
                String subject = "Thank you for your application to " + company + " — " + jobTitle;
                String htmlBody = "<div style=\"font-family: 'Inter', Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">" +
                                "  <div style=\"background: linear-gradient(135deg, #2563eb, #4f46e5); padding: 32px; text-align: center;\">" +
                                "    <h1 style=\"color: white; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.02em;\">TalentSync</h1>" +
                                "  </div>" +
                                "  <div style=\"padding: 32px; background: white;\">" +
                                "    <h2 style=\"color: #1e293b; margin-top: 0; font-size: 20px;\">Dear " + candidateName + ",</h2>" +
                                "    <p style=\"color: #475569; line-height: 1.6; font-size: 16px;\">" +
                                "      Thank you for your application and interest in the position of <strong>" + jobTitle + "</strong> at <strong>" + company + "</strong>. " +
                                "      Our Talent Acquisition Team will review your application and be in touch shortly." +
                                "    </p>" +
                                "    <div style=\"margin: 32px 0; padding: 20px; background: #f8fafc; border-radius: 8px; border: 1px solid #f1f5f9;\">" +
                                "      <p style=\"margin: 0; color: #64748b; font-size: 14px;\">You can review your application status at any time on your dashboard:</p>" +
                                "      <a href=\"https://talentsynctech.in/candidate-dashboard.html\" style=\"display: inline-block; margin-top: 12px; padding: 12px 24px; background: #2563eb; color: white; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px;\">View Application Status</a>" +
                                "    </div>" +
                                "    <p style=\"color: #475569; line-height: 1.6; font-size: 16px;\">" +
                                "      Your username is: <strong style=\"color: #2563eb;\">" + toEmail + "</strong>" +
                                "    </p>" +
                                "    <p style=\"color: #475569; line-height: 1.6; font-size: 16px;\">" +
                                "      We wish you all the best with your application." +
                                "    </p>" +
                                "    <p style=\"color: #1e293b; margin-top: 32px; font-weight: 700;\">Best regards,<br>Talent Acquisition Team</p>" +
                                "  </div>" +
                                "  <div style=\"padding: 24px; background: #f8fafc; text-align: center; border-top: 1px solid #e2e8f0;\">" +
                                "    <p style=\"margin: 0; color: #94a3b8; font-size: 12px;\">&copy; 2026 TalentSync Technology. All rights reserved.</p>" +
                                "  </div>" +
                                "</div>";

                sendViaResend(toEmail, subject, htmlBody, null);
        }

        public void sendVerificationEmail(String toEmail, String code) {
                String subject = "Your TalentSync Verification Code";
                String body = "Welcome to TalentSync!\n\n" +
                                "Your verification code is: " + code + "\n\n" +
                                "This code will expire in 15 minutes.\n\n" +
                                "If you didn't request this code, please ignore this email.";

                sendViaResend(toEmail, subject, body, null);
        }

        public void sendStatusUpdateEmail(String toEmail, String candidateName, String jobTitle, String status,
                        String notes) {
                String subject = "";
                String body = "";
                String notesSection = (notes != null && !notes.isEmpty()) ? "\n\nRecruiter Notes: " + notes : "";

                switch (status) {
                        case "ATS_REJECTED":
                                subject = "Application Update — TalentSync";
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Thank you for applying for the position of \"" + jobTitle
                                                + "\" on TalentSync.\n\n" +
                                                "After reviewing your application through our automated screening system, "
                                                +
                                                "we regret to inform you that your profile does not meet the minimum requirements "
                                                +
                                                "for this role at this time.\n\n" +
                                                "We encourage you to strengthen the skills listed in your profile and apply again in the future."
                                                +
                                                notesSection + "\n\n" +
                                                "We wish you the best in your job search.\n\nBest regards,\nTalentSync Team";
                                break;

                        case "UNDER_REVIEW":
                                subject = "✅ Your Application Passed Screening — " + jobTitle;
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Great news! Your application for \"" + jobTitle
                                                + "\" has passed our initial ATS screening.\n\n" +
                                                "Your application is now under manual review by our recruiting team. " +
                                                "We will get back to you with the next steps shortly." +
                                                notesSection + "\n\n" +
                                                "Thank you for your patience.\n\nBest regards,\nTalentSync Team";
                                break;

                        case "SHORTLISTED":
                                subject = "🎉 You've Been Shortlisted! — " + jobTitle;
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Congratulations! We are pleased to inform you that you have been shortlisted "
                                                +
                                                "for the position of \"" + jobTitle + "\".\n\n" +
                                                "Our team was impressed with your profile and skills. " +
                                                "We will reach out to you soon with further steps." +
                                                notesSection + "\n\n" +
                                                "Well done and keep it up!\n\nBest regards,\nTalentSync Team";
                                break;

                        case "INTERVIEW":
                                subject = "📅 Interview Invitation — " + jobTitle;
                                body = "Dear " + candidateName + ",\n\n" +
                                                "We are excited to invite you for an interview for the position of \""
                                                + jobTitle + "\"!\n\n" +
                                                "Please expect a follow-up communication from our team with the interview details "
                                                +
                                                "including the date, time, and format (in-person / virtual)." +
                                                notesSection + "\n\n" +
                                                "Please reply to this email to confirm your availability.\n\n" +
                                                "Looking forward to speaking with you!\n\nBest regards,\nTalentSync Team";
                                break;

                        case "OFFERED":
                                subject = "🎉 Job Offer — " + jobTitle + " | TalentSync";
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Congratulations! We are thrilled to offer you the position of \""
                                                + jobTitle + "\"!\n\n" +
                                                "Our team was highly impressed with your skills and performance throughout the process. "
                                                +
                                                "Please expect a formal offer letter from our HR team shortly." +
                                                notesSection + "\n\n" +
                                                "Welcome to the team!\n\nBest regards,\nTalentSync Team";
                                break;

                        case "REJECTED":
                                subject = "Application Update — " + jobTitle;
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Thank you for your time and interest in the position of \"" + jobTitle
                                                + "\" at TalentSync.\n\n" +
                                                "After careful consideration, we have decided to move forward with other candidates "
                                                +
                                                "whose experience more closely matches our current requirements." +
                                                notesSection + "\n\n" +
                                                "We appreciate your effort and encourage you to apply for future openings that match your skills.\n\n"
                                                +
                                                "Best of luck in your career journey.\n\nBest regards,\nTalentSync Team";
                                break;

                        default:
                                subject = "Application Status Update — " + jobTitle;
                                body = "Dear " + candidateName + ",\n\n" +
                                                "Your application for \"" + jobTitle
                                                + "\" has been updated. Current status: " + status +
                                                notesSection + "\n\nBest regards,\nTalentSync Team";
                }

                sendViaResend(toEmail, subject, body, null);
        }

        /**
         * Send status update email using the job's custom pipeline template if
         * available,
         * otherwise fall back to the default system template.
         *
         * @param toEmail       Candidate email
         * @param candidateName Candidate full name
         * @param job           The job (may contain custom hiringPipeline)
         * @param stageKey      Status key e.g. "STAGE_1", "INTERVIEW", "SHORTLISTED"
         * @param stageName     Human-readable stage name e.g. "Round 1: Coding Test"
         * @param notes         Optional recruiter notes appended to the body
         */
        public void sendStatusUpdateEmailWithJob(String toEmail, String candidateName,
                        Job job, String stageKey, String stageName, String notes) {
                try {
                        if (job != null && job.getHiringPipeline() != null) {
                                for (HiringStage stage : job.getHiringPipeline()) {
                                        boolean matchesKey = stageKey.equalsIgnoreCase(stage.getStageKey());
                                        boolean matchesName = stageName != null
                                                        && stageName.equalsIgnoreCase(stage.getStageName());
                                        if (stage.isCustomTemplate() && (matchesKey || matchesName)) {
                                                String notesText = (notes != null && !notes.isEmpty()) ? notes : "";
                                                String subject = substitute(stage.getEmailSubject(), candidateName,
                                                                job.getTitle(),
                                                                stage.getStageName(), job.getCompany(), notesText,
                                                                null);
                                                String body = substitute(stage.getEmailBody(), candidateName,
                                                                job.getTitle(),
                                                                stage.getStageName(), job.getCompany(), notesText,
                                                                null);

                                                sendViaResend(toEmail, subject, body, null);
                                                logger.info("✅ Custom pipeline email sent for stage: {}",
                                                                stage.getStageName());
                                                return;
                                        }
                                }
                        }
                        // Fallback
                        sendStatusUpdateEmail(toEmail, candidateName, job != null ? job.getTitle() : "the position",
                                        stageKey,
                                        notes);
                } catch (Exception e) {
                        logger.error("Failed to send pipeline stage email to {}: {}", toEmail, e.getMessage());
                }
        }

        /** Replace template placeholders with actual values */
        private String substitute(String template, String candidateName, String jobTitle,
                        String stageName, String companyName, String notes, String interviewDateTime) {
                if (template == null)
                        return "";
                return template
                                .replace("{{candidateName}}", candidateName != null ? candidateName : "")
                                .replace("{{jobTitle}}", jobTitle != null ? jobTitle : "")
                                .replace("{{stageName}}", stageName != null ? stageName : "")
                                .replace("{{companyName}}", companyName != null ? companyName : "")
                                .replace("{{interviewDateTime}}", interviewDateTime != null ? interviewDateTime : "")
                                .replace("{{recruiterNotes}}",
                                                (notes != null && !notes.isEmpty())
                                                                ? "\n\nNote from recruiter: " + notes
                                                                : "");
        }

        /**
         * Send an interview invitation email with an .ics calendar invite attached.
         * Uses MimeMessage with multipart to include the ICS file.
         */
        public void sendInterviewEmailWithICS(String toEmail, String candidateName,
                        String jobTitle, String company, String interviewDateTimeStr, String notes) {
                try {
                        java.time.LocalDateTime interviewDt;
                        try {
                                interviewDt = java.time.LocalDateTime.parse(interviewDateTimeStr);
                        } catch (Exception e) {
                                interviewDt = java.time.LocalDateTime.now().plusDays(3);
                        }

                        String formattedDate = interviewDt
                                        .format(java.time.format.DateTimeFormatter.ofPattern(
                                                        "EEEE, MMMM d yyyy 'at' h:mm a",
                                                        java.util.Locale.ENGLISH));
                        String notesText = (notes != null && !notes.isEmpty()) ? notes : "";

                        String subject = "📅 Interview Invitation — " + jobTitle + " | " + company;
                        String body = "Dear " + candidateName + ",\n\n" +
                                        "We are delighted to invite you for an interview for the position of \""
                                        + jobTitle + "\" at "
                                        + company + ".\n\n" +
                                        "📅 Interview Date & Time: " + formattedDate + "\n" +
                                        "📍 Format: Please check the calendar invite attached to this email for join details.\n\n"
                                        +
                                        "A calendar invite (.ics) is attached — click it to save the event to your calendar."
                                        +
                                        ((notesText != null && !notesText.isEmpty())
                                                        ? "\n\nNote from recruiter: " + notesText
                                                        : "")
                                        + "\n\n" +
                                        "Please reply to this email to confirm your availability.\n\n" +
                                        "Best regards,\nRecruitment Team\n" + company;

                        // Build ICS content
                        String ics = buildICS(candidateName, jobTitle, company, interviewDt, toEmail);

                        Map<String, String> attachment = new HashMap<>();
                        attachment.put("filename", "interview-invite.ics");
                        attachment.put("content", Base64.getEncoder()
                                        .encodeToString(ics.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

                        sendViaResend(toEmail, subject, body, Collections.singletonList(attachment));
                        logger.info("✅ Interview email with ICS sent to {}", toEmail);
                } catch (Exception e) {
                        logger.error("Failed to send ICS email: {}", e.getMessage());
                        // Fallback to simple email
                        sendStatusUpdateEmail(toEmail, candidateName, jobTitle, "INTERVIEW", notes);
                }
        }

        private String buildICS(String candidateName, String jobTitle, String company, java.time.LocalDateTime dt,
                        String attendeeEmail) {
                String uid = java.util.UUID.randomUUID().toString();
                String dtStart = dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                String dtEnd = dt.plusHours(1)
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                String now = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

                String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@talentsync.in";

                return "BEGIN:VCALENDAR\r\n" +
                                "VERSION:2.0\r\n" +
                                "PRODID:-//TalentSync//Interview//EN\r\n" +
                                "METHOD:REQUEST\r\n" +
                                "BEGIN:VEVENT\r\n" +
                                "UID:" + uid + "\r\n" +
                                "DTSTART:" + dtStart + "\r\n" +
                                "DTEND:" + dtEnd + "\r\n" +
                                "DTSTAMP:" + now + "\r\n" +
                                "SUMMARY:Interview - " + jobTitle + " at " + company + "\r\n" +
                                "DESCRIPTION:Interview for " + jobTitle + " at " + company + ". Candidate: "
                                + candidateName + "\r\n" +
                                "ORGANIZER;CN=" + company + ":mailto:" + sender + "\r\n" +
                                "ATTENDEE;CN=" + candidateName + ";RSVP=TRUE:mailto:" + attendeeEmail + "\r\n" +
                                "STATUS:CONFIRMED\r\n" +
                                "BEGIN:VALARM\r\n" +
                                "TRIGGER:-PT30M\r\n" +
                                "ACTION:DISPLAY\r\n" +
                                "DESCRIPTION:Interview reminder\r\n" +
                                "END:VALARM\r\n" +
                                "END:VEVENT\r\n" +
                                "END:VCALENDAR\r\n";
        }

        /**
         * Notify a panel interviewer about their upcoming interview assignment.
         * Includes candidate info (name, email, ATS score, skills gap) + ICS.
         */
        /**
         * Notify a panel interviewer about their upcoming interview assignment.
         */
        public void sendInterviewerNotification(
                        String interviewerEmail, String interviewerName,
                        String candidateName, String candidateEmail,
                        String jobTitle, String company,
                        String interviewDateTimeStr, String panelName,
                        Double atsScore, java.util.List<String> skillsGap) {
                try {
                        java.time.LocalDateTime interviewDt;
                        try {
                                interviewDt = java.time.LocalDateTime.parse(interviewDateTimeStr);
                        } catch (Exception e) {
                                interviewDt = java.time.LocalDateTime.now().plusDays(3);
                        }

                        String formattedDate = interviewDt.format(
                                        java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a",
                                                        java.util.Locale.ENGLISH));

                        String scoreStr = atsScore != null ? String.format("%.1f%%", atsScore) : "N/A";
                        String gapStr = (skillsGap != null && !skillsGap.isEmpty())
                                        ? String.join(", ", skillsGap)
                                        : "None identified";

                        String subject = "📋 Interview Assignment — " + candidateName + " | " + jobTitle;
                        String emailBody = "Dear " + interviewerName + ",\n\n" +
                                        "You have been assigned to interview a candidate for the position of \""
                                        + jobTitle + "\" at "
                                        + company + ".\n\n" +
                                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                        "🎓 CANDIDATE DETAILS\n" +
                                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                        "Name:         " + candidateName + "\n" +
                                        "Email:        " + candidateEmail + "\n" +
                                        "ATS Score:    " + scoreStr + "\n" +
                                        "Skills Gap:   " + gapStr + "\n\n" +
                                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                        "📅 INTERVIEW DETAILS\n" +
                                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                        "Date & Time:  " + formattedDate + "\n" +
                                        "Panel:        " + panelName + "\n\n" +
                                        "A calendar invite is attached to this email.\n\n" +
                                        "Best regards,\nTalentSync Recruitment System";

                        String ics = buildICS(candidateName, jobTitle, company, interviewDt, interviewerEmail);

                        Map<String, String> attachment = new HashMap<>();
                        attachment.put("filename", "interview-" + candidateName.replaceAll("\\s+", "-") + ".ics");
                        attachment.put("content", Base64.getEncoder()
                                        .encodeToString(ics.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

                        sendViaResend(interviewerEmail, subject, emailBody, Collections.singletonList(attachment));
                        logger.info("✅ Interviewer notification sent to {}", interviewerEmail);
                } catch (Exception e) {
                        logger.error("Failed to notify interviewer {}: {}", interviewerEmail, e.getMessage());
                }
        }
}
