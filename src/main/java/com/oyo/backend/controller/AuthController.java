package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.dto.auth.*;
import com.oyo.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.sendOtp(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyOtp(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/login-with-phone")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPhone(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.loginWithPhone(body.get("phone"), body.get("otp"))));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(body.get("refreshToken"))));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(body.get("emailOrPhone"))));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.googleLogin(
                        body.get("idToken"),
                        body.get("googleId"),
                        body.get("email"),
                        body.get("name"),
                        body.get("profileImage"))));
    }

    /**
     * Phone login via Firebase Phone Auth.
     * The Android app verifies the SMS OTP with Firebase, then sends the
     * resulting Firebase ID token here. We verify it with Firebase Admin SDK
     * and issue our own JWT.
     */
    @PostMapping("/firebase-phone-login")
    public ResponseEntity<ApiResponse<AuthResponse>> firebasePhoneLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.firebasePhoneLogin(body.get("firebaseIdToken"), body.get("phone"))));
    }
}
