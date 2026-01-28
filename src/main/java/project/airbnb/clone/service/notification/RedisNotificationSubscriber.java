package project.airbnb.clone.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import project.airbnb.clone.dto.notification.NotificationResDto;

@Slf4j
@Service
public class RedisNotificationSubscriber {

    private final ObjectMapper redisObjMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public RedisNotificationSubscriber(@Qualifier("redisObjMapper") ObjectMapper redisObjMapper,
                                      SimpMessageSendingOperations messagingTemplate) {
        this.redisObjMapper = redisObjMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Redis에서 발행된 알림을 WebSocket으로 전송
     */
    public void handleNotification(String publishMessage) {
        try {
            NotificationResDto notification = redisObjMapper.readValue(publishMessage, NotificationResDto.class);
            
            log.info("알림 수신 - 사용자: {}, 타입: {}", notification.notificationId(), notification.type());
            
            // WebSocket으로 실시간 전송
            // 사용자 ID는 notification에 포함되어 있지 않으므로 별도로 추출 필요
            // 임시로 /topic/notifications로 전송 (추후 개선)
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            
        } catch (Exception e) {
            log.error("Redis 알림 처리 오류: {}", e.getMessage(), e);
        }
    }
}
