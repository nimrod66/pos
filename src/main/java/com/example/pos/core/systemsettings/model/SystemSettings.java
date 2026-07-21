package com.example.pos.core.systemsettings.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "system_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"setting_key", "branch_id", "pharmacy_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SystemSettings extends BaseEntity {

    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(name = "setting_value", length = 4000)
    private String settingValue;

    @Column(length = 1000)
    private String description;
}
