package project.airbnb.clone.repository.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.airbnb.clone.common.exceptions.factory.ChatExceptions;
import project.airbnb.clone.dto.chat.ChatMessageResDto;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.entity.chat.ChatMessage;
import project.airbnb.clone.entity.chat.ChatParticipant;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.repository.jpa.ChatMessageRepository;
import project.airbnb.clone.repository.jpa.ChatParticipantRepository;
import project.airbnb.clone.repository.jpa.ChatRoomRepository;
import project.airbnb.clone.repository.query.ChatMessageQueryRepository;
import project.airbnb.clone.repository.query.ChatRoomQueryRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatRepositoryFacadeManager {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;

    public Optional<ChatRoom> findChatRoomByMembersId(Long currentMemberId, Long otherMemberId) {
        return chatRoomRepository.findByMembersId(currentMemberId, otherMemberId);
    }

    public ChatRoom getChatRoomByRoomId(Long roomId) {
        return chatRoomRepository.findById(roomId)
                                 .orElseThrow(() -> ChatExceptions.notFoundChatRoom(roomId));
    }

    public List<ChatParticipant> findParticipantsByChatRoom(ChatRoom chatRoom) {
        return chatParticipantRepository.findByChatRoom(chatRoom);
    }

    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessageResDto> getMessages(Long lastMessageId, Long roomId, int pageSize) {
        return chatMessageQueryRepository.getMessages(lastMessageId, roomId, pageSize);
    }

    public ChatMessage getChatMessageById(Long id) {
        return chatMessageRepository.findById(id)
                                    .orElseThrow(() -> ChatExceptions.notFoundChatMessage(id));
    }

    public int updateCustomName(String customName, ChatRoom chatRoom, Long id) {
        return chatParticipantRepository.updateCustomName(customName, chatRoom, id);
    }

    public void markLatestMessageAsRead(Long roomId, ChatParticipant chatParticipant) {
        chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(roomId)
                             .ifPresent(chatParticipant::updateLastReadMessage);
    }

    public List<ChatRoomResDto> findChatRoomsByMemberId(Long memberId) {
        return chatRoomQueryRepository.findChatRooms(memberId);
    }

    public Optional<ChatParticipant> findByChatRoomIdAndMemberId(Long roomId, Long memberId) {
        return chatParticipantRepository.findByChatRoomIdAndMemberId(roomId, memberId);
    }

    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    public void saveChatParticipants(List<ChatParticipant> participants) {
        chatParticipantRepository.saveAll(participants);
    }

    public ChatRoomResDto getChatRoomInfo(Long currentMemberId, Long otherMemberId, ChatRoom chatRoom) {
        return chatRoomQueryRepository.findChatRoomInfo(currentMemberId, otherMemberId, chatRoom)
                                      .orElseThrow(() -> ChatExceptions.notFoundChatRoom(currentMemberId, otherMemberId));
    }

    public void saveAllChatMessages(List<ChatMessage> messages) {
        chatMessageRepository.saveAll(messages);
    }

    public List<Long> getParticipantIdsByRoomId(Long roomId) {
        return chatParticipantRepository.getParticipantIdsByRoomId(roomId);
    }
}
