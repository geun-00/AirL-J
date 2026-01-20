package project.airbnb.clone.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.events.chat.ChatLeaveEvent;
import project.airbnb.clone.common.events.chat.ChatRequestAcceptedEvent;
import project.airbnb.clone.common.events.chat.ChatRequestCreatedEvent;
import project.airbnb.clone.common.events.chat.ChatRequestRejectedEvent;
import project.airbnb.clone.common.exceptions.factory.ChatExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.dto.chat.*;
import project.airbnb.clone.entity.chat.ChatParticipant;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.repository.dto.redis.ChatRequest;
import project.airbnb.clone.repository.facade.ChatRepositoryFacadeManager;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.redis.ChatRequestRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ChatService {

    private final ObjectMapper redisObjMapper;
    private final StringRedisTemplate strRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatRepositoryFacadeManager chatRepositoryFacade;

    public ChatService(@Qualifier("redisObjMapper") ObjectMapper redisObjMapper,
                       StringRedisTemplate strRedisTemplate,
                       RedisTemplate<String, Object> redisTemplate,
                       MemberRepository memberRepository,
                       ApplicationEventPublisher eventPublisher,
                       ChatRequestRepository chatRequestRepository,
                       ChatRepositoryFacadeManager chatRepositoryFacade) {
        this.redisObjMapper = redisObjMapper;
        this.strRedisTemplate = strRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
        this.chatRequestRepository = chatRequestRepository;
        this.chatRepositoryFacade = chatRepositoryFacade;
    }

    @Transactional
    public ChatMessageResDto saveChatMessage(Long roomId, ChatMessageReqDto chatMessageDto) {
        String memberSetKey = "chat:room:" + roomId + ":members";

        Long senderId = chatMessageDto.senderId();
        if (Boolean.FALSE.equals(strRedisTemplate.opsForSet().isMember(memberSetKey, senderId.toString()))) {
            throw ChatExceptions.notFoundChatParticipant(roomId, senderId);
        }

        return ChatMessageResDto.builder()
                                .messageId(UUID.randomUUID().toString())
                                .roomId(roomId)
                                .senderId(senderId)
                                .content(chatMessageDto.content())
                                .timestamp(LocalDateTime.now())
                                .left(false)
                                .build();
    }

    @Transactional
    public ChatMessagesResDto getMessageHistories(Long lastMessageId, Long roomId, int pageSize, Long memberId) {
        List<ChatMessageResDto> fetchedMessages = chatRepositoryFacade.getMessages(lastMessageId, roomId, pageSize);
        List<ChatMessageResDto> resultMessages = new ArrayList<>(fetchedMessages);

        if (lastMessageId == null) {
            String cacheKey = "chat:cache:" + roomId;
            List<Object> cachedRaw = redisTemplate.opsForList().range(cacheKey, 0, -1);

            if (cachedRaw != null && !cachedRaw.isEmpty()) {
                List<ChatMessageResDto> cachedData =
                        cachedRaw.stream()
                                 .map(obj -> redisObjMapper.convertValue(obj, ChatMessageResDto.class))
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

    @Transactional
    public ChatRoomResDto updateChatRoomName(String customName, Long otherMemberId, Long myId, Long roomId) {
        ChatRoom chatRoom = chatRepositoryFacade.getChatRoomByRoomId(roomId);

        int updated = chatRepositoryFacade.updateCustomName(customName, chatRoom, myId);
        if (updated == 0) {
            throw ChatExceptions.notFoundChatParticipant(roomId, myId);
        }

        return chatRepositoryFacade.getChatRoomInfo(myId, otherMemberId, chatRoom);
    }

    @Transactional
    public void leaveChatRoom(Long roomId, Long memberId, Boolean active) {
        ChatParticipant chatParticipant = getChatParticipant(roomId, memberId);
        chatParticipant.leave();

        chatRepositoryFacade.markLatestMessageAsRead(roomId, chatParticipant);
        eventPublisher.publishEvent(new ChatLeaveEvent(chatParticipant.getMember().getName(), roomId));

        String memberSetKey = "chat:room:" + roomId + ":members";
        strRedisTemplate.opsForSet().remove(memberSetKey, memberId.toString());
    }

    @Transactional
    public ChatRoomResDto acceptRequestChat(String requestId, Long receiverId) {
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
                                                       .orElseThrow(() -> ChatExceptions.notFoundChatRequest(requestId));

        if (!chatRequest.getReceiverId().equals(receiverId)) {
            throw ChatExceptions.notOwnerOfChatRequest(requestId, receiverId);
        }

        chatRequestRepository.delete(chatRequest);

        Long senderId = chatRequest.getSenderId();
        ChatRoom chatRoom = chatRepositoryFacade.findChatRoomByMembersId(receiverId, senderId)
                                                .map(existingRoom -> {
                                                    reactiveIfHasLeft(existingRoom.getId(), receiverId);
                                                    reactiveIfHasLeft(existingRoom.getId(), senderId);
                                                    return existingRoom;
                                                })
                                                .orElseGet(() -> createNewChatRoom(receiverId, senderId));

        ChatRoomResDto senderChatRoomInfo = chatRepositoryFacade.getChatRoomInfo(senderId, receiverId, chatRoom);
        eventPublisher.publishEvent(new ChatRequestAcceptedEvent(requestId, senderId, senderChatRoomInfo));

        String memberSetKey = "chat:room:" + chatRoom.getId() + ":members";
        strRedisTemplate.opsForSet().add(memberSetKey, senderId.toString(), receiverId.toString());

        return chatRepositoryFacade.getChatRoomInfo(receiverId, senderId, chatRoom);
    }

    public void rejectRequestChat(String requestId, Long rejecterId) {
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
                                                       .orElseThrow(() -> ChatExceptions.notFoundChatRequest(requestId));

        if (!chatRequest.getReceiverId().equals(rejecterId)) {
            throw ChatExceptions.notOwnerOfChatRequest(requestId, rejecterId);
        }

        chatRequestRepository.delete(chatRequest);
        eventPublisher.publishEvent(new ChatRequestRejectedEvent(requestId, chatRequest.getSenderId()));
    }

    public RequestChatResDto requestChat(Long receiverId, Long senderId) {
        String requestKey = "chat:chatRequest:" + senderId + ":" + receiverId;

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

        Member sender = memberRepository.findById(senderId).orElseThrow(() -> MemberExceptions.notFoundById(senderId));
        Member receiver = memberRepository.findById(receiverId)
                                          .orElseThrow(() -> MemberExceptions.notFoundById(receiverId));

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

    public List<ChatRoomResDto> getChatRooms(Long memberId) {
        List<ChatRoomResDto> rooms = chatRepositoryFacade.findChatRoomsByMemberId(memberId);
        return rooms.stream()
                    .map(room -> {
                        String unreadKey = "chat:unread:" + room.roomId();
                        Object countObj = strRedisTemplate.opsForHash().get(unreadKey, memberId.toString());
                        int unreadCount = (countObj != null) ? Integer.parseInt(countObj.toString()) : 0;

                        return new ChatRoomResDto(
                                room.roomId(),
                                room.customRoomName(),
                                room.memberId(),
                                room.memberName(),
                                room.memberProfileImage(),
                                room.isOtherMemberActive(),
                                room.lastMessage(),
                                room.lastMessageTime(),
                                unreadCount
                        );
                    })
                    .toList();
    }

    public boolean isChatRoomParticipant(Long roomId, Long memberId) {
        return chatRepositoryFacade.findByChatRoomIdAndMemberId(roomId, memberId)
                                   .map(ChatParticipant::isActiveParticipant)
                                   .orElse(false);
    }

    public List<RequestChatResDto> getReceivedChatRequests(Long memberId) {
        return chatRequestRepository.findByReceiverId(memberId)
                                    .stream()
                                    .map(ChatRequest::toResDto)
                                    .toList();
    }

    public List<RequestChatResDto> getSentChatRequests(Long memberId) {
        return chatRequestRepository.findBySenderId(memberId)
                                    .stream()
                                    .map(ChatRequest::toResDto)
                                    .toList();
    }

    @Transactional
    public void markChatRoomAsRead(Long roomId, Long memberId) {
        strRedisTemplate.opsForHash().put("chat:unread:" + roomId, memberId.toString(), "0");

        //TODO : 비동기 or 이벤트 처리
        ChatParticipant chatParticipant = getChatParticipant(roomId, memberId);
        chatRepositoryFacade.markLatestMessageAsRead(roomId, chatParticipant);
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

    private ChatParticipant getChatParticipant(Long roomId, Long memberId) {
        return chatRepositoryFacade.findByChatRoomIdAndMemberId(roomId, memberId)
                                   .orElseThrow(() -> ChatExceptions.notFoundChatParticipant(roomId, memberId));
    }

    public void validateParticipant(Long roomId, Long senderId) {
        String key = "chat:room:" + roomId + ":members";

        if (Boolean.FALSE.equals(strRedisTemplate.opsForSet().isMember(key, senderId.toString()))) {

            if (!strRedisTemplate.hasKey(key)) {
                refreshChatMembers(roomId, key);

                if (Boolean.TRUE.equals(strRedisTemplate.opsForSet().isMember(key, senderId.toString()))) {
                    return;
                }
            }

            throw ChatExceptions.notFoundChatParticipant(roomId, senderId);
        }
    }

    private void refreshChatMembers(Long roomId, String key) {
        List<Long> participantIds = chatRepositoryFacade.getParticipantIdsByRoomId(roomId);

        if (!participantIds.isEmpty()) {
            String[] ids = participantIds.stream()
                                         .map(String::valueOf)
                                         .toArray(String[]::new);
            strRedisTemplate.opsForSet().add(key, ids);
        }
    }

    public void handleMessagePostProcess(ChatMessageResDto responseDto) {
        Long roomId = responseDto.getRoomId();
        Long senderId = responseDto.getSenderId();

        String unreadKey = "chat:unread:" + roomId;
        getOpponentId(roomId, senderId)
                .ifPresent(opponentId -> strRedisTemplate.opsForHash().increment(unreadKey, opponentId.toString(), 1));

        String queueKey = "chat:queue";
        redisTemplate.opsForList().rightPush(queueKey, responseDto);

        String cacheKey = "chat:cache:" + roomId;
        redisTemplate.opsForList().leftPush(cacheKey, responseDto);
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
    }

    private Optional<Long> getOpponentId(Long roomId, Long senderId) {
        String key = "chat:room:" + roomId + ":members";

        return Objects.requireNonNull(strRedisTemplate.opsForSet().members(key))
                      .stream()
                      .map(Long::valueOf)
                      .filter(id -> !id.equals(senderId))
                      .findFirst();
    }
}
