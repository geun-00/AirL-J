package project.airbnb.clone.service.review;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.dto.PageResponseDto;
import project.airbnb.clone.dto.review.MyReviewResDto;
import project.airbnb.clone.dto.review.UpdateReviewReqDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.accommodation.AccommodationImage;
import project.airbnb.clone.entity.area.AreaCode;
import project.airbnb.clone.entity.area.SigunguCode;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.entity.reservation.Review;
import project.airbnb.clone.fixtures.AccommodationFixture;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.fixtures.ReservationFixture;
import project.airbnb.clone.repository.jpa.ReviewRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewServiceTest extends TestContainerSupport {

    @Autowired ReviewService reviewService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    private Member member;
    private Review myReview;

    @BeforeEach
    void setUp() {
        member = MemberFixture.create();
        em.persist(member);

        AreaCode areaCode = AreaCode.create("1", "서울");
        em.persist(areaCode);

        SigunguCode sigunguCode = SigunguCode.create("1-1", "강남구", areaCode);
        em.persist(sigunguCode);

        Accommodation accommodation = AccommodationFixture.create("숙소", sigunguCode, 1.0, 1.0);
        em.persist(accommodation);

        AccommodationImage thumbnail = AccommodationFixture.createThumbnail(accommodation);
        em.persist(thumbnail);

        Reservation reservation = ReservationFixture.create(member, accommodation);
        em.persist(reservation);

        myReview = Review.create(4.5, "정말 좋았어요!", reservation, member);
        em.persist(myReview);

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class GetReviewsTest {

        @Test
        @DisplayName("Success: 내가 작성한 리뷰 목록을 페이징하여 조회한다.")
        void getMyReviews_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            PageResponseDto<MyReviewResDto> result = reviewService.getMyReviews(member.getId(), pageable);

            // then
            assertThat(result.getContents()).hasSize(1);
            assertThat(result.getContents().get(0).content()).isEqualTo("정말 좋았어요!");
            assertThat(result.getTotalCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("리뷰 수정 테스트")
    class UpdateReviewTest {

        @Test
        @DisplayName("Success: 리뷰 내용과 평점을 수정한다.")
        void updateReview_success() {
            // given
            UpdateReviewReqDto reqDto = new UpdateReviewReqDto(BigDecimal.valueOf(5.0), "생각해보니 최고였어요!");

            // when
            reviewService.updateReview(myReview.getId(), reqDto, member.getId());

            // then
            Review updatedReview = reviewRepository.findById(myReview.getId()).get();
            assertThat(updatedReview.getContent()).isEqualTo("생각해보니 최고였어요!");
            assertThat(updatedReview.getRating()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Fail: 타인의 리뷰를 수정하려 하면 예외가 발생한다.")
        void updateReview_notOwner_throwsException() {
            // given
            Member stranger = MemberFixture.create();
            em.persist(stranger);
            em.flush();

            UpdateReviewReqDto reqDto = new UpdateReviewReqDto(BigDecimal.valueOf(1.0), "해킹 시도");

            // when & then
            assertThatThrownBy(() -> reviewService.updateReview(myReview.getId(), reqDto, stranger.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("후기 조회 실패");
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 테스트")
    class DeleteReviewTest {

        @Test
        @DisplayName("Success: 작성한 리뷰를 삭제한다.")
        void deleteReview_success() {
            // when
            reviewService.deleteReview(myReview.getId(), member.getId());

            // then
            assertThat(reviewRepository.findById(myReview.getId())).isEmpty();
        }

        @Test
        @DisplayName("Fail: 존재하지 않는 리뷰 ID로 삭제 요청 시 예외가 발생한다.")
        void deleteReview_notFound_throwsException() {
            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(999L, member.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }
}