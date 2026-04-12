package com.oyo.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    @NotBlank(message = "Email or phone is required")
    private String emailOrPhone;

    @NotBlank(message = "OTP is required")
    private String otp;
}
