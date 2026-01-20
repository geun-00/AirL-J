package project.airbnb.clone.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import project.airbnb.clone.dto.chat.ChatMessageResDto;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.StompChatRequestNotification;
import project.airbnb.clone.dto.chat.StompChatRequestResponseNotification;
import project.airbnb.clone.repository.dto.redis.ChatRequest;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotifyService {

    private final SimpMessageSendingOperations messageTemplate;

    public void sendChatRequestNotification(ChatRequest chatRequest) {
        StompChatRequestNotification notification = StompChatRequestNotification.builder()
                                                                                .requestId(chatRequest.getRequestId())
                                                                                .senderId(chatRequest.getSenderId())
                                                                                .senderName(chatRequest.getSenderName())
                                                                                .senderProfileImage(chatRequest.getSenderProfileImage())
                                                                                .expiresAt(chatRequest.getExpiresAt())
                                                                                .build();
        messageTemplate.convertAndSendToUser(
                String.valueOf(chatRequest.getReceiverId()),
                "/queue/chat-requests",
                notification);
    }

    public void sendChatRequestAcceptedNotification(String requestId, Long senderId, ChatRoomResDto chatRoomResDto) {
        StompChatRequestResponseNotification notification = StompChatRequestResponseNotification.builder()
                                                                                                .requestId(requestId)
                                                                                                .accepted(true)
                                                                                                .message("채팅 요청이 수락되었습니다.")
                                                                                                .roomId(chatRoomResDto.roomId())
                                                                                                .chatRoom(chatRoomResDto)
                                                                                                .build();
        messageTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/chat-request-responses",
                notification
        );
    }

    public void sendChatRequestRejectedNotification(String requestId, Long senderId) {
        StompChatRequestResponseNotification notification = StompChatRequestResponseNotification.builder()
                                                                                                .requestId(requestId)
                                                                                                .accepted(false)
                                                                                                .message("채팅 요청이 거절되었습니다.")
                                                                                                .roomId(null)
                                                                                                .chatRoom(null)
                                                                                                .build();
        messageTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/chat-request-responses",
                notification
        );
    }

    public void sendChatLeaveNotification(String name, Long roomId) {
        ChatMessageResDto leaveMessage = ChatMessageResDto.builder()
                                                          .messageId(null)
                                                          .roomId(roomId)
                                                          .senderId(null)
                                                          .senderName(null)
                                                          .content(name + "님이 대화를 떠났습니다.")
                                                          .timestamp(LocalDateTime.now())
                                                          .left(true)
                                                          .build();

        messageTemplate.convertAndSend("/topic/" + roomId, leaveMessage);
    }
}
