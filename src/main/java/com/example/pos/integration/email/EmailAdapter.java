package com.example.pos.integration.email;

import com.example.pos.integration.email.dto.v1.EmailRequest;
import com.example.pos.integration.email.dto.v1.EmailResponse;

public interface EmailAdapter {
    EmailResponse send(EmailRequest request);
    boolean isAvailable();
}
