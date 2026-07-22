package com.example.pos.terminal.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "hardware_peripherals")
public class HardwarePeripheral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", referencedColumnName = "id", nullable = false)
    private Terminal terminal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeripheralType type;

    @Column(length = 50)
    private String manufacturer;

    @Column(length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 20)
    private ConnectionType connectionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PeripheralStatus status = PeripheralStatus.UNKNOWN;

    @Column(columnDefinition = "JSON", length = 2000)
    private String configuration;
}
