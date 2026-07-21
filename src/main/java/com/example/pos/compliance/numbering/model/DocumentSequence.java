package com.example.pos.compliance.numbering.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "document_sequences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doc_type_branch_date", columnNames = {"document_type", "branch_code", "sequence_date"})
})
public class DocumentSequence extends BaseEntity {

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "sequence_date", length = 8, nullable = false)
    private String sequenceDate;

    @Column(name = "last_sequence", nullable = false)
    @Builder.Default
    private Long lastSequence = 0L;
}
