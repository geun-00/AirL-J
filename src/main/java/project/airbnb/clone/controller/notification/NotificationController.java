package project.airbnb.clone.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.airbnb.clone.common.annotations.CurrentMemberId;
import project.airbnb.clone.dto.notification.NotificationResDto;
import project.airbnb.clone.dto.notification.UnreadCountResDto;
import project.airbnb.clone.entity.notification.NotificationType;
import project.airbnb.clone.service.notification.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     *
     * @param isRead null: 전체, true: 읽은 알림, false: 읽지 않은 알림
     * @param type null: 전체 타입, 지정 시 해당 타입만 조회
     */
    @GetMapping
    public ResponseEntity<List<NotificationResDto>> getNotifications(@CurrentMemberId Long memberId,
                                                                     @RequestParam(required = false) Boolean isRead,
                                                                     @RequestParam(required = false) NotificationType type) {
        List<NotificationResDto> notifications = notificationService.getNotifications(memberId, isRead, type);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 미확인 알림 개수 조회
     */
    @GetMapping("/unread/count")
    public ResponseEntity<UnreadCountResDto> getUnreadCount(@CurrentMemberId Long memberId) {
        UnreadCountResDto count = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId,
                                           @CurrentMemberId Long memberId) {
        notificationService.markAsRead(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentMemberId Long memberId) {
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId,
                                                   @CurrentMemberId Long memberId) {
        notificationService.deleteNotification(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 알림 삭제
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(@CurrentMemberId Long memberId) {
        notificationService.deleteAllNotifications(memberId);
        return ResponseEntity.ok().build();
    }
}
