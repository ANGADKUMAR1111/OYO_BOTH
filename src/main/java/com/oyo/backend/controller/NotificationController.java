package com.oyo.backend.controller;

import com.oyo.backend.dto.ApiResponse;
import com.oyo.backend.entity.Notification;
import com.oyo.backend.entity.User;
import com.oyo.backend.repository.UserRepository;
import com.oyo.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.oyo.backend.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private String getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Notification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                notificationService.getUserNotifications(getUserId(auth), page, size))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markRead(@PathVariable String id, Authentication auth) {
        notificationService.markAsRead(id, getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllRead(Authentication auth) {
        notificationService.markAllRead(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("All marked as read"));
    }
}
