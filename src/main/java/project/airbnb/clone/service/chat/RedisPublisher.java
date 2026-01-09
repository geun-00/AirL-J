package project.airbnb.clone.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import project.airbnb.clone.dto.chat.ChatMessageResDto;

@Service
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChatMessageResDto message) {
        redisTemplate.convertAndSend("chatTopic", message);
    }
}
