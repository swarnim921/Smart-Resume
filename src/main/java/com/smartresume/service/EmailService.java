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
        message.setFrom(sender);
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
            String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@talentsync.in";
            message.setFrom(sender);
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
                            "Thank you for applying for the position of \"" + jobTitle + "\" on TalentSync.\n\n" +
                            "After reviewing your application through our automated screening system, " +
                            "we regret to inform you that your profile does not meet the minimum requirements " +
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
                            "Congratulations! We are pleased to inform you that you have been shortlisted " +
                            "for the position of \"" + jobTitle + "\".\n\n" +
                            "Our team was impressed with your profile and skills. " +
                            "We will reach out to you soon with further steps." +
                            notesSection + "\n\n" +
                            "Well done and keep it up!\n\nBest regards,\nTalentSync Team";
                    break;

                case "INTERVIEW":
                    subject = "📅 Interview Invitation — " + jobTitle;
                    body = "Dear " + candidateName + ",\n\n" +
                            "We are excited to invite you for an interview for the position of \"" + jobTitle
                            + "\"!\n\n" +
                            "Please expect a follow-up communication from our team with the interview details " +
                            "including the date, time, and format (in-person / virtual)." +
                            notesSection + "\n\n" +
                            "Please reply to this email to confirm your availability.\n\n" +
                            "Looking forward to speaking with you!\n\nBest regards,\nTalentSync Team";
                    break;

                case "OFFERED":
                    subject = "🎉 Job Offer — " + jobTitle + " | TalentSync";
                    body = "Dear " + candidateName + ",\n\n" +
                            "Congratulations! We are thrilled to offer you the position of \"" + jobTitle + "\"!\n\n" +
                            "Our team was highly impressed with your skills and performance throughout the process. " +
                            "Please expect a formal offer letter from our HR team shortly." +
                            notesSection + "\n\n" +
                            "Welcome to the team!\n\nBest regards,\nTalentSync Team";
                    break;

                case "REJECTED":
                    subject = "Application Update — " + jobTitle;
                    body = "Dear " + candidateName + ",\n\n" +
                            "Thank you for your time and interest in the position of \"" + jobTitle
                            + "\" at TalentSync.\n\n" +
                            "After careful consideration, we have decided to move forward with other candidates " +
                            "whose experience more closely matches our current requirements." +
                            notesSection + "\n\n" +
                            "We appreciate your effort and encourage you to apply for future openings that match your skills.\n\n"
                            +
                            "Best of luck in your career journey.\n\nBest regards,\nTalentSync Team";
                    break;

                default:
                    subject = "Application Status Update — " + jobTitle;
                    body = "Dear " + candidateName + ",\n\n" +
                            "Your application for \"" + jobTitle + "\" has been updated. Current status: " + status +
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
                    boolean matchesName = stageName != null && stageName.equalsIgnoreCase(stage.getStageName());
                    if (stage.isCustomTemplate() && (matchesKey || matchesName)) {
                        // Use the recruiter's custom template with placeholder substitution
                        String notesText = (notes != null && !notes.isEmpty()) ? notes : "";
                        String subject = substitute(stage.getEmailSubject(), candidateName, job.getTitle(),
                                stage.getStageName(), job.getCompany(), notesText);
                        String body = substitute(stage.getEmailBody(), candidateName, job.getTitle(),
                                stage.getStageName(), job.getCompany(), notesText);

                        SimpleMailMessage message = new SimpleMailMessage();
                        String sender = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail
                                : "noreply@talentsync.in";
                        message.setFrom(sender);
                        message.setTo(toEmail);
                        message.setSubject(subject);
                        message.setText(body);
                        mailSender.send(message);
                        System.out.println("✅ Custom pipeline email sent for stage: " + stage.getStageName());
                        return; // Custom template sent — done
                    }
                }
            }
            // No custom template found — fall back to default
            sendStatusUpdateEmail(toEmail, candidateName, job != null ? job.getTitle() : "the position", stageKey,
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
                .replace("{{recruiterNotes}}", notes != null && !notes.isEmpty() ? "Note: " + notes : "");
    }
}
