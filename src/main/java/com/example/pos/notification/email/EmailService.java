package com.example.pos.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender;
        this.enabled = username != null && !username.isBlank();
        if (!enabled) {
            log.warn("SMTP not configured (spring.mail.username is empty); email sending disabled");
        }
    }

    public void sendWelcomeEmail(String to, String firstName, String email, String tempPassword) {
        if (!enabled) {
            log.info("Email disabled — would send welcome email to {}", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Welcome to Pharmacy POS");
        msg.setText(String.format("""
                Hello %s,

                Your account has been created in the Pharmacy POS system.

                Email: %s
                Temporary Password: %s

                Please log in and change your password immediately.

                Regards,
                Pharmacy POS Team
                """, firstName, email, tempPassword));
        mailSender.send(msg);
        log.info("Welcome email sent to {}", to);
    }

    public void sendPasswordReset(String to, String firstName, String token) {
        if (!enabled) {
            log.info("Email disabled — would send password reset to {}", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Password Reset - Pharmacy POS");
        msg.setText(String.format("""
                Hello %s,

                You requested a password reset. Use the token below:

                %s

                If you did not request this, please ignore this email.

                Regards,
                Pharmacy POS Team
                """, firstName, token));
        mailSender.send(msg);
        log.info("Password reset email sent to {}", to);
    }

    public void sendStockAlert(String to, String firstName, String subject, String body) {
        if (!enabled) {
            log.info("Email disabled — would send stock alert '{}' to {}", subject, to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Pharmacy POS Alert: " + subject);
        msg.setText(String.format("""
                Hello %s,

                %s

                Regards,
                Pharmacy POS Team
                """, firstName, body));
        mailSender.send(msg);
        log.info("Stock alert email sent to {}: {}", to, subject);
    }
}
