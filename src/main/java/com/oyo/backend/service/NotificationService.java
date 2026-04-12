package com.oyo.backend.service;

import com.oyo.backend.entity.Notification;
import com.oyo.backend.entity.User;
import com.oyo.backend.enums.NotificationType;
import com.oyo.backend.exception.ApiException;
import com.oyo.backend.repository.NotificationRepository;
import com.oyo.backend.repository.UserRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void createNotification(String userId, String title, String message,
            NotificationType type, String referenceId) {
        // Save to DB
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);

        // Send FCM push if user has token
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
                sendFcmPush(user.getFcmToken(), title, message);
            }
        });
    }

    private void sendFcmPush(String fcmToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("title", title)
                    .putData("body", body)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed: {}", e.getMessage());
        }
    }

    public Page<Notification> getUserNotifications(String userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new ApiException("Access denied");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(String userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
