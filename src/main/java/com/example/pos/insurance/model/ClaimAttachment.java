package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "claim_attachments")
public class ClaimAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private InsuranceClaim claim;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "attachment_type", nullable = false, length = 50)
    private String attachmentType;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 300)
    private String description;
}
