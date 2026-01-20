package project.airbnb.clone.controller.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.airbnb.clone.common.annotations.CurrentMemberId;
import project.airbnb.clone.dto.chat.ChatMessagesResDto;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.LeaveChatRoomReqDto;
import project.airbnb.clone.dto.chat.UpdateChatRoomNameReqDto;
import project.airbnb.clone.service.chat.ChatRoomService;
import project.airbnb.clone.service.chat.ChatMessageService;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping
    public ResponseEntity<List<ChatRoomResDto>> getChatRooms(@CurrentMemberId Long memberId) {
        List<ChatRoomResDto> response = chatRoomService.getChatRooms(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ChatMessagesResDto> getMessageHistories(@RequestParam(value = "lastMessageId", required = false) Long lastMessageId,
                                                                  @RequestParam("size") int pageSize,
                                                                  @PathVariable("roomId") Long roomId) {
        ChatMessagesResDto response = chatMessageService.getMessageHistories(lastMessageId, roomId, pageSize);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{roomId}/name")
    public ResponseEntity<?> updateChatRoomName(@Valid @RequestBody UpdateChatRoomNameReqDto reqDto,
                                                @PathVariable("roomId") Long roomId,
                                                @CurrentMemberId Long memberId) {
        ChatRoomResDto response = chatRoomService.updateChatRoomName(reqDto.customName(), reqDto.otherMemberId(), memberId, roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}")
    public ResponseEntity<?> leaveChatRoom(@PathVariable("roomId") Long roomId,
                                           @RequestBody LeaveChatRoomReqDto reqDto,
                                           @CurrentMemberId Long memberId) {
        chatRoomService.leaveChatRoom(roomId, memberId, reqDto.isActive());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomId}/read")
    public ResponseEntity<?> markChatRoomAsRead(@PathVariable("roomId") Long roomId,
                                                @CurrentMemberId Long memberId) {
        chatRoomService.markChatRoomAsRead(roomId, memberId);
        return ResponseEntity.ok().build();
    }
}
