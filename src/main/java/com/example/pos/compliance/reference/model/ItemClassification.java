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
@Table(name = "kra_item_classification")
public class ItemClassification extends BaseEntity {

    @Column(name = "classification_code", unique = true, nullable = false, length = 30)
    private String classificationCode;

    @Column(name = "classification_name", nullable = false, length = 200)
    private String classificationName;

    @Column(name = "parent_code", length = 30)
    private String parentCode;

    @Column(name = "level")
    private Integer level;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}