package com.example.pos.terminal.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "terminal_sessions")
public class TerminalSession extends BaseEntity {

    @Column(name = "session_id", length = 36, nullable = false, unique = true)
    @Builder.Default
    private String sessionId = java.util.UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", referencedColumnName = "id", nullable = false)
    private Terminal terminal;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "cashier_id")
    private UUID cashierId;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean active = true;
}
