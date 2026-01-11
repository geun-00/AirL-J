package project.airbnb.clone.fixtures;

import project.airbnb.clone.dto.reservation.PostReservationReqDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.reservation.Reservation;

import java.time.LocalDate;

public class ReservationFixture {

    public static Reservation create(Member member, Accommodation accommodation) {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().minusDays(3);
        return Reservation.createPending(member, accommodation, new PostReservationReqDto(startDate, endDate, 2, 0, 0));
    }
}
