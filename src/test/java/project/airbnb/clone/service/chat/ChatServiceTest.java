package project.airbnb.clone.service.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.dto.chat.ChatMessageReqDto;
import project.airbnb.clone.dto.chat.ChatMessagesResDto;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.redis.ChatRequestRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatServiceTest extends TestContainerSupport {

    @Autowired ChatService chatService;
    @Autowired MemberRepository memberRepository;
    @Autowired ChatRequestRepository chatRequestRepository;

    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        sender = memberRepository.save(MemberFixture.create());
        receiver = memberRepository.save(MemberFixture.create());
    }

    @Nested
    @DisplayName("채팅 요청 테스트")
    class RequestChatTest {

        @Test
        @DisplayName("Success: 새로운 채팅 요청을 보낸다.")
        void requestChat_success() {
            RequestChatResDto response = chatService.requestChat(receiver.getId(), sender.getId());

            assertThat(response.senderId()).isEqualTo(sender.getId());
            assertThat(chatRequestRepository.existsById(response.requestId())).isTrue();
        }

        @Test
        @DisplayName("Fail: 자기 자신에게 요청하면 예외가 발생한다.")
        void requestChat_toSelf_throwsException() {
            assertThatThrownBy(() -> chatService.requestChat(sender.getId(), sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자신에게 채팅을 요청했습니다");
        }

        @Test
        @DisplayName("Fail: 이미 보낸 요청이 존재하면 예외가 발생한다.")
        void requestChat_alreadyExists_throwsException() {
            chatService.requestChat(receiver.getId(), sender.getId());

            assertThatThrownBy(() -> chatService.requestChat(receiver.getId(), sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 채팅 요청된 기록이 존재");
        }

        @Test
        @DisplayName("Fail: 이미 활성화된 채팅방이 있으면 예외가 발생한다.")
        void requestChat_alreadyActive_throwsException() {
            RequestChatResDto request = chatService.requestChat(receiver.getId(), sender.getId());
            chatService.acceptRequestChat(request.requestId(), receiver.getId());

            assertThatThrownBy(() -> chatService.requestChat(receiver.getId(), sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 활성화된 채팅방이 있습니다");
        }
    }

    @Nested
    @DisplayName("채팅 수락 및 거절 테스트")
    class AcceptOrRejectTest {

        private RequestChatResDto chatRequest;

        @BeforeEach
        void setUp() {
            chatRequest = chatService.requestChat(receiver.getId(), sender.getId());
        }

        @Test
        @DisplayName("Success: 요청을 수락하면 채팅방이 생성되고 요청 데이터는 삭제된다.")
        void acceptRequest_success() {
            ChatRoomResDto room = chatService.acceptRequestChat(chatRequest.requestId(), receiver.getId());

            assertThat(room.roomId()).isNotNull();
            assertThat(chatRequestRepository.existsById(chatRequest.requestId())).isFalse();
        }

        @Test
        @DisplayName("Success: 요청을 거절하면 요청 데이터가 삭제된다.")
        void rejectRequest_success() {
            chatService.rejectRequestChat(chatRequest.requestId(), receiver.getId());

            assertThat(chatRequestRepository.existsById(chatRequest.requestId())).isFalse();
        }

        @Test
        @DisplayName("Fail: 수락 권한이 없는 사용자가 수락하면 예외가 발생한다.")
        void acceptRequest_noPermission_throwsException() {
            Member stranger = memberRepository.save(MemberFixture.create());

            assertThatThrownBy(() -> chatService.acceptRequestChat(chatRequest.requestId(), stranger.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("채팅 요청의 수신자가 아닙니다");
        }
    }

    @Nested
    @DisplayName("메시지 전송 및 이력 테스트")
    class MessageTest {

        private Long roomId;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatService.requestChat(receiver.getId(), sender.getId());
            roomId = chatService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
        }

        @Test
        @DisplayName("Success: 메시지를 저장하고 이력을 조회한다.")
        void saveAndGetMessages_success() {
            chatService.saveChatMessage(roomId, new ChatMessageReqDto(sender.getId(), "Hello"));

            ChatMessagesResDto histories = chatService.getMessageHistories(null, roomId, 10, sender.getId());
            assertThat(histories.messages()).hasSize(1);
            assertThat(histories.messages().get(0).content()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("Fail: 상대방이 나간 채팅방에는 메시지를 보낼 수 없다.")
        void saveMessage_whenPartnerLeft_throwsException() {
            chatService.leaveChatRoom(roomId, receiver.getId(), false);

            assertThatThrownBy(() -> chatService.saveChatMessage(roomId, new ChatMessageReqDto(sender.getId(), "Are you there?")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사용자가 채팅방을 나가 메시지 전송에 실패");
        }

        @Test
        @DisplayName("Fail: 참여하지 않은 채팅방에 메시지를 보내면 예외가 발생한다.")
        void saveMessage_notParticipant_throwsException() {
            Member stranger = memberRepository.save(MemberFixture.create());

            assertThatThrownBy(() -> chatService.saveChatMessage(roomId, new ChatMessageReqDto(stranger.getId(), "Hi")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사용자 조회 실패");
        }
    }

    @Nested
    @DisplayName("채팅방 관리 및 조회 테스트")
    class RoomManagementTest {

        private Long roomId;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatService.requestChat(receiver.getId(), sender.getId());
            roomId = chatService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
        }

        @Test
        @DisplayName("Success: 채팅방 이름을 변경한다.")
        void updateChatRoomName_success() {
            String newName = "새로운 방 이름";
            ChatRoomResDto result = chatService.updateChatRoomName(newName, receiver.getId(), sender.getId(), roomId);

            assertThat(result.customRoomName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("Success: 채팅방을 읽음 처리한다.")
        void markChatRoomAsRead_success() {
            chatService.saveChatMessage(roomId, new ChatMessageReqDto(receiver.getId(), "안읽은 메세지"));
            
            // 예외가 발생하지 않으면 성공
            chatService.markChatRoomAsRead(roomId, sender.getId());
        }

        @Test
        @DisplayName("Success: 참여 중인 채팅방 목록을 조회한다.")
        void getChatRooms_success() {
            List<ChatRoomResDto> rooms = chatService.getChatRooms(sender.getId());
            assertThat(rooms).hasSize(1);
        }

        @Test
        @DisplayName("Success: 나갔던 채팅방에 다시 참여 요청이 오면 재입장(Rejoin) 처리된다.")
        void reactiveIfHasLeft_success() {
            // sender가 방을 나감
            chatService.leaveChatRoom(roomId, sender.getId(), false);
            assertThat(chatService.isChatRoomParticipant(roomId, sender.getId())).isFalse();

            // 다시 요청을 보내고 수락하면 재입장 처리되어야 함
            chatService.requestChat(receiver.getId(), sender.getId());
            String newRequestKey = "chat:chatRequest:" + sender.getId() + ":" + receiver.getId();
            chatService.acceptRequestChat(newRequestKey, receiver.getId());

            assertThat(chatService.isChatRoomParticipant(roomId, sender.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("채팅 요청 목록 조회 테스트")
    class RequestListTest {

        @BeforeEach
        void setUp() {
            chatService.requestChat(receiver.getId(), sender.getId());
        }

        @Test
        @DisplayName("Success: 받은 채팅 요청 목록을 조회한다.")
        void getReceivedChatRequests_success() {
            List<RequestChatResDto> requests = chatService.getReceivedChatRequests(receiver.getId());
            assertThat(requests).hasSize(1);
            assertThat(requests.get(0).senderId()).isEqualTo(sender.getId());
        }

        @Test
        @DisplayName("Success: 보낸 채팅 요청 목록을 조회한다.")
        void getSentChatRequests_success() {
            List<RequestChatResDto> requests = chatService.getSentChatRequests(sender.getId());
            assertThat(requests).hasSize(1);
            assertThat(requests.get(0).receiverId()).isEqualTo(receiver.getId());
        }
    }

    @Nested
    @DisplayName("메시지 페이징 테스트")
    class MessagePagingTest {

        private Long roomId;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatService.requestChat(receiver.getId(), sender.getId());
            roomId = chatService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
            
            chatService.saveChatMessage(roomId, new ChatMessageReqDto(sender.getId(), "M1"));
            chatService.saveChatMessage(roomId, new ChatMessageReqDto(sender.getId(), "M2"));
            chatService.saveChatMessage(roomId, new ChatMessageReqDto(sender.getId(), "M3"));
        }

        @Test
        @DisplayName("Success: 메시지 페이징 조회 시 hasMore가 정상 작동한다.")
        void getMessageHistories_paging() {
            ChatMessagesResDto result = chatService.getMessageHistories(null, roomId, 2, sender.getId());

            assertThat(result.messages()).hasSize(2);
            assertThat(result.hasMore()).isTrue();
        }
    }
}