package project.airbnb.clone.entity.notification;

import jakarta.persistence.*;
import lombok.*;
import project.airbnb.clone.entity.BaseEntity;
import project.airbnb.clone.entity.member.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "reference_id", length = 100)
    private String referenceId;  // 채팅방 ID, 예약 ID 등

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 읽음 처리
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
