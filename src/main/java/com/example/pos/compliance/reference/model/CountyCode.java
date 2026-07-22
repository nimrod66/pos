package com.example.pos.compliance.reference.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "kra_county_code")
public class CountyCode extends BaseEntity {

    @Column(name = "county_code", unique = true, nullable = false, length = 10)
    private String countyCode;

    @Column(name = "county_name", nullable = false, length = 100)
    private String countyName;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}