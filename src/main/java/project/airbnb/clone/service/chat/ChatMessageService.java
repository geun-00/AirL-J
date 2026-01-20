package project.airbnb.clone.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.dto.chat.ChatMessageReqDto;
import project.airbnb.clone.dto.chat.ChatMessageResDto;
import project.airbnb.clone.dto.chat.ChatMessagesResDto;
import project.airbnb.clone.repository.facade.ChatRepositoryFacadeManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final ChatRedisService chatRedisService;
    private final ChatRepositoryFacadeManager chatRepositoryFacade;

    /**
     * 메시지 기록 조회(커서 기반)
     *
     * @param lastMessageId 마지막 조회 메시지
     * @param roomId        채팅방
     * @param pageSize      조회 개수
     */
    public ChatMessagesResDto getMessageHistories(Long lastMessageId, Long roomId, int pageSize) {
        List<ChatMessageResDto> fetchedMessages = chatRepositoryFacade.getMessages(lastMessageId, roomId, pageSize);
        List<ChatMessageResDto> resultMessages = new ArrayList<>(fetchedMessages);

        if (lastMessageId == null) {
            List<Object> cachedRaw = chatRedisService.getCachedRaw(roomId);

            if (cachedRaw != null && !cachedRaw.isEmpty()) {
                List<ChatMessageResDto> cachedData =
                        cachedRaw.stream()
                                 .map(chatRedisService::convert)
                                 .filter(m -> fetchedMessages.isEmpty() || m.getTimestamp().isAfter(fetchedMessages.get(0).getTimestamp()))
                                 .toList();

                resultMessages.addAll(0, cachedData);
            }
        }

        boolean hasMore = resultMessages.size() > pageSize;

        if (hasMore) {
            resultMessages.remove(resultMessages.size() - 1);
        }

        return new ChatMessagesResDto(resultMessages, hasMore);
    }

    /**
     * 메시지 전송 처리(읽지 않은 메시지 증가/메시지큐 저장/캐시 저장)
     *
     * @param roomId         채팅방
     * @param chatMessageDto 전송자, 메시지 내용
     */
    public void handleMessagePostProcess(Long roomId, ChatMessageReqDto chatMessageDto) {
        Long senderId = chatMessageDto.senderId();

        ChatMessageResDto responseDto = ChatMessageResDto.builder()
                                                         .messageId(UUID.randomUUID().toString())
                                                         .roomId(roomId)
                                                         .senderId(senderId)
                                                         .content(chatMessageDto.content())
                                                         .timestamp(LocalDateTime.now())
                                                         .left(false)
                                                         .build();
        chatRoomService.getOpponentId(roomId, senderId)
                       .ifPresent(opponentId -> chatRedisService.incrementUnreadCount(roomId, senderId));
        chatRedisService.addMessageToQueueAndCache(roomId, responseDto);
        chatRedisService.publish(responseDto);
    }
}
