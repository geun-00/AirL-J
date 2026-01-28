package project.airbnb.clone.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import project.airbnb.clone.entity.notification.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 미확인 알림 조회
    List<Notification> findByMemberIdAndReadFalseOrderByCreatedAtDesc(Long memberId);
    
    // 확인한 알림 조회
    List<Notification> findByMemberIdAndReadTrueOrderByCreatedAtDesc(Long memberId);
    
    // 전체 알림 조회
    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    // 미확인 알림 개수
    long countByMemberIdAndReadFalse(Long memberId);
    
    // 알림 일괄 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.member.id = :memberId AND n.read = false")
    int markAllAsReadByMemberId(Long memberId);
    
    // 사용자의 모든 알림 삭제
    void deleteByMemberId(Long memberId);
}
