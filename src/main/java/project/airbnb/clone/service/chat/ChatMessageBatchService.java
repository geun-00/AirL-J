package project.airbnb.clone.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.dto.chat.ChatMessageResDto;
import project.airbnb.clone.entity.chat.ChatMessage;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.repository.facade.ChatRepositoryFacadeManager;
import project.airbnb.clone.repository.jpa.MemberRepository;

import java.util.List;

@Slf4j
@Component
public class ChatMessageBatchService {

    private final ObjectMapper redisObjMapper;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRepositoryFacadeManager chatRepositoryFacade;

    public ChatMessageBatchService(@Qualifier("redisObjMapper") ObjectMapper redisObjMapper,
                                   MemberRepository memberRepository,
                                   RedisTemplate<String, Object> redisTemplate,
                                   ChatRepositoryFacadeManager chatRepositoryFacade) {
        this.redisObjMapper = redisObjMapper;
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
        this.chatRepositoryFacade = chatRepositoryFacade;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void flushMessagesToDB() {
        String queueKey = "chat:queue";
        String backupKey = "chat:queue:backup";

        if (!redisTemplate.hasKey(queueKey)) return;

        redisTemplate.rename(queueKey, backupKey);

        List<Object> rawMessages = redisTemplate.opsForList().range(backupKey, 0, -1);
        if (rawMessages == null || rawMessages.isEmpty()) return;

        List<ChatMessage> entities = rawMessages.stream()
                                                .map(obj -> redisObjMapper.convertValue(obj, ChatMessageResDto.class))
                                                .map(dto -> {
                                                    ChatRoom room = chatRepositoryFacade.getChatRoomByRoomId(dto.getRoomId());
                                                    Member writer = memberRepository.findById(dto.getSenderId())
                                                                                    .orElseThrow(() -> MemberExceptions.notFoundById(dto.getSenderId()));
                                                    return ChatMessage.create(room, writer, dto.getContent());
                                                })
                                                .toList();
        if (!entities.isEmpty()) {
            chatRepositoryFacade.saveAllChatMessages(entities);
            redisTemplate.delete(backupKey);
        }
    }
}
