package project.airbnb.clone.service.chat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.dto.chat.ChatRoomResDto;
import project.airbnb.clone.dto.chat.RequestChatResDto;
import project.airbnb.clone.entity.chat.ChatMessage;
import project.airbnb.clone.entity.chat.ChatRoom;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomServiceTest extends TestContainerSupport {

    @Autowired EntityManager em;
    @Autowired ChatRoomService chatRoomService;
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
    @DisplayName("채팅방 관리 및 조회 테스트")
    class RoomManagementTest {

        private Long roomId;

        @BeforeEach
        void setUp() {
            RequestChatResDto request = chatRequestService.requestChat(receiver.getId(), sender.getId());
            roomId = chatRequestService.acceptRequestChat(request.requestId(), receiver.getId()).roomId();
        }

        @Test
        @DisplayName("Success: 채팅방 이름을 변경한다.")
        void updateChatRoomName_success() {
            String newName = "새로운 방 이름";
            ChatRoomResDto result = chatRoomService.updateChatRoomName(newName, receiver.getId(), sender.getId(), roomId);

            assertThat(result.customRoomName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("Success: 채팅방을 읽음 처리한다.")
        void markChatRoomAsRead_success() {
            ChatRoom chatRoom = em.find(ChatRoom.class, roomId);
            ChatMessage.create(chatRoom, receiver, "test message");

            em.flush();
            em.clear();

            // 예외가 발생하지 않으면 성공
            chatRoomService.markChatRoomAsRead(roomId, sender.getId());
        }

        @Test
        @DisplayName("Success: 참여 중인 채팅방 목록을 조회한다.")
        void getChatRooms_success() {
            List<ChatRoomResDto> rooms = chatRoomService.getChatRooms(sender.getId());
            assertThat(rooms).hasSize(1);
        }

        @Test
        @DisplayName("Success: 나갔던 채팅방에 다시 참여 요청이 오면 재입장(Rejoin) 처리된다.")
        void reactiveIfHasLeft_success() {
            // sender가 방을 나감
            chatRoomService.leaveChatRoom(roomId, sender.getId(), false);
            assertThat(chatRoomService.isChatRoomParticipant(roomId, sender.getId())).isFalse();

            // 다시 요청을 보내고 수락하면 재입장 처리되어야 함
            chatRequestService.requestChat(receiver.getId(), sender.getId());
            String newRequestKey = "chat:chatRequest:" + sender.getId() + ":" + receiver.getId();
            chatRequestService.acceptRequestChat(newRequestKey, receiver.getId());

            assertThat(chatRoomService.isChatRoomParticipant(roomId, sender.getId())).isTrue();
        }
    }
}