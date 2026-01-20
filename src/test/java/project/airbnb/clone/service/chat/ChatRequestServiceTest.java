package project.airbnb.clone.service.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.redis.ChatRequestRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRequestServiceTest extends TestContainerSupport {

    @Autowired MemberRepository memberRepository;
    @Autowired ChatRequestService chatRequestService;
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
            RequestChatResDto response = chatRequestService.requestChat(receiver.getId(), sender.getId());

            assertThat(response.senderId()).isEqualTo(sender.getId());
            assertThat(chatRequestRepository.existsById(response.requestId())).isTrue();
        }

        @Test
        @DisplayName("Fail: 자기 자신에게 요청하면 예외가 발생한다.")
        void requestChat_toSelf_throwsException() {
            assertThatThrownBy(() -> chatRequestService.requestChat(sender.getId(), sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자신에게 채팅을 요청했습니다");
        }

        @Test
        @DisplayName("Fail: 이미 보낸 요청이 존재하면 예외가 발생한다.")
        void requestChat_alreadyExists_throwsException() {
            chatRequestService.requestChat(receiver.getId(), sender.getId());

            assertThatThrownBy(() -> chatRequestService.requestChat(receiver.getId(), sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 채팅 요청된 기록이 존재");
        }

        @Test
        @DisplayName("Fail: 이미 활성화된 채팅방이 있으면 예외가 발생한다.")
        void requestChat_alreadyActive_throwsException() {
            RequestChatResDto request = chatRequestService.requestChat(receiver.getId(), sender.getId());
            chatRequestService.acceptRequestChat(request.requestId(), receiver.getId());

            assertThatThrownBy(() -> chatRequestService.requestChat(receiver.getId(), sender.getId()))
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
            chatRequest = chatRequestService.requestChat(receiver.getId(), sender.getId());
        }

        @Test
        @DisplayName("Success: 요청을 수락하면 채팅방이 생성되고 요청 데이터는 삭제된다.")
        void acceptRequest_success() {
            ChatRoomResDto room = chatRequestService.acceptRequestChat(chatRequest.requestId(), receiver.getId());

            assertThat(room.roomId()).isNotNull();
            assertThat(chatRequestRepository.existsById(chatRequest.requestId())).isFalse();
        }

        @Test
        @DisplayName("Success: 요청을 거절하면 요청 데이터가 삭제된다.")
        void rejectRequest_success() {
            chatRequestService.rejectRequestChat(chatRequest.requestId(), receiver.getId());

            assertThat(chatRequestRepository.existsById(chatRequest.requestId())).isFalse();
        }

        @Test
        @DisplayName("Fail: 수락 권한이 없는 사용자가 수락하면 예외가 발생한다.")
        void acceptRequest_noPermission_throwsException() {
            Member stranger = memberRepository.save(MemberFixture.create());

            assertThatThrownBy(() -> chatRequestService.acceptRequestChat(chatRequest.requestId(), stranger.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("채팅 요청의 수신자가 아닙니다");
        }
    }

    @Nested
    @DisplayName("채팅 요청 목록 조회 테스트")
    class RequestListTest {

        @BeforeEach
        void setUp() {
            chatRequestService.requestChat(receiver.getId(), sender.getId());
        }

        @Test
        @DisplayName("Success: 받은 채팅 요청 목록을 조회한다.")
        void getReceivedChatRequests_success() {
            List<RequestChatResDto> requests = chatRequestService.getReceivedChatRequests(receiver.getId());
            assertThat(requests).hasSize(1);
            assertThat(requests.get(0).senderId()).isEqualTo(sender.getId());
        }

        @Test
        @DisplayName("Success: 보낸 채팅 요청 목록을 조회한다.")
        void getSentChatRequests_success() {
            List<RequestChatResDto> requests = chatRequestService.getSentChatRequests(sender.getId());
            assertThat(requests).hasSize(1);
            assertThat(requests.get(0).receiverId()).isEqualTo(receiver.getId());
        }
    }
}