package com.oyo.backend.dto.auth;

import com.oyo.backend.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String profileImage;
        private UserRole role;
        private Boolean isVerified;
    }
}
