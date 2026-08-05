package com.example.pos.security.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResult {

    private UUID userId;
    private String email;
    private String name;
    private UUID branchId;
    private String csrfToken;
    private long expiresIn;
}
