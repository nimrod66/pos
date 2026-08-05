package com.example.pos.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {

    private String expiresAt;

    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String email;
        private String displayName;
        private UUID pharmacyId;
        private String pharmacyName;
        private BranchInfo activeBranch;
        private List<String> roles;
        private List<String> permissions;
        private Map<String, Boolean> featureFlags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private UUID id;
        private String code;
        private String name;
    }
}
