package project.airbnb.clone.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.events.chat.ChatRequestAcceptedEvent;
import project.airbnb.clone.common.events.chat.ChatRequestCreatedEvent;
import project.airbnb.clone.common.events.chat.ChatRequestRejectedEvent;
import project.airbnb.clone.common.exceptions.factory.ChatExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.entity.chat.ChatParticipant;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.repository.dto.redis.ChatRequest;
import project.airbnb.clone.repository.facade.ChatRepositoryFacadeManager;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.redis.ChatRequestRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static project.airbnb.clone.service.chat.ChatRedisKey.CHAT_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRequestService {

    private final ChatRoomService chatRoomService;
    private final ChatRedisService chatRedisService;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatRepositoryFacadeManager chatRepositoryFacade;

    /**
     * 채팅 요청 보내기
     *
     * @param receiverId 수신자
     * @param senderId   요청자
     */
    public RequestChatResDto requestChat(Long receiverId, Long senderId) {
        String requestKey = CHAT_REQUEST.format(senderId, receiverId);

        if (receiverId.equals(senderId)) throw ChatExceptions.sameParticipant(receiverId);
        if (chatRequestRepository.existsById(requestKey)) throw ChatExceptions.alreadyRequest(requestKey);

        chatRepositoryFacade.findChatRoomByMembersId(receiverId, senderId)
                            .map(chatRoom -> getChatParticipant(chatRoom.getId(), senderId))
                            .ifPresent(participant -> {
                                if (participant.isActiveParticipant()) {
                                    throw ChatExceptions.alreadyActiveChat();
                                }
                            });

        LocalDateTime now = LocalDateTime.now();
        Duration requestTTL = Duration.ofDays(1);

        Member sender = getMemberById(senderId);
        Member receiver = getMemberById(receiverId);

        ChatRequest chatRequest = ChatRequest.builder()
                                             .requestId(requestKey)
                                             .senderId(senderId)
                                             .senderName(sender.getName())
                                             .senderProfileImage(sender.getProfileUrl())
                                             .receiverId(receiverId)
                                             .receiverName(receiver.getName())
                                             .receiverProfileImage(receiver.getProfileUrl())
                                             .createdAt(now)
                                             .expiresAt(now.plus(requestTTL))
                                             .build();
        chatRequestRepository.save(chatRequest);

        eventPublisher.publishEvent(new ChatRequestCreatedEvent(chatRequest));

        return chatRequest.toResDto();
    }

    private ChatParticipant getChatParticipant(Long roomId, Long memberId) {
        return chatRepositoryFacade.findByChatRoomIdAndMemberId(roomId, memberId)
                                   .orElseThrow(() -> ChatExceptions.notFoundChatParticipant(roomId, memberId));
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                               .orElseThrow(() -> MemberExceptions.notFoundById(memberId));
    }

    /**
     * 채팅 요청 수락
     *
     * @param requestId  채팅 요청 식별값
     * @param receiverId 수신자
     */
    @Transactional
    public ChatRoomResDto acceptRequestChat(String requestId, Long receiverId) {
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
                                                       .orElseThrow(() -> ChatExceptions.notFoundChatRequest(requestId));

        if (!chatRequest.getReceiverId().equals(receiverId)) {
            throw ChatExceptions.notOwnerOfChatRequest(requestId, receiverId);
        }

        chatRequestRepository.delete(chatRequest);

        Long senderId = chatRequest.getSenderId();
        ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(receiverId, senderId);

        ChatRoomResDto senderChatRoomInfo = chatRoomService.getChatRoomInfoWithUnreadCount(senderId, receiverId, chatRoom);
        eventPublisher.publishEvent(new ChatRequestAcceptedEvent(requestId, senderId, senderChatRoomInfo));

        chatRedisService.addMembers(chatRoom.getId(), senderId.toString(), receiverId.toString());

        return chatRoomService.getChatRoomInfoWithUnreadCount(receiverId, senderId, chatRoom);
    }

    /**
     * 채팅 요청 거절
     *
     * @param requestId  채팅 요청 식별값
     * @param rejecterId 수신자
     */
    public void rejectRequestChat(String requestId, Long rejecterId) {
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
                                                       .orElseThrow(() -> ChatExceptions.notFoundChatRequest(requestId));

        if (!chatRequest.getReceiverId().equals(rejecterId)) {
            throw ChatExceptions.notOwnerOfChatRequest(requestId, rejecterId);
        }

        chatRequestRepository.delete(chatRequest);
        eventPublisher.publishEvent(new ChatRequestRejectedEvent(requestId, chatRequest.getSenderId()));
    }

    /**
     * 보낸 채팅 요청 목록 조회
     */
    public List<RequestChatResDto> getReceivedChatRequests(Long memberId) {
        return chatRequestRepository.findByReceiverId(memberId)
                                    .stream()
                                    .map(ChatRequest::toResDto)
                                    .toList();
    }

    /**
     * 받은 채팅 요청 목록 조회
     */
    public List<RequestChatResDto> getSentChatRequests(Long memberId) {
        return chatRequestRepository.findBySenderId(memberId)
                                    .stream()
                                    .map(ChatRequest::toResDto)
                                    .toList();
    }
}
