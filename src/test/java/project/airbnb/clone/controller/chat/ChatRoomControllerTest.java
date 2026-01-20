package project.airbnb.clone.controller.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import project.airbnb.clone.WithMockMember;
import project.airbnb.clone.controller.RestDocsTestSupport;
import project.airbnb.clone.dto.chat.*;
import project.airbnb.clone.service.chat.ChatMessageService;
import project.airbnb.clone.service.chat.ChatRoomService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest extends RestDocsTestSupport {

    private static final String API_TAG = "ChatRoom API";
    private static final String PATH_PREFIX = "/api/chat/rooms";

    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean ChatMessageService chatMessageService;

    @Test
    @DisplayName("참여 중인 전체 채팅방 조회")
    @WithMockMember
    void getChatRooms() throws Exception {
        //given
        List<ChatRoomResDto> response = List.of(
                new ChatRoomResDto(1L, "my-chat-room-1", 1L, "Ahmad Gul", "https://example-a.com", true,
                        "안녕하세요", LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.MICROS), 3),
                new ChatRoomResDto(2L, "my-chat-room-2", 2L, "Aleksey Begam", "https://example-b.com", false,
                        "반갑습니다", LocalDateTime.now().minusDays(6).truncatedTo(ChronoUnit.MICROS), 1),
                new ChatRoomResDto(3L, "my-chat-room-3", 3L, "Doris Sharma", "https://example-c.com", true,
                        "좋은 여행지입니다.", LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.MICROS), 0)
        );

        given(chatRoomService.getChatRooms(anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(get(PATH_PREFIX)
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRoomController.class),
                       handler().methodName("getChatRooms"),
                       status().isOk(),
                       jsonPath("$.length()").value(response.size())

               )
               .andDo(document("get-chat-rooms",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("현재 참여중인 전체 채팅방 조회")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .responseFields(
                                               fieldWithPath("[].roomId")
                                                       .type(NUMBER)
                                                       .description("채팅방 ID"),
                                               fieldWithPath("[].customRoomName")
                                                       .type(STRING)
                                                       .description("채팅방 이름"),
                                               fieldWithPath("[].memberId")
                                                       .type(NUMBER)
                                                       .description("상대방 ID"),
                                               fieldWithPath("[].memberName")
                                                       .type(STRING)
                                                       .description("상대방 이름"),
                                               fieldWithPath("[].memberProfileImage")
                                                       .type(STRING)
                                                       .description("상대방 프로필 이미지 URL"),
                                               fieldWithPath("[].isOtherMemberActive")
                                                       .type(BOOLEAN)
                                                       .description("상대방 채팅방 나감 여부"),
                                               fieldWithPath("[].lastMessage")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("마지막 메시지 내용"),
                                               fieldWithPath("[].lastMessageTime")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("마지막 메시지 전송 시간"),
                                               fieldWithPath("[].unreadCount")
                                                       .type(NUMBER)
                                                       .description("읽지 않은 메시지 개수")
                                       )
                                       .responseSchema(schema("ChatRoomResponse"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("채팅방 메시지 기록 조회")
    @WithMockMember
    void getMessageHistories() throws Exception {
        //given
        List<ChatMessageResDto> messages = List.of(
                new ChatMessageResDto(1L, 1L, 4L, "Maria Lai", "안녕하세요", LocalDateTime.now().minusDays(3)),
                new ChatMessageResDto(2L, 1L, 5L, "Ha Cui", "반갑습니다", LocalDateTime.now().minusDays(2)),
                new ChatMessageResDto(3L, 1L, 6L, "Halima Pham", "수고하세요", LocalDateTime.now().minusDays(1))
        );
        ChatMessagesResDto response = new ChatMessagesResDto(messages, true);

        given(chatMessageService.getMessageHistories(anyLong(), anyLong(), anyInt())).willReturn(response);

        //when
        //then
        mockMvc.perform(get(PATH_PREFIX + "/{roomId}/messages", 1L)
                       .header(AUTHORIZATION, "Bearer {access-token}")
                       .param("lastMessageId", "3")
                       .param("size", "30")
               )
               .andExpectAll(
                       handler().handlerType(ChatRoomController.class),
                       handler().methodName("getMessageHistories"),
                       status().isOk(),
                       jsonPath("$.messages.length()").value(response.messages().size()),
                       jsonPath("$.hasMore").value(response.hasMore())
               )
               .andDo(document("get-chat-messages",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("특정 채팅방 메시지 기록 조회")
                                       .description("특정 채팅방의 메시지 기록을 스크롤 방식으로 조회합니다. 초기 lastMessageId를 전달하지 않으면 마지막 메시지부터 전달되므로 첫 요청 이후 마지막 메시지 ID를 전달하면 됩니다.")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("roomId").description("채팅방 ID"))
                                       .queryParameters(
                                               parameterWithName("lastMessageId")
                                                       .optional()
                                                       .description("마지막 메시지 ID"),
                                               parameterWithName("size")
                                                       .description("한번에 조회할 개수")
                                       )
                                       .responseFields(
                                               fieldWithPath("messages[].messageId")
                                                       .type(STRING)
                                                       .description("메시지 ID"),
                                               fieldWithPath("messages[].roomId")
                                                       .type(NUMBER)
                                                       .description("채팅방 ID"),
                                               fieldWithPath("messages[].senderId")
                                                       .type(NUMBER)
                                                       .description("전송자 ID"),
                                               fieldWithPath("messages[].senderName")
                                                       .type(STRING)
                                                       .description("전송자 이름"),
                                               fieldWithPath("messages[].content")
                                                       .type(STRING)
                                                       .description("메시지 내용"),
                                               fieldWithPath("messages[].timestamp")
                                                       .type(STRING)
                                                       .description("메시지 전송 시간"),
                                               fieldWithPath("messages[].left")
                                                       .type(BOOLEAN)
                                                       .description("채팅방 나감 메시지 여부"),
                                               fieldWithPath("hasMore")
                                                       .type(BOOLEAN)
                                                       .description("더 과거 메시지 기록 존재 여부")
                                       )
                                       .responseSchema(schema("ChatMessagesResponse"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("채팅방 이름 설정")
    @WithMockMember
    void updateChatRoomName() throws Exception {
        //given
        UpdateChatRoomNameReqDto request = new UpdateChatRoomNameReqDto("custom-room-name", 1L);
        ChatRoomResDto response = new ChatRoomResDto(1L, "custom-room-name", 1L, "Ahmad Gul", "https://example.com", true,
                "안녕하세요", LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.MICROS), 3);

        given(chatRoomService.updateChatRoomName(anyString(), anyLong(), anyLong(), anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(patch(PATH_PREFIX + "/{roomId}/name", 1L)
                       .header(AUTHORIZATION, "Bearer {access-token}")
                       .contentType(MediaType.APPLICATION_JSON_VALUE)
                       .content(creatJson(request))
               )
               .andExpectAll(
                       handler().handlerType(ChatRoomController.class),
                       handler().methodName("updateChatRoomName"),
                       status().isOk(),
                       jsonPath("$.roomId").value(response.roomId()),
                       jsonPath("$.customRoomName").value(response.customRoomName()),
                       jsonPath("$.memberId").value(response.memberId()),
                       jsonPath("$.memberName").value(response.memberName()),
                       jsonPath("$.memberProfileImage").value(response.memberProfileImage()),
                       jsonPath("$.isOtherMemberActive").value(response.isOtherMemberActive()),
                       jsonPath("$.lastMessage").value(response.lastMessage()),
                       jsonPath("$.lastMessageTime").exists(),
                       jsonPath("$.unreadCount").value(response.unreadCount())
               )
               .andDo(document("update-room-name",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("채팅방 이름 수정")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("roomId").description("채팅방 ID"))
                                       .requestFields(
                                               fieldWithPath("customName")
                                                       .description("원하는 채팅방 이름")
                                                       .type(STRING),
                                               fieldWithPath("otherMemberId")
                                               .description("상대방 사용자 ID")
                                               .type(NUMBER)
                                       )
                                       .responseFields(
                                               fieldWithPath("roomId")
                                                       .type(NUMBER)
                                                       .description("채팅방 ID"),
                                               fieldWithPath("customRoomName")
                                                       .type(STRING)
                                                       .description("채팅방 이름"),
                                               fieldWithPath("memberId")
                                                       .type(NUMBER)
                                                       .description("상대방 ID"),
                                               fieldWithPath("memberName")
                                                       .type(STRING)
                                                       .description("상대방 이름"),
                                               fieldWithPath("memberProfileImage")
                                                       .type(STRING)
                                                       .description("상대방 프로필 이미지 URL"),
                                               fieldWithPath("isOtherMemberActive")
                                                       .type(BOOLEAN)
                                                       .description("상대방 채팅방 나감 여부"),
                                               fieldWithPath("lastMessage")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("마지막 메시지 내용"),
                                               fieldWithPath("lastMessageTime")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("마지막 메시지 전송 시간"),
                                               fieldWithPath("unreadCount")
                                                       .type(NUMBER)
                                                       .description("읽지 않은 메시지 개수")
                                       )
                                       .requestSchema(schema("UpdateChatRoomNameRequest"))
                                       .responseSchema(schema("ChatRoomResponse"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("채팅방 나가기")
    @WithMockMember
    void leaveChatRoom() throws Exception {
        //given
        LeaveChatRoomReqDto request = new LeaveChatRoomReqDto(true);

        //when
        //then
        mockMvc.perform(post(PATH_PREFIX + "/{roomId}", 1L)
                       .header(AUTHORIZATION, "Bearer {access-token}")
                       .contentType(MediaType.APPLICATION_JSON_VALUE)
                       .content(creatJson(request))
               )
               .andExpectAll(
                       handler().handlerType(ChatRoomController.class),
                       handler().methodName("leaveChatRoom"),
                       status().isOk()
               )
               .andDo(document("leave-chat-room",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("채팅방 나가기")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("roomId").description("채팅방 ID"))
                                       .requestFields(fieldWithPath("isActive").description("채팅방이 화면에서 활성화되어 있는지 여부").type(BOOLEAN))
                                       .requestSchema(schema("LeaveChatRoomRequest"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("채팅방 메시지 모두 읽음 처리")
    @WithMockMember
    void markChatRoomAsRead() throws Exception {
        //given
        Long roomId = 1L;

        //when
        //then
        mockMvc.perform(put(PATH_PREFIX + "/{roomId}/read", roomId)
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRoomController.class),
                       handler().methodName("markChatRoomAsRead"),
                       status().isOk()
               )
               .andDo(document("mark-chat-room-as-read",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("채팅방 메시지 읽음 처리")
                                       .description("특정 채팅방의 읽지 않은 메시지 카운트를 0으로 초기화하고, 마지막 읽은 메시지 지점을 갱신합니다.")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("roomId").description("채팅방 ID"))
                                       .build()
                       )
               ));
    }
}