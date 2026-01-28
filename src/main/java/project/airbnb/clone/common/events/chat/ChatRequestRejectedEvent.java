package project.airbnb.clone.common.events.chat;

public record ChatRequestRejectedEvent(String requestId, Long senderId, String receiverName) {
}
