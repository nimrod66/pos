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
@Table(name = "kra_code_list")
public class KraCodeList extends BaseEntity {

    @Column(name = "code_type", nullable = false, length = 50)
    private String codeType;

    @Column(name = "code_value", nullable = false, length = 50)
    private String codeValue;

    @Column(name = "code_name", nullable = false, length = 200)
    private String codeName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "kra_code")
    private String kraCode;
}