package project.airbnb.clone.controller.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.airbnb.clone.common.annotations.CurrentMemberId;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatReqDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.service.chat.ChatRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/chat/requests")
@RequiredArgsConstructor
public class ChatRequestController {

    private final ChatRequestService chatRequestService;

    @PostMapping
    public ResponseEntity<RequestChatResDto> requestChat(@Valid @RequestBody RequestChatReqDto requestChatReqDto,
                                                         @CurrentMemberId Long senderId) {
        RequestChatResDto response = chatRequestService.requestChat(requestChatReqDto.receiverId(), senderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<ChatRoomResDto> acceptRequestChat(@PathVariable("requestId") String requestId,
                                                            @CurrentMemberId Long memberId) {
        ChatRoomResDto response = chatRequestService.acceptRequestChat(requestId, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequestChat(@PathVariable("requestId") String requestId,
                                               @CurrentMemberId Long memberId) {
        chatRequestService.rejectRequestChat(requestId, memberId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/received")
    public ResponseEntity<List<RequestChatResDto>> getReceivedChatRequests(@CurrentMemberId Long memberId) {
        List<RequestChatResDto> response = chatRequestService.getReceivedChatRequests(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<RequestChatResDto>> getSentChatRequests(@CurrentMemberId Long memberId) {
        List<RequestChatResDto> response = chatRequestService.getSentChatRequests(memberId);
        return ResponseEntity.ok(response);
    }
}
