package project.airbnb.clone.service.chat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.dto.chat.ChatMessagesResDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.entity.chat.ChatMessage;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageServiceTest extends TestContainerSupport {

    @Autowired EntityManager em;
    @Autowired ChatRoomService chatRoomService;
    @Autowired ChatMessageService chatMessageService;
    @Autowired ChatRequestService chatRequestService;

    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        sender = MemberFixture.create();
        receiver = MemberFixture.create();
        em.persist(sender);
        em.persist(receiver);

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("메시지 전송 및 이력 테스트")
    class MessageTest {

        private Long roomId;
        private ChatRoom chatRoom;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatRequestService.requestChat(receiver.getId(), sender.getId());
            roomId = chatRequestService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
            chatRoom = em.find(ChatRoom.class, roomId);
        }

        @Test
        @DisplayName("Success: 메시지를 저장하고 이력을 조회한다.")
        void saveAndGetMessages_success() {
            ChatMessage msg = ChatMessage.create(chatRoom, sender, "Hello");
            em.persist(msg);

            em.flush();
            em.clear();

            ChatMessagesResDto histories = chatMessageService.getMessageHistories(null, roomId, 10);
            assertThat(histories.messages()).hasSize(1);
            assertThat(histories.messages().get(0).getContent()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("Fail: 상대방이 나간 채팅방에는 메시지를 보낼 수 없다.")
        void saveMessage_whenPartnerLeft_throwsException() {
            chatRoomService.leaveChatRoom(roomId, receiver.getId(), false);

            assertThatThrownBy(() -> chatRoomService.validateMessageDelivery(roomId, sender.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("채팅방을 나가 메시지 전송에 실패");
        }

        @Test
        @DisplayName("Fail: 참여하지 않은 채팅방에 메시지를 보내면 예외가 발생한다.")
        void saveMessage_notParticipant_throwsException() {
            Member stranger = MemberFixture.create();
            em.persist(stranger);

            assertThatThrownBy(() -> chatRoomService.validateMessageDelivery(roomId, stranger.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사용자 조회 실패");
        }
    }

    @Nested
    @DisplayName("메시지 페이징 테스트")
    class MessagePagingTest {

        private Long roomId;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatRequestService.requestChat(receiver.getId(), sender.getId());
            roomId = chatRequestService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
            ChatRoom chatRoom = em.find(ChatRoom.class, roomId);

            for (int i = 1; i <= 3; i++) {
                em.persist(ChatMessage.create(chatRoom, sender, "M" + i));
            }

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("Success: 메시지 페이징 조회 시 hasMore가 정상 작동한다.")
        void getMessageHistories_paging() {
            ChatMessagesResDto result = chatMessageService.getMessageHistories(null, roomId, 2);

            assertThat(result.messages()).hasSize(2);
            assertThat(result.hasMore()).isTrue();
        }
    }
}