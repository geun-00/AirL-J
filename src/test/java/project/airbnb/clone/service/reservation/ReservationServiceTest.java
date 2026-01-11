package project.airbnb.clone.service.reservation;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.common.exceptions.ErrorCode;
import project.airbnb.clone.consts.ReservationStatus;
import project.airbnb.clone.dto.reservation.PostReservationReqDto;
import project.airbnb.clone.dto.reservation.PostReservationResDto;
import project.airbnb.clone.dto.reservation.PostReviewReqDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.area.AreaCode;
import project.airbnb.clone.entity.area.SigunguCode;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.fixtures.AccommodationFixture;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.repository.jpa.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest extends TestContainerSupport {

    @Autowired EntityManager em;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;

    private Member verifiedMember;
    private Accommodation accommodation;

    @BeforeEach
    void setUp() {
        verifiedMember = MemberFixture.create();
        verifiedMember.verifyEmail();
        em.persist(verifiedMember);

        AreaCode areaCode = AreaCode.create("1", "서울");
        em.persist(areaCode);

        SigunguCode sigunguCode = SigunguCode.create("1-1", "강남구", areaCode);
        em.persist(sigunguCode);

        accommodation = AccommodationFixture.create("test", sigunguCode, 1.1, 1.2);
        em.persist(accommodation);

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("예약 생성 테스트")
    class PostReservationTest {

        private PostReservationReqDto createReqDto(LocalDate start, LocalDate end) {
            return new PostReservationReqDto(start, end, 2, 0, 0);
        }

        @Test
        @DisplayName("Success: 유효한 정보로 예약을 요청하면 PENDING 상태의 예약이 생성된다.")
        void postReservation_success() {
            // given
            PostReservationReqDto reqDto = createReqDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            // when
            PostReservationResDto response = reservationService.postReservation(verifiedMember.getId(), accommodation.getId(), reqDto);

            // then
            assertThat(response.reservationId()).isNotNull();

            Reservation saved = reservationRepository.findById(response.reservationId()).orElseThrow();
            assertThat(saved.getMember().getId()).isEqualTo(verifiedMember.getId());
            assertThat(saved.getAccommodation().getId()).isEqualTo(accommodation.getId());
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("Fail: 이메일 인증이 되지 않은 회원은 예약할 수 없다.")
        void postReservation_unverifiedEmail_throwsException() {
            // given
            Member unverifiedMember = MemberFixture.create();
            em.persist(unverifiedMember);

            PostReservationReqDto reqDto = createReqDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

            // when & then
            assertThatThrownBy(() -> reservationService.postReservation(unverifiedMember.getId(), accommodation.getId(), reqDto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("Fail: 선택한 기간에 이미 확정된 예약이 있으면 예외가 발생한다.")
        void postReservation_alreadyReserved_throwsException() {
            // given
            LocalDate start = LocalDate.now().plusDays(5);
            LocalDate end = LocalDate.now().plusDays(7);

            Reservation existing = Reservation.createPending(verifiedMember, accommodation, createReqDto(start, end));
            existing.confirm();
            em.persist(existing);

            PostReservationReqDto newReq = createReqDto(start, end);

            // when & then
            assertThatThrownBy(() -> reservationService.postReservation(verifiedMember.getId(), accommodation.getId(), newReq))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_RESERVED);
        }

        @Test
        @DisplayName("Fail: 존재하지 않는 숙소에 대해 예약하면 예외가 발생한다.")
        void postReservation_notFoundAccommodation_throwsException() {
            // given
            PostReservationReqDto reqDto = createReqDto(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

            // when & then
            assertThatThrownBy(() -> reservationService.postReservation(verifiedMember.getId(), 999L, reqDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("숙소 조회 실패");
        }
    }

    @Nested
    @DisplayName("리뷰 작성 테스트")
    class PostReviewTest {

        private Long reservationId;

        @BeforeEach
        void setUp() {
            PostReservationReqDto reqDto = new PostReservationReqDto(LocalDate.now(), LocalDate.now().plusDays(1), 1, 0, 0);
            PostReservationResDto res = reservationService.postReservation(verifiedMember.getId(), accommodation.getId(), reqDto);
            reservationId = res.reservationId();
        }

        @Test
        @DisplayName("Success: 예약에 대해 리뷰를 작성한다.")
        void postReview_success() {
            // given
            PostReviewReqDto reqDto = new PostReviewReqDto(BigDecimal.valueOf(5.0), "정말 좋았습니다!");

            // when
            reservationService.postReview(reservationId, reqDto, verifiedMember.getId());

            // then
            assertThat(reviewRepository.findAll()).hasSize(1);
            assertThat(reviewRepository.findAll().get(0).getContent()).isEqualTo("정말 좋았습니다!");
        }

        @Test
        @DisplayName("Fail: 존재하지 않는 예약에 리뷰를 쓰려 하면 예외가 발생한다.")
        void postReview_notFoundReservation_throwsException() {
            // given
            PostReviewReqDto reqDto = new PostReviewReqDto(BigDecimal.valueOf(2.0), "Bad");

            // when & then
            assertThatThrownBy(() -> reservationService.postReview(999L, reqDto, verifiedMember.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("예약 조회 실패");
        }
    }
}