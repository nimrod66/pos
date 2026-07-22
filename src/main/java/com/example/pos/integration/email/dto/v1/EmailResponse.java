package com.example.pos.integration.email.dto.v1;

public record EmailResponse(boolean success, String messageId, String error) {
    public static EmailResponse ok(String messageId) {
        return new EmailResponse(true, messageId, null);
    }

    public static EmailResponse fail(String error) {
        return new EmailResponse(false, null, error);
    }
}
