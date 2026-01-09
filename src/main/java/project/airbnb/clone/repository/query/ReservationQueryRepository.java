package project.airbnb.clone.repository.query;

import org.springframework.stereotype.Repository;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.repository.query.support.CustomQuerydslRepositorySupport;

import java.time.LocalDateTime;

import static project.airbnb.clone.consts.ReservationStatus.CONFIRMED;
import static project.airbnb.clone.entity.reservation.QReservation.reservation;

@Repository
public class ReservationQueryRepository extends CustomQuerydslRepositorySupport {

    public ReservationQueryRepository() {
        super(Reservation.class);
    }

    public boolean existsConfirmedReservation(Long accId, LocalDateTime from, LocalDateTime to) {
        return getQueryFactory()
                .selectOne()
                .from(reservation)
                .where(
                        reservation.accommodation.id.eq(accId),
                        reservation.status.eq(CONFIRMED),
                        reservation.startDate.lt(to),
                        reservation.endDate.gt(from)
                )
                .fetchFirst() != null;
    }
}
