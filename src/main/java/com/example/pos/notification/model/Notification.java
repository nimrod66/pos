package com.example.pos.notification.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notification")
public class Notification extends BaseEntity {

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Status status;

    private UUID referenceId;
    private String referenceType;
    private UUID userId;
    private UUID branchId;

    public enum Type {
        LOW_STOCK, EXPIRY_WARNING, SALE_COMPLETED, SHIFT_REMINDER, SYSTEM_ALERT
    }

    public enum Status {
        UNREAD, READ, DISMISSED
    }
}
