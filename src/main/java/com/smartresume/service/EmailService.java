package com.smartresume.service;

import com.smartresume.model.HiringStage;
import com.smartresume.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

        @Autowired
        private JavaMailSender mailSender;

        @Value("${spring.mail.username}")
        private String fromEmail;

        public void sendVerificationEmail(String toEmail, String code) {
                SimpleMailMessage message = new SimpleMailMessage();
                String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@talentsync.in";
                message.setFrom("TalentSync <" + sender + ">");
                message.setTo(toEmail);
                message.setSubject("Your TalentSync Verification Code");
                message.setText("Welcome to TalentSync!\n\n" +
                                "Your verification code is: " + code + "\n\n" +
                                "This code will expire in 15 minutes.\n\n" +
                                "If you didn't request this code, please ignore this email.");
                mailSender.send(message);
        }

        public void sendStatusUpdateEmail(String toEmail, String candidateName, String jobTitle, String status,
                        String notes) {
                try {
                        SimpleMailMessage message = new SimpleMailMessage();
                        String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail
                                        : "noreply@talentsync.in";
                        message.setFrom("TalentSync <" + sender + ">");
                        message.setTo(toEmail);

                        String subject;
                        String body;
                        String notesSection = (notes != null && !notes.isEmpty())
                                        ? "\n\nRecruiter Notes: " + notes
                                        : "";

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
                                                        "Your application is now under manual review by our recruiting team. "
                                                        +
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
                                                        + jobTitle
                                                        + "\"!\n\n" +
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
                                                        "Please expect a formal offer letter from our HR team shortly."
                                                        +
                                                        notesSection + "\n\n" +
                                                        "Welcome to the team!\n\nBest regards,\nTalentSync Team";
                                        break;

                                case "REJECTED":
                                        subject = "Application Update — " + jobTitle;
                                        body = "Dear " + candidateName + ",\n\n" +
                                                        "Thank you for your time and interest in the position of \""
                                                        + jobTitle
                                                        + "\" at TalentSync.\n\n" +
                                                        "After careful consideration, we have decided to move forward with other candidates "
                                                        +
                                                        "whose experience more closely matches our current requirements."
                                                        +
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

                        message.setSubject(subject);
                        message.setText(body);
                        mailSender.send(message);
                } catch (Exception e) {
                        // Don't crash the status update if email fails — log and continue
                        System.err.println("Failed to send status email to " + toEmail + ": " + e.getMessage());
                }
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
                        // Look for a custom template in the job's hiring pipeline
                        if (job != null && job.getHiringPipeline() != null) {
                                List<HiringStage> pipeline = job.getHiringPipeline();
                                for (HiringStage stage : pipeline) {
                                        boolean matchesKey = stageKey.equalsIgnoreCase(stage.getStageKey());
                                        boolean matchesName = stageName != null
                                                        && stageName.equalsIgnoreCase(stage.getStageName());
                                        if (stage.isCustomTemplate() && (matchesKey || matchesName)) {
                                                // Use the recruiter's custom template with placeholder substitution
                                                String notesText = (notes != null && !notes.isEmpty()) ? notes : "";
                                                String subject = substitute(stage.getEmailSubject(), candidateName,
                                                                job.getTitle(),
                                                                stage.getStageName(), job.getCompany(), notesText);
                                                String body = substitute(stage.getEmailBody(), candidateName,
                                                                job.getTitle(),
                                                                stage.getStageName(), job.getCompany(), notesText);

                                                SimpleMailMessage message = new SimpleMailMessage();
                                                String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail
                                                                : "noreply@talentsync.in";
                                                message.setFrom("TalentSync <" + sender + ">");
                                                message.setTo(toEmail);
                                                message.setSubject(subject);
                                                message.setText(body);
                                                mailSender.send(message);
                                                System.out.println("✅ Custom pipeline email sent for stage: "
                                                                + stage.getStageName());
                                                return; // Custom template sent — done
                                        }
                                }
                        }
                        // No custom template found — fall back to default
                        sendStatusUpdateEmail(toEmail, candidateName, job != null ? job.getTitle() : "the position",
                                        stageKey,
                                        notes);
                } catch (Exception e) {
                        System.err.println("Failed to send pipeline stage email to " + toEmail + ": " + e.getMessage());
                }
        }

        /** Replace template placeholders with actual values */
        private String substitute(String template, String candidateName, String jobTitle,
                        String stageName, String companyName, String notes) {
                if (template == null)
                        return "";
                return template
                                .replace("{{candidateName}}", candidateName != null ? candidateName : "")
                                .replace("{{jobTitle}}", jobTitle != null ? jobTitle : "")
                                .replace("{{stageName}}", stageName != null ? stageName : "")
                                .replace("{{companyName}}", companyName != null ? companyName : "")
                                .replace("{{recruiterNotes}}",
                                                notes != null && !notes.isEmpty() ? "Note: " + notes : "");
        }

        /**
         * Send an interview invitation email with an .ics calendar invite attached.
         * Uses MimeMessage with multipart to include the ICS file.
         */
        public void sendInterviewEmailWithICS(String toEmail, String candidateName,
                        String jobTitle, String company, String interviewDateTimeStr, String notes) {
                try {
                        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        message, true, "UTF-8");

                        String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail
                                        : "noreply@talentsync.in";
                        helper.setFrom("TalentSync <" + sender + ">");
                        helper.setTo(toEmail);
                        helper.setSubject("📅 Interview Invitation — " + jobTitle + " | " + company);

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
                        String notesText = (notes != null && !notes.isEmpty()) ? "\n\nAdditional Notes: " + notes : "";

                        String emailBody = "Dear " + candidateName + ",\n\n" +
                                        "We are delighted to invite you for an interview for the position of \""
                                        + jobTitle + "\" at "
                                        + company + ".\n\n" +
                                        "📅 Interview Date & Time: " + formattedDate + "\n" +
                                        "📍 Format: Please check the calendar invite attached to this email for join details.\n\n"
                                        +
                                        "A calendar invite (.ics) is attached — click it to save the event to your calendar."
                                        +
                                        notesText + "\n\n" +
                                        "Please reply to this email to confirm your availability.\n\n" +
                                        "Best regards,\nRecruitment Team\n" + company;

                        helper.setText(emailBody);

                        // Build ICS content
                        String uid = java.util.UUID.randomUUID().toString();
                        String dtStart = interviewDt
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                        String dtEnd = interviewDt.plusHours(1)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                        String now = java.time.LocalDateTime.now()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

                        String ics = "BEGIN:VCALENDAR\r\n" +
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
                                        + candidateName
                                        + "\r\n" +
                                        "ORGANIZER;CN=" + company + ":mailto:" + sender + "\r\n" +
                                        "ATTENDEE;CN=" + candidateName + ";RSVP=TRUE:mailto:" + toEmail + "\r\n" +
                                        "STATUS:CONFIRMED\r\n" +
                                        "BEGIN:VALARM\r\n" +
                                        "TRIGGER:-PT30M\r\n" +
                                        "ACTION:DISPLAY\r\n" +
                                        "DESCRIPTION:Interview reminder\r\n" +
                                        "END:VALARM\r\n" +
                                        "END:VEVENT\r\n" +
                                        "END:VCALENDAR\r\n";

                        byte[] icsBytes = ics.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        helper.addAttachment("interview-invite.ics",
                                        new org.springframework.core.io.ByteArrayResource(icsBytes),
                                        "text/calendar; charset=UTF-8; method=REQUEST");

                        mailSender.send(message);
                        System.out.println("✅ Interview email with ICS sent to " + toEmail);
                } catch (Exception e) {
                        System.err.println("Failed to send ICS email: " + e.getMessage());
                        // Fallback to simple email
                        sendStatusUpdateEmail(toEmail, candidateName, jobTitle, "INTERVIEW", notes);
                }
        }

        /**
         * Notify a panel interviewer about their upcoming interview assignment.
         * Includes candidate info (name, email, ATS score, skills gap) + ICS.
         */
        public void sendInterviewerNotification(
                        String interviewerEmail, String interviewerName,
                        String candidateName, String candidateEmail,
                        String jobTitle, String company,
                        String interviewDateTimeStr, String panelName,
                        Double atsScore, java.util.List<String> skillsGap) {
                try {
                        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
                        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                                        message, true, "UTF-8");

                        String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail
                                        : "noreply@talentsync.in";
                        helper.setFrom("TalentSync <" + sender + ">");
                        helper.setTo(interviewerEmail);
                        helper.setSubject("📋 Interview Assignment — " + candidateName + " | " + jobTitle);

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

                        helper.setText(emailBody);

                        // Build ICS for interviewer
                        String uid = java.util.UUID.randomUUID().toString();
                        String dtStart = interviewDt
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                        String dtEnd = interviewDt.plusHours(1)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                        String now = java.time.LocalDateTime.now()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

                        String ics = "BEGIN:VCALENDAR\r\n" +
                                        "VERSION:2.0\r\n" +
                                        "PRODID:-//TalentSync//Interview//EN\r\n" +
                                        "METHOD:REQUEST\r\n" +
                                        "BEGIN:VEVENT\r\n" +
                                        "UID:" + uid + "\r\n" +
                                        "DTSTART:" + dtStart + "\r\n" +
                                        "DTEND:" + dtEnd + "\r\n" +
                                        "DTSTAMP:" + now + "\r\n" +
                                        "SUMMARY:Interview: " + candidateName + " for " + jobTitle + "\r\n" +
                                        "DESCRIPTION:Candidate: " + candidateName + " (" + candidateEmail
                                        + ")\\nATS Score: " + scoreStr
                                        + "\\nPanel: " + panelName + "\r\n" +
                                        "ORGANIZER;CN=" + company + ":mailto:" + sender + "\r\n" +
                                        "ATTENDEE;CN=" + interviewerName + ";RSVP=TRUE:mailto:" + interviewerEmail
                                        + "\r\n" +
                                        "STATUS:CONFIRMED\r\n" +
                                        "BEGIN:VALARM\r\n" +
                                        "TRIGGER:-PT30M\r\n" +
                                        "ACTION:DISPLAY\r\n" +
                                        "DESCRIPTION:Interview reminder: " + candidateName + "\r\n" +
                                        "END:VALARM\r\n" +
                                        "END:VEVENT\r\n" +
                                        "END:VCALENDAR\r\n";

                        byte[] icsBytes = ics.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        helper.addAttachment("interview-" + candidateName.replaceAll("\\s+", "-") + ".ics",
                                        new org.springframework.core.io.ByteArrayResource(icsBytes),
                                        "text/calendar; charset=UTF-8; method=REQUEST");

                        mailSender.send(message);
                        System.out.println("✅ Interviewer notification sent to " + interviewerEmail);
                } catch (Exception e) {
                        System.err.println("Failed to notify interviewer " + interviewerEmail + ": " + e.getMessage());
                }
        }
}
