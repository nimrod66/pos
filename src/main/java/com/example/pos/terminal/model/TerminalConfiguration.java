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
@Table(name = "terminal_configuration", uniqueConstraints = {
        @UniqueConstraint(name = "uk_terminal_config_key", columnNames = {"terminal_id", "config_key"})
})
public class TerminalConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", referencedColumnName = "id", nullable = false)
    private Terminal terminal;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", length = 1000)
    private String configValue;

    @Column(length = 500)
    private String description;
}
