package project.airbnb.clone.entity.reservation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.airbnb.clone.consts.ReservationStatus;
import project.airbnb.clone.dto.reservation.PostReservationReqDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.BaseEntity;
import project.airbnb.clone.entity.member.Member;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "adults", nullable = false)
    private int adults;

    @Column(name = "children", nullable = false)
    private int children;

    @Column(name = "infant", nullable = false)
    private int infants;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    public static Reservation createPending(Member member, Accommodation accommodation, PostReservationReqDto reqDto) {
        return new Reservation(member, accommodation, reqDto.adults(), reqDto.children(), reqDto.infants(),
                reqDto.startDate().atStartOfDay(), reqDto.endDate().atTime(23, 59, 59), ReservationStatus.PENDING);
    }

    private Reservation(Member member, Accommodation accommodation, int adults, int children, int infants, LocalDateTime startDate, LocalDateTime endDate, ReservationStatus status) {
        this.member = member;
        this.accommodation = accommodation;
        this.adults = adults;
        this.children = children;
        this.infants = infants;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public boolean isOwner(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}