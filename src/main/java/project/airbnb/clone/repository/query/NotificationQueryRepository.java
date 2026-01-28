package project.airbnb.clone.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import project.airbnb.clone.entity.notification.Notification;
import project.airbnb.clone.entity.notification.NotificationType;

import java.util.List;

import static project.airbnb.clone.entity.notification.QNotification.notification;

/**
 * 알림 동적 쿼리를 위한 QueryDSL Repository
 */
@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 알림 목록 동적 조회
     * 
     * @param memberId 회원 ID (필수)
     * @param isRead 읽음 여부 (null이면 전체 조회)
     * @param type 알림 타입 (null이면 전체 타입)
     * @return 조건에 맞는 알림 목록
     */
    public List<Notification> findNotifications(Long memberId, Boolean isRead, NotificationType type) {
        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.member.id.eq(memberId),
                        eqRead(isRead),
                        eqType(type)
                )
                .orderBy(notification.createdAt.desc())
                .fetch();
    }

    /**
     * 미확인 알림 개수 조회
     */
    public long countUnreadNotifications(Long memberId) {
        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.member.id.eq(memberId),
                        notification.read.isFalse()
                )
                .fetchCount();
    }

    /**
     * 특정 타입의 미확인 알림 개수 조회
     */
    public long countUnreadNotificationsByType(Long memberId, NotificationType type) {
        return queryFactory
                .selectFrom(notification)
                .where(
                        notification.member.id.eq(memberId),
                        notification.read.isFalse(),
                        eqType(type)
                )
                .fetchCount();
    }

    /**
     * 읽음 여부 필터링 조건
     */
    private BooleanExpression eqRead(Boolean isRead) {
        if (isRead == null) {
            return null; // 조건 무시
        }
        return isRead ? notification.read.isTrue() : notification.read.isFalse();
    }

    /**
     * 알림 타입 필터링 조건
     */
    private BooleanExpression eqType(NotificationType type) {
        return type != null ? notification.type.eq(type) : null;
    }
}
