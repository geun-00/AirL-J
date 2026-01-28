package project.airbnb.clone.entity.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 채팅
    CHAT_REQUEST("채팅 요청"),
    CHAT_REQUEST_ACCEPTED("채팅 수락"),
    CHAT_REQUEST_REJECTED("채팅 거절"),
    CHAT_MESSAGE("새 메시지"),
    
    // 예약
    RESERVATION_CONFIRMED("예약 확정"),
    RESERVATION_CANCELLED("예약 취소"),
    RESERVATION_REMINDER("예약 알림");

    private final String description;
}
