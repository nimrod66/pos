package com.example.pos.user.staffshifts.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "staff_shifts")
public class StaffShifts extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "staffShifts", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<CashDrawers> cashDrawers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private UserRoles userRoles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String shiftName;
    private Integer shiftNumber;

    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ACTIVE, CLOSED, CANCELLED, REVIEWED
    }

    private String remarks;
}
