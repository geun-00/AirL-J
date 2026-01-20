package project.airbnb.clone.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.events.chat.ChatLeaveEvent;
import project.airbnb.clone.common.exceptions.factory.ChatExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.entity.chat.ChatParticipant;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.repository.facade.ChatRepositoryFacadeManager;
import project.airbnb.clone.repository.jpa.MemberRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService {

    private final MemberRepository memberRepository;
    private final ChatRedisService chatRedisService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatRepositoryFacadeManager chatRepositoryFacade;

    @Transactional
    public ChatRoomResDto updateChatRoomName(String customName, Long otherMemberId, Long myId, Long roomId) {
        ChatRoom chatRoom = chatRepositoryFacade.getChatRoomByRoomId(roomId);

        int updated = chatRepositoryFacade.updateCustomName(customName, chatRoom, myId);
        if (updated == 0) {
            throw ChatExceptions.notFoundChatParticipant(roomId, myId);
        }

        return getChatRoomInfoWithUnreadCount(myId, otherMemberId, chatRoom);
    }

    protected ChatRoomResDto getChatRoomInfoWithUnreadCount(Long myId, Long otherMemberId, ChatRoom chatRoom) {
        ChatRoomResDto dto = chatRepositoryFacade.getChatRoomInfo(myId, otherMemberId, chatRoom);

        int unreadCount = chatRedisService.getUnreadCount(dto.roomId(), myId);

        return ChatRoomResDto.withUnreadCount(dto, unreadCount);
    }

    /**
     * 채팅방 나가기
     *
     * @param roomId   채팅방
     * @param memberId 사용자
     * @param isActive 활성화 여부
     */
    @Transactional
    public void leaveChatRoom(Long roomId, Long memberId, Boolean isActive) {
        ChatParticipant chatParticipant = getChatParticipant(roomId, memberId);
        chatParticipant.leave();

        chatRepositoryFacade.markLatestMessageAsRead(roomId, chatParticipant);
        eventPublisher.publishEvent(new ChatLeaveEvent(chatParticipant.getMember().getName(), roomId));

        chatRedisService.removeMember(roomId, memberId);
    }

    private ChatParticipant getChatParticipant(Long roomId, Long memberId) {
        return chatRepositoryFacade.findByChatRoomIdAndMemberId(roomId, memberId)
                                   .orElseThrow(() -> ChatExceptions.notFoundChatParticipant(roomId, memberId));
    }

    /**
     * 참여 중 채팅방 목록 조회
     *
     * @param memberId 사용자
     */
    public List<ChatRoomResDto> getChatRooms(Long memberId) {
        List<ChatRoomResDto> rooms = chatRepositoryFacade.findChatRoomsByMemberId(memberId);

        return rooms.stream()
                    .map(room -> {
                        int unreadCount = chatRedisService.getUnreadCount(room.roomId(), memberId);
                        return ChatRoomResDto.withUnreadCount(room, unreadCount);
                    })
                    .toList();
    }

    /**
     * 채팅 메시지 모두 읽음 처리
     *
     * @param roomId   채팅방
     * @param memberId 사용자
     */
    @Transactional
    public void markChatRoomAsRead(Long roomId, Long memberId) {
        chatRedisService.resetUnreadCount(roomId, memberId);

        //TODO : 비동기 or 이벤트 처리
        ChatParticipant chatParticipant = getChatParticipant(roomId, memberId);
        chatRepositoryFacade.markLatestMessageAsRead(roomId, chatParticipant);
    }

    /**
     * 채팅방 참여 여부 검증 - StompHandler
     *
     * @param roomId   채팅방
     * @param memberId 사용자
     */
    public boolean isChatRoomParticipant(Long roomId, Long memberId) {
        try {
            return checkParticipation(roomId, memberId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 메시지 전송 가능 여부 검증 - 비즈니스
     *
     * @param roomId   채팅방
     * @param senderId 사용자
     */
    public void validateMessageDelivery(Long roomId, Long senderId) {
        if (!checkParticipation(roomId, senderId)) {
            throw ChatExceptions.notFoundChatParticipant(roomId, senderId);
        }
        if (chatRedisService.getRoomMembers(roomId).size() < 2) {
            throw ChatExceptions.participantLeft(roomId, senderId);
        }
    }

    private boolean checkParticipation(Long roomId, Long memberId) {
        Boolean isMember = chatRedisService.isMember(roomId, memberId);

        if (Boolean.FALSE.equals(isMember) && !chatRedisService.hasRoomMembersKey(roomId)) {
            refreshChatMembers(roomId);
            isMember = chatRedisService.isMember(roomId, memberId);
        }

        return Boolean.TRUE.equals(isMember);
    }

    private void refreshChatMembers(Long roomId) {
        List<Long> participantIds = chatRepositoryFacade.getParticipantIdsByRoomId(roomId);

        if (!participantIds.isEmpty()) {
            String[] ids = participantIds.stream()
                                         .map(String::valueOf)
                                         .toArray(String[]::new);
            chatRedisService.addMembers(roomId, ids);
        }
    }

    protected ChatRoom getOrCreateChatRoom(Long receiverId, Long senderId) {
        return chatRepositoryFacade.findChatRoomByMembersId(receiverId, senderId)
                                   .map(existingRoom -> {
                                       reactiveIfHasLeft(existingRoom.getId(), receiverId);
                                       reactiveIfHasLeft(existingRoom.getId(), senderId);
                                       return existingRoom;
                                   })
                                   .orElseGet(() -> createNewChatRoom(receiverId, senderId));
    }

    private ChatRoom createNewChatRoom(Long receiverId, Long senderId) {
        Member receiver = getMemberById(receiverId);
        Member sender = getMemberById(senderId);

        ChatRoom chatRoom = chatRepositoryFacade.saveChatRoom(new ChatRoom());

        List<ChatParticipant> newParticipants = List.of(
                ChatParticipant.participant(chatRoom, receiver, sender.getName() + "님과의 대화"),
                ChatParticipant.creator(chatRoom, sender, receiver.getName() + "님과의 대화")
        );

        chatRepositoryFacade.saveChatParticipants(newParticipants);
        return chatRoom;
    }

    private void reactiveIfHasLeft(Long roomId, Long memberId) {
        ChatParticipant chatParticipant = getChatParticipant(roomId, memberId);
        if (chatParticipant.hasLeft()) {
            chatParticipant.rejoin();
        }
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                               .orElseThrow(() -> MemberExceptions.notFoundById(memberId));
    }

    protected Optional<Long> getOpponentId(Long roomId, Long senderId) {
        return Objects.requireNonNull(chatRedisService.getRoomMembers(roomId))
                      .stream()
                      .map(Long::valueOf)
                      .filter(id -> !id.equals(senderId))
                      .findFirst();
    }
}
