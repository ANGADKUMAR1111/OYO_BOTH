package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.entity.User;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(Authentication auth) {
        User user = getUser(auth);
        user.setPassword(null); // Don't expose password
        user.setOtp(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        if (body.containsKey("name"))
            user.setName(body.get("name"));
        if (body.containsKey("phone"))
            user.setPhone(body.get("phone"));
        user = userRepository.save(user);
        user.setPassword(null);
        user.setOtp(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/change-password")
    @Transactional
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (user.getPassword() == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ApiException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/upload-profile-image")
    @Transactional
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        user.setProfileImage(body.get("imageUrl"));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile image updated"));
    }

    @PutMapping("/fcm-token")
    @Transactional
    public ResponseEntity<ApiResponse<String>> updateFcmToken(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        user.setFcmToken(body.get("fcmToken"));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("FCM token updated"));
    }

    @DeleteMapping("/account")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteAccount(Authentication auth) {
        User user = getUser(auth);
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }
}
