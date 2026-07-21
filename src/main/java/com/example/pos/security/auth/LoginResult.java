package com.example.pos.security.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResult {

    private Long userId;
    private String email;
    private String name;
    private Long branchId;
    private String csrfToken;
    private long expiresIn;
}
