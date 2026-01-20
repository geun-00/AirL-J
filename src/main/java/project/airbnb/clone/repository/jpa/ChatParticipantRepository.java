package project.airbnb.clone.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.airbnb.clone.entity.chat.ChatParticipant;
import project.airbnb.clone.entity.chat.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);

    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE ChatParticipant cp
                SET cp.customRoomName = :customName
                WHERE cp.chatRoom = :chatRoom
                AND cp.member.id = :memberId
            """)
    int updateCustomName(@Param("customName") String customName, @Param("chatRoom") ChatRoom chatRoom, @Param("memberId") Long memberId);

    Optional<ChatParticipant> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    @Query("""
        SELECT cp.member.id
        FROM ChatParticipant cp
        WHERE cp.chatRoom.id = :roomId AND cp.isActive = true
        """)
    List<Long> getParticipantIdsByRoomId(@Param("roomId") Long roomId);
}