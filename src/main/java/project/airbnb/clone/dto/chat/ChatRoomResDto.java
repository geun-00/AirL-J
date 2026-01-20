package project.airbnb.clone.dto.chat;

import java.time.LocalDateTime;

public record ChatRoomResDto(
        Long roomId,
        String customRoomName,
        Long memberId,
        String memberName,
        String memberProfileImage,
        boolean isOtherMemberActive,
        String lastMessage,
        LocalDateTime lastMessageTime,
        int unreadCount) {

    public static ChatRoomResDto withUnreadCount(ChatRoomResDto dto, int unreadCount) {
        return new ChatRoomResDto(
                dto.roomId(), dto.customRoomName(), dto.memberId(),
                dto.memberName(), dto.memberProfileImage(), dto.isOtherMemberActive(),
                dto.lastMessage(), dto.lastMessageTime(), unreadCount
        );
    }
}
