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
@Table(name = "kra_notice")
public class KraNotice extends BaseEntity {

    @Column(name = "notice_number", length = 50)
    private String noticeNumber;

    @Column(name = "notice_date", length = 20)
    private String noticeDate;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "acknowledged")
    @Builder.Default
    private Boolean acknowledged = false;
}