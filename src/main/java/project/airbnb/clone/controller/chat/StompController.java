package project.airbnb.clone.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import project.airbnb.clone.dto.chat.ChatMessageReqDto;
import project.airbnb.clone.service.chat.ChatMessageService;
import project.airbnb.clone.service.chat.ChatRoomService;

@Controller
@RequiredArgsConstructor
public class StompController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable("roomId") Long roomId, ChatMessageReqDto chatMessageDto) {
        Long senderId = chatMessageDto.senderId();

        chatRoomService.validateMessageDelivery(roomId, senderId);
        chatMessageService.handleMessagePostProcess(roomId, chatMessageDto);
    }
}
