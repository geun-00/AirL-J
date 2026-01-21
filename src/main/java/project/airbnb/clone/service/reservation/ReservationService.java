package project.airbnb.clone.service.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.common.exceptions.ErrorCode;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.common.exceptions.factory.ReservationExceptions;
import project.airbnb.clone.dto.reservation.PostReservationReqDto;
import project.airbnb.clone.dto.reservation.PostReservationResDto;
import project.airbnb.clone.dto.reservation.PostReviewReqDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.entity.reservation.Review;
import project.airbnb.clone.repository.jpa.*;
import project.airbnb.clone.repository.query.ReservationQueryRepository;
import project.airbnb.clone.service.CacheService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final CacheService cacheService;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final AccommodationRepository accommodationRepository;
    private final ReservationQueryRepository reservationQueryRepository;
    private final AccommodationImageRepository accommodationImageRepository;

    @Transactional
    public PostReservationResDto postReservation(Long memberId, Long accommodationId, PostReservationReqDto reqDto) {
        Member member = memberRepository.findById(memberId)
                                        .orElseThrow(() -> MemberExceptions.notFoundById(memberId));
        //사용자 이메일 인증 안되어 있을 시 실패
        if (!member.getIsEmailVerified()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                                             .orElseThrow(() -> AccommodationExceptions.notFoundById(accommodationId));
        //요청 기간에 이미 결제까지 이루어진 예약이 있으면 실패
        LocalDateTime from = reqDto.startDate().atStartOfDay();
        LocalDateTime to = reqDto.endDate().atTime(23, 59, 59);

        if (reservationQueryRepository.existsConfirmedReservation(accommodation.getId(), from, to)) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED);
        }

        Reservation reservation = reservationRepository.save(Reservation.createPending(member, accommodation, reqDto));
        String thumbnailUrl = accommodationImageRepository.findThumbnailUrl(accommodation);

        return PostReservationResDto.of(accommodation, thumbnailUrl, reservation);
    }

    @Transactional
    public void postReview(Long reservationId, PostReviewReqDto reqDto, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                                                       .orElseThrow(() -> ReservationExceptions.notFoundById(reservationId));
        Member member = memberRepository.findById(memberId)
                                        .orElseThrow(() -> MemberExceptions.notFoundById(memberId));

        reviewRepository.save(Review.create(reqDto.rating().doubleValue(), reqDto.content(), reservation, member));
        cacheService.evictAccCommonInfo(reservation.getAccommodation().getId());
    }
}
