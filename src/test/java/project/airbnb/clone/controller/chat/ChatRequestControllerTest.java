package project.airbnb.clone.controller.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import project.airbnb.clone.WithMockMember;
import project.airbnb.clone.controller.RestDocsTestSupport;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatReqDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.service.chat.ChatRequestService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRequestController.class)
class ChatRequestControllerTest extends RestDocsTestSupport {

    private static final String API_TAG = "Chat Request API";

    @MockitoBean ChatRequestService chatRequestService;

    @Test
    @DisplayName("대화 요청")
    @WithMockMember
    void requestChat() throws Exception {
        //given
        RequestChatReqDto request = new RequestChatReqDto(1L);
        RequestChatResDto response = new RequestChatResDto("request-id", 2L, "Walter Umar", "https://sender-profile.com",
                1L, "Amina Morales", "https://receiver-profile.com", LocalDateTime.now().plusDays(1));
        given(chatRequestService.requestChat(anyLong(), anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(post("/api/chat/requests")
                       .header(AUTHORIZATION, "Bearer {access-token}")
                       .contentType(MediaType.APPLICATION_JSON_VALUE)
                       .content(creatJson(request))
               )
               .andExpectAll(
                       handler().handlerType(ChatRequestController.class),
                       handler().methodName("requestChat"),
                       status().isOk(),
                       jsonPath("$.requestId").value(response.requestId()),
                       jsonPath("$.senderId").value(response.senderId()),
                       jsonPath("$.senderName").value(response.senderName()),
                       jsonPath("$.senderProfileImage").value(response.senderProfileImage()),
                       jsonPath("$.receiverId").value(response.receiverId()),
                       jsonPath("$.receiverName").value(response.receiverName()),
                       jsonPath("$.receiverProfileImage").value(response.receiverProfileImage()),
                       jsonPath("$.expiresAt").exists()
               )
               .andDo(document("request-chat",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("대화 요청")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .requestFields(fieldWithPath("receiverId")
                                               .description("대화를 원하는 상대방 사용자 ID")
                                               .type(NUMBER)
                                       )
                                       .responseFields(
                                               fieldWithPath("requestId")
                                                       .type(STRING)
                                                       .description("대화 요청 ID"),
                                               fieldWithPath("senderId")
                                                       .type(NUMBER)
                                                       .description("요청 보낸 사용자 ID"),
                                               fieldWithPath("senderName")
                                                       .type(STRING)
                                                       .description("요청 보낸 사용자 이름"),
                                               fieldWithPath("senderProfileImage")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("요청 보낸 사용자 프로필 이미지"),
                                               fieldWithPath("receiverId")
                                                       .type(NUMBER)
                                                       .description("요청 받은 사용자 ID"),
                                               fieldWithPath("receiverName")
                                                       .type(STRING)
                                                       .description("요청 받은 사용자 이름"),
                                               fieldWithPath("receiverProfileImage")
                                                       .optional()
                                                       .type(STRING)
                                                       .description("요청 받은 사용자 프로필 이미지"),
                                               fieldWithPath("expiresAt")
                                                       .type(STRING)
                                                       .description("요청 만료시간(24시간)")
                                       )
                                       .requestSchema(schema("RequestChatRequest"))
                                       .responseSchema(schema("RequestChatResponse"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("대화 요청 수락")
    @WithMockMember
    void acceptRequestChat() throws Exception {
        //given
        ChatRoomResDto response = new ChatRoomResDto(1L, "custom-room-name", 1L, "Ahmad Gul", "https://example.com", true,
                "안녕하세요", LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.MICROS), 3);

        given(chatRequestService.acceptRequestChat(anyString(), anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(post("/api/chat/requests/{requestId}/accept", "chat:request:1:2")
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRequestController.class),
                       handler().methodName("acceptRequestChat"),
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
               .andDo(document("accept-request-chat",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("대화 요청 수락")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("requestId").description("대화 요청 시 생성된 고유 ID"))
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
                                       .responseSchema(schema("ChatRoomResponse"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("대화 요청 거절")
    @WithMockMember
    void rejectRequestChat() throws Exception {
        //given

        //when
        //then
        mockMvc.perform(post("/api/chat/requests/{requestId}/reject", "chat:request:1:2")
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRequestController.class),
                       handler().methodName("rejectRequestChat"),
                       status().isOk()
               )
               .andDo(document("reject-request-chat",
                       resource(
                               builder()
                                       .tag(API_TAG)
                                       .summary("대화 요청 거절")
                                       .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                       .pathParameters(parameterWithName("requestId").description("대화 요청 시 생성된 고유 ID"))
                                       .build()
                       )
               ));
    }

    @Test
    @DisplayName("받은 대화 요청 목록 조회")
    @WithMockMember
    void getReceivedChatRequests() throws Exception {
        //given
        List<RequestChatResDto> response = List.of(
                new RequestChatResDto("request-id-1", 1L, "Walter Umar", "https://sender-1-profile.com",
                        2L, "Amina Morales", "https://receiver-1-profile.com", LocalDateTime.now().plusDays(1)),
                new RequestChatResDto("request-id-2", 3L, "Natalya Bello", "https://sender-2-profile.com",
                        4L, "Richard Santos", "https://receiver-2-profile.com", LocalDateTime.now().plusHours(3)),
                new RequestChatResDto("request-id-3", 5L, "Dmitriy Sari", "https://sender-3-profile.com",
                        6L, "Frank Rai", "https://receiver-3-profile.com", LocalDateTime.now().plusHours(5)));
        given(chatRequestService.getReceivedChatRequests(anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(get("/api/chat/requests/received")
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRequestController.class),
                       handler().methodName("getReceivedChatRequests"),
                       status().isOk(),
                       jsonPath("$.length()").value(response.size())
               )
               .andDo(
                       document("get-received-request-chat",
                               resource(
                                       builder()
                                               .tag(API_TAG)
                                               .summary("받은 대화 요청 목록 조회")
                                               .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                               .responseFields(
                                                       fieldWithPath("[].requestId")
                                                               .type(STRING)
                                                               .description("대화 요청 ID"),
                                                       fieldWithPath("[].senderId")
                                                               .type(NUMBER)
                                                               .description("요청 보낸 사용자 ID"),
                                                       fieldWithPath("[].senderName")
                                                               .type(STRING)
                                                               .description("요청 보낸 사용자 이름"),
                                                       fieldWithPath("[].senderProfileImage")
                                                               .optional()
                                                               .type(STRING)
                                                               .description("요청 보낸 사용자 프로필 이미지"),
                                                       fieldWithPath("[].receiverId")
                                                               .type(NUMBER)
                                                               .description("요청 받은 사용자 ID"),
                                                       fieldWithPath("[].receiverName")
                                                               .type(STRING)
                                                               .description("요청 받은 사용자 이름"),
                                                       fieldWithPath("[].receiverProfileImage")
                                                               .optional()
                                                               .type(STRING)
                                                               .description("요청 받은 사용자 프로필 이미지"),
                                                       fieldWithPath("[].expiresAt")
                                                               .type(STRING)
                                                               .description("요청 만료시간(24시간)")
                                               )
                                               .responseSchema(schema("RequestChatResponse"))
                                               .build()
                               )
                       ));
    }

    @Test
    @DisplayName("보낸 대화 요청 목록 조회")
    @WithMockMember
    void getSentChatRequests() throws Exception {
        //given
        List<RequestChatResDto> response = List.of(
                new RequestChatResDto("request-id-1", 1L, "Walter Umar", "https://sender-1-profile.com",
                        2L, "Amina Morales", "https://receiver-1-profile.com", LocalDateTime.now().plusDays(1)),
                new RequestChatResDto("request-id-2", 3L, "Natalya Bello", "https://sender-2-profile.com",
                        4L, "Richard Santos", "https://receiver-2-profile.com", LocalDateTime.now().plusHours(3)),
                new RequestChatResDto("request-id-3", 5L, "Dmitriy Sari", "https://sender-3-profile.com",
                        6L, "Frank Rai", "https://receiver-3-profile.com", LocalDateTime.now().plusHours(5)));
        given(chatRequestService.getSentChatRequests(anyLong())).willReturn(response);

        //when
        //then
        mockMvc.perform(get("/api/chat/requests/sent")
                       .header(AUTHORIZATION, "Bearer {access-token}")
               )
               .andExpectAll(
                       handler().handlerType(ChatRequestController.class),
                       handler().methodName("getSentChatRequests"),
                       status().isOk(),
                       jsonPath("$.length()").value(response.size())
               )
               .andDo(
                       document("get-sent-request-chat",
                               resource(
                                       builder()
                                               .tag(API_TAG)
                                               .summary("보낸 대화 요청 목록 조회")
                                               .requestHeaders(headerWithName(AUTHORIZATION).description("Bearer {액세스 토큰}"))
                                               .responseFields(
                                                       fieldWithPath("[].requestId")
                                                               .type(STRING)
                                                               .description("대화 요청 ID"),
                                                       fieldWithPath("[].senderId")
                                                               .type(NUMBER)
                                                               .description("요청 보낸 사용자 ID"),
                                                       fieldWithPath("[].senderName")
                                                               .type(STRING)
                                                               .description("요청 보낸 사용자 이름"),
                                                       fieldWithPath("[].senderProfileImage")
                                                               .optional()
                                                               .type(STRING)
                                                               .description("요청 보낸 사용자 프로필 이미지"),
                                                       fieldWithPath("[].receiverId")
                                                               .type(NUMBER)
                                                               .description("요청 받은 사용자 ID"),
                                                       fieldWithPath("[].receiverName")
                                                               .type(STRING)
                                                               .description("요청 받은 사용자 이름"),
                                                       fieldWithPath("[].receiverProfileImage")
                                                               .optional()
                                                               .type(STRING)
                                                               .description("요청 받은 사용자 프로필 이미지"),
                                                       fieldWithPath("[].expiresAt")
                                                               .type(STRING)
                                                               .description("요청 만료시간(24시간)")
                                               )
                                               .responseSchema(schema("RequestChatResponse"))
                                               .build()
                               )
                       ));
    }
}