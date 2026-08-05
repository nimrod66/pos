package com.example.pos.user.loginhistory.dto;

import com.example.pos.user.loginhistory.model.LoginHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponseDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String device;
    private String browser;
    private LocalDateTime createdAt;

    public static LoginHistoryResponseDto from(LoginHistory history) {
        return LoginHistoryResponseDto.builder()
                .id(history.getId())
                .userId(history.getUser() != null ? history.getUser().getId() : null)
                .userName(history.getUser() != null
                        ? history.getUser().getFirstName() + " " + history.getUser().getLastName()
                        : null)
                .loginTime(history.getLoginTime())
                .logoutTime(history.getLogoutTime())
                .ipAddress(history.getIpAddress())
                .device(history.getDevice())
                .browser(history.getBrowser())
                .createdAt(history.getCreatedAt())
                .build();
    }
}

