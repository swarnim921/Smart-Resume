package com.smartresume.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        // Fallback if fromEmail is null (though it should be injected)
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
}
