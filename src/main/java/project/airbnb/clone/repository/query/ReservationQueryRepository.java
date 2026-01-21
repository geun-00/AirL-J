package project.airbnb.clone.repository.query;

import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Repository;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.repository.dto.ReservedDateQueryDto;
import project.airbnb.clone.repository.query.support.CustomQuerydslRepositorySupport;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<ReservedDateQueryDto> findReservedDatesByAccommodationId(Long accommodationId) {
        return getQueryFactory()
                .select(Projections.constructor(ReservedDateQueryDto.class,
                        reservation.startDate,
                        reservation.endDate))
                .from(reservation)
                .where(reservation.accommodation.id.eq(accommodationId)
                                                   .and(reservation.status.eq(CONFIRMED))
                                                   .and(reservation.endDate.after(LocalDateTime.now())))
                .fetch();
    }
}
