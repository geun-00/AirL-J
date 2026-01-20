package project.airbnb.clone.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import project.airbnb.clone.dto.chat.ChatMessageResDto;

import java.util.List;
import java.util.Set;

import static project.airbnb.clone.service.chat.ChatRedisKey.*;

@Service
public class ChatRedisService {

    private final ObjectMapper objectMapper;
    private final RedisPublisher redisPublisher;
    private final StringRedisTemplate strRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatRedisService(@Qualifier("redisObjMapper") ObjectMapper objectMapper,
                            RedisPublisher redisPublisher,
                            StringRedisTemplate strRedisTemplate,
                            RedisTemplate<String, Object> redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisPublisher = redisPublisher;
        this.strRedisTemplate = strRedisTemplate;
        this.redisTemplate = redisTemplate;
    }

    protected void incrementUnreadCount(Long roomId, Long memberId) {
        String key = UNREAD.format(roomId);
        strRedisTemplate.opsForHash().increment(key, memberId.toString(), 1);
    }

    protected void addMessageToQueueAndCache(Long roomId, ChatMessageResDto message) {
        redisTemplate.opsForList().rightPush(MESSAGE_QUEUE.getTemplate(), message);

        String cacheKey = MESSAGE_CACHE.format(roomId);
        redisTemplate.opsForList().leftPush(cacheKey, message);
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
    }

    protected List<Object> getCachedRaw(Long roomId) {
        String cacheKey = MESSAGE_CACHE.format(roomId);
        return redisTemplate.opsForList().range(cacheKey, 0, -1);
    }

    protected ChatMessageResDto convert(Object obj) {
        return objectMapper.convertValue(obj, ChatMessageResDto.class);
    }

    protected void publish(ChatMessageResDto responseDto) {
        redisPublisher.publish(responseDto);
    }

    protected void addMembers(Long roomId, String... memberIds) {
        String key = ROOM_MEMBERS.format(roomId);
        strRedisTemplate.opsForSet().add(key, memberIds);
        strRedisTemplate.expire(key, java.time.Duration.ofDays(1));
    }

    protected void removeMember(Long roomId, Long memberId) {
        String key = ROOM_MEMBERS.format(roomId);
        strRedisTemplate.opsForSet().remove(key, memberId.toString());
    }

    protected Boolean isMember(Long roomId, Long memberId) {
        String key = ROOM_MEMBERS.format(roomId);
        return strRedisTemplate.opsForSet().isMember(key, memberId.toString());
    }

    protected boolean hasRoomMembersKey(Long roomId) {
        return strRedisTemplate.hasKey(ROOM_MEMBERS.format(roomId));
    }

    protected Set<String> getRoomMembers(Long roomId) {
        return strRedisTemplate.opsForSet().members(ROOM_MEMBERS.format(roomId));
    }

    protected int getUnreadCount(Long roomId, Long memberId) {
        String key = UNREAD.format(roomId);
        Object count = strRedisTemplate.opsForHash().get(key, memberId.toString());
        return (count != null) ? Integer.parseInt(count.toString()) : 0;
    }

    protected void resetUnreadCount(Long roomId, Long memberId) {
        String key = UNREAD.format(roomId);
        strRedisTemplate.opsForHash().put(key, memberId.toString(), "0");
    }
}
