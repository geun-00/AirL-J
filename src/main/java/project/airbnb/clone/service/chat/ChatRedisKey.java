package project.airbnb.clone.service.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRedisKey {
    UNREAD("chat:unread:%d"),
    ROOM_MEMBERS("chat:room:%d:members"),
    MESSAGE_CACHE("chat:cache:%d"),
    MESSAGE_QUEUE("chat:queue"),
    CHAT_REQUEST("chat:chatRequest:%d:%d");

    private final String template;

    public String format(Object... args) {
        return String.format(template, args);
    }
}
