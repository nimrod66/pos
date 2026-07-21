package com.example.pos.audit.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    //will be fixed accordingly to fit the auditing of the system
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String tableName;
    private String recordId;
    private String action;
}
