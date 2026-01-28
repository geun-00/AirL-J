package project.airbnb.clone.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub 메시지 발행을 담당하는 공통 서비스
 * 채팅, 알림 등 다양한 토픽에 메시지를 발행할 수 있도록 제네릭하게 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {
    
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 지정된 토픽으로 메시지 발행
     * 
     * @param topic 발행할 토픽 이름
     * @param message 발행할 메시지 객체
     */
    public void publish(String topic, Object message) {
        try {
            redisTemplate.convertAndSend(topic, message);
            log.debug("Redis 메시지 발행 성공 - Topic: {}, Message: {}", topic, message.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Redis 메시지 발행 실패 - Topic: {}, Error: {}", topic, e.getMessage(), e);
            throw new RuntimeException("메시지 발행 중 오류가 발생했습니다.", e);
        }
    }
}
