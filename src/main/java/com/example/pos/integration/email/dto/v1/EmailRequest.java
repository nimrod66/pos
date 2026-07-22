package com.example.pos.integration.email.dto.v1;

import java.util.List;

public record EmailRequest(
        String to,
        String subject,
        String body,
        boolean html,
        List<String> attachments
) {}
