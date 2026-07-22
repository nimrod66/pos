package com.example.pos.integration.sms;

import com.example.pos.integration.sms.dto.v1.SmsRequest;
import com.example.pos.integration.sms.dto.v1.SmsResponse;

public interface SmsAdapter {
    SmsResponse send(SmsRequest request);
    boolean isAvailable();
}
