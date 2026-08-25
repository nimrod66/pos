package com.example.pos.security.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_failed_login")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFailedLogin {

    @Id
    @Column(length = 255)
    private String email;

    private Integer failureCount;

    private java.time.LocalDateTime lastFailure;
}
