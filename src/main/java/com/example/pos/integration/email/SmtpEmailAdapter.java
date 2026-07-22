package com.example.pos.integration.email;

import com.example.pos.integration.config.FeatureFlagService;
import com.example.pos.integration.email.dto.v1.EmailRequest;
import com.example.pos.integration.email.dto.v1.EmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "pos.features.email", havingValue = "true")
public class SmtpEmailAdapter implements EmailAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final FeatureFlagService featureFlags;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public SmtpEmailAdapter(JavaMailSender mailSender, FeatureFlagService featureFlags) {
        this.mailSender = mailSender;
        this.featureFlags = featureFlags;
    }

    @Override
    public EmailResponse send(EmailRequest request) {
        if (!featureFlags.isEmailEnabled()) {
            return EmailResponse.fail("Email feature disabled");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            return EmailResponse.fail("SMTP sender address not configured");
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, request.attachments() != null && !request.attachments().isEmpty());
            helper.setFrom(fromAddress);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(request.body(), request.html());

            String messageId = UUID.randomUUID().toString() + "@pos-pharmacy.local";

            mailSender.send(message);
            log.info("Email sent to {}: {}", request.to(), request.subject());
            return EmailResponse.ok(messageId);
        } catch (Exception e) {
            log.error("Email send failed to {}: {}", request.to(), e.getMessage());
            return EmailResponse.fail(e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return featureFlags.isEmailEnabled() && fromAddress != null && !fromAddress.isBlank();
    }
}
