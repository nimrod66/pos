package com.example.pos.core.pharmacy.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.systemsettings.model.SystemSettings;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pharmacy")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pharmacy extends BaseEntity {

    private String name;
    private String address;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private String kraPin;

    @Builder.Default
    @OneToMany(mappedBy = "pharmacy", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Branch> branches = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "pharmacy", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SystemSettings> systemSettings = new HashSet<>();


}
