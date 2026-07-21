package com.example.pos.user.loginhistory.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "login_history")

public class LoginHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String device;
    private String browser;
}
