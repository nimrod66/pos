package com.example.pos.integration.sms.dto.v1;

public record SmsRequest(
        String to,
        String message,
        String senderId
) {}
