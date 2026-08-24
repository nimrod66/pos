package com.example.pos.terminal.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "terminal_heartbeats", indexes = {
        @Index(name = "idx_heartbeat_terminal_time", columnList = "terminal_id, timestamp")
})
public class TerminalHeartbeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", referencedColumnName = "id", nullable = false)
    private Terminal terminal;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "battery_charging")
    private Boolean batteryCharging;

    @Column(name = "network_type", length = 20)
    private String networkType;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    @Column(name = "uptime_minutes")
    private Long uptimeMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", length = 2000)
    private String peripheralStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", length = 3000)
    private String additionalMetrics;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
