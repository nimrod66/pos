package com.example.pos.integration.sms.dto.v1;

public record SmsResponse(boolean success, String messageId, String error) {
    public static SmsResponse ok(String messageId) { return new SmsResponse(true, messageId, null); }
    public static SmsResponse fail(String error) { return new SmsResponse(false, null, error); }
}
