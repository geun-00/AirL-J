package project.airbnb.clone.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import project.airbnb.clone.dto.chat.ChatMessageReqDto;
import project.airbnb.clone.dto.chat.ChatMessageResDto;
import project.airbnb.clone.service.chat.ChatService;
import project.airbnb.clone.service.chat.RedisPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StompController {

    private final ChatService chatService;
    private final RedisPublisher redisPublisher;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable("roomId") Long roomId, ChatMessageReqDto chatMessageDto) {
        Long senderId = chatMessageDto.senderId();

        chatService.validateParticipant(roomId, senderId);

        ChatMessageResDto responseDto = ChatMessageResDto.builder()
                                                         .messageId(UUID.randomUUID().toString())
                                                         .roomId(roomId)
                                                         .senderId(senderId)
                                                         .content(chatMessageDto.content())
                                                         .timestamp(LocalDateTime.now())
                                                         .left(false)
                                                         .build();
        // Redis Pub/Sub으로 메시지 발행
        redisPublisher.publish(responseDto);

        chatService.handleMessagePostProcess(responseDto);
    }
}
