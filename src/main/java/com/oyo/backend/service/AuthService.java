package com.oyo.backend.service;

import com.oyo.backend.dto.auth.*;
import com.oyo.backend.entity.User;
import com.oyo.backend.enums.UserRole;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.security.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ApiException("Phone already registered");
        }
        String otp = generateOtp();
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .isVerified(false)
                .build();
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);
        return "OTP sent to " + request.getEmail();
    }

    public String sendOtp(OtpRequest request) {
        User user = findUserByEmailOrPhone(request.getEmailOrPhone());
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);
        return "OTP sent successfully";
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        User user = findUserByEmailOrPhone(request.getEmailOrPhone());
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new ApiException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new ApiException("OTP has expired");
        }
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = findUserByEmailOrPhone(request.getEmailOrPhone());
        if (!user.getIsVerified()) {
            throw new ApiException("Account not verified. Please verify your OTP first.");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    public AuthResponse loginWithPhone(String phone, String otp) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApiException("User not found with phone: " + phone));
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new ApiException("Invalid OTP");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new ApiException("OTP has expired");
        }
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
        return buildAuthResponse(user);
    }

    public String forgotPassword(String emailOrPhone) {
        User user = findUserByEmailOrPhone(emailOrPhone);
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), otp);
        return "Password reset OTP sent to " + user.getEmail();
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = findUserByEmailOrPhone(request.getEmailOrPhone());
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new ApiException("Invalid OTP");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new ApiException("OTP has expired");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        return "Password reset successful";
    }

    @Transactional
    public AuthResponse googleLogin(String idToken, String googleId, String email, String name, String profileImage) {
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email).orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .googleId(googleId)
                            .profileImage(profileImage)
                            .role(UserRole.USER)
                            .isVerified(true)
                            .build();
                    return userRepository.save(newUser);
                }));
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }
        return buildAuthResponse(user);
    }

    /**
     * Verifies a Firebase Phone Auth ID token (proves user got the SMS OTP),
     * then finds / auto-creates the user and returns our app JWT.
     */
    @Transactional
    public AuthResponse firebasePhoneLogin(String firebaseIdToken, String phone) {
        try {
            // Verify with Firebase Admin SDK
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            // The phone claim from the token (e.g. "+919876543210")
            String verifiedPhone = (String) decoded.getClaims().get("phone_number");
            String lookupPhone = verifiedPhone != null ? verifiedPhone : phone;

            // Normalise: strip +91 prefix for our DB lookup if needed
            String dbPhone = lookupPhone.startsWith("+91") ? lookupPhone.substring(3) : lookupPhone;

            User user = userRepository.findByPhone(dbPhone)
                    .orElseGet(() -> {
                        // Auto-create user account for new phone-only sign-ups
                        User newUser = User.builder()
                                .name("OYO User")
                                .phone(dbPhone)
                                .email(dbPhone + "@phone.oyo") // placeholder email
                                .role(UserRole.USER)
                                .isVerified(true)
                                .build();
                        return userRepository.save(newUser);
                    });
            user.setIsVerified(true);
            userRepository.save(user);
            return buildAuthResponse(user);
        } catch (Exception e) {
            throw new ApiException("Firebase phone verification failed: " + e.getMessage());
        }
    }

    private User findUserByEmailOrPhone(String emailOrPhone) {
        if (emailOrPhone.contains("@")) {
            return userRepository.findByEmail(emailOrPhone)
                    .orElseThrow(() -> new ApiException("User not found"));
        } else {
            return userRepository.findByPhone(emailOrPhone)
                    .orElseThrow(() -> new ApiException("User not found"));
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole())
                        .isVerified(user.getIsVerified())
                        .build())
                .build();
    }

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
