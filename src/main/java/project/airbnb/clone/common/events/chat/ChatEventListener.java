package project.airbnb.clone.common.events.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import project.airbnb.clone.service.chat.ChatNotifyService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatNotifyService chatNotifyService;

    @EventListener
    public void handleChatRequestCreatedEvent(ChatRequestCreatedEvent event) {
        chatNotifyService.sendChatRequestNotification(event.chatRequest());
    }

    @EventListener
    public void handleChatRequestAcceptedEvent(ChatRequestAcceptedEvent event) {
        chatNotifyService.sendChatRequestAcceptedNotification(event.requestId(), event.senderId(), event.chatRoomResDto());
    }

    @EventListener
    public void handleChatRequestRejectedEvent(ChatRequestRejectedEvent event) {
        chatNotifyService.sendChatRequestRejectedNotification(event.requestId(), event.senderId(), event.receiverName());
    }

    @TransactionalEventListener
    public void handleChatLeaveEvent(ChatLeaveEvent event) {
        chatNotifyService.sendChatLeaveNotification(event.name(), event.roomId());
    }
}
