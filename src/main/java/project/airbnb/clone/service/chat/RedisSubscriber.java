package project.airbnb.clone.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import project.airbnb.clone.dto.chat.ChatMessageResDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public void sendMessage(String publishMessage) {
        try {
            // RedisTemplate의 serializer에 의해 JSON으로 온 데이터를 객체로 변환
            ChatMessageResDto chatMessage = objectMapper.readValue(publishMessage, ChatMessageResDto.class);
            
            // 실시간으로 해당 채팅방을 구독 중인 클라이언트에게 전달
            messagingTemplate.convertAndSend("/topic/" + chatMessage.roomId(), chatMessage);
        } catch (Exception e) {
            log.error("Redis Subscriber error: {}", e.getMessage());
        }
    }
}
