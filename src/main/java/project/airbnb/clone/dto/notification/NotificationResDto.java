package project.airbnb.clone.dto.notification;

import lombok.Builder;
import project.airbnb.clone.entity.notification.Notification;
import project.airbnb.clone.entity.notification.NotificationType;

import java.time.LocalDateTime;

@Builder
public record NotificationResDto(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        String referenceId,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResDto from(Notification notification) {
        return NotificationResDto.builder()
                                 .notificationId(notification.getId())
                                 .type(notification.getType())
                                 .title(notification.getTitle())
                                 .content(notification.getContent())
                                 .referenceId(notification.getReferenceId())
                                 .isRead(notification.isRead())
                                 .createdAt(notification.getCreatedAt())
                                 .readAt(notification.getReadAt())
                                 .build();
    }
}
