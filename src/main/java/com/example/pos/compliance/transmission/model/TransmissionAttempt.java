package com.example.pos.compliance.transmission.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "transmission_attempts")
public class TransmissionAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transmission_id", nullable = false)
    private Transmission transmission;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "response_at")
    private LocalDateTime responseAt;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "success")
    private boolean success;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;
}
