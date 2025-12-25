package project.airbnb.clone.service.accommodation;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.events.view.ViewHistoryEvent;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.consts.DayType;
import project.airbnb.clone.consts.Season;
import project.airbnb.clone.dto.PageResponseDto;
import project.airbnb.clone.dto.accommodation.*;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.accommodation.AccommodationImage;
import project.airbnb.clone.entity.accommodation.AccommodationPrice;
import project.airbnb.clone.entity.area.AreaCode;
import project.airbnb.clone.entity.area.SigunguCode;
import project.airbnb.clone.entity.history.ViewHistory;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.AccommodationFixture;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.service.DateManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static project.airbnb.clone.consts.DayType.WEEKDAY;
import static project.airbnb.clone.consts.DayType.WEEKEND;
import static project.airbnb.clone.consts.Season.OFF;
import static project.airbnb.clone.consts.Season.PEAK;

@Transactional
@RecordApplicationEvents
class AccommodationServiceTest extends TestContainerSupport {

    @Autowired ApplicationEvents applicationEvents;
    @Autowired AccommodationService accommodationService;
    @Autowired AccommodationStatisticsService statisticsService;
    @Autowired EntityManager em;

    @MockitoBean DateManager dateManager;

    Member member;
    AreaCode seoulArea;
    AreaCode busanArea;
    SigunguCode gangnamSigungu;
    SigunguCode haeundaeSigungu;

    @BeforeEach
    void setUp() {
        member = MemberFixture.create();
        em.persist(member);

        seoulArea = AreaCode.create("11", "서울");
        busanArea = AreaCode.create("21", "부산");
        em.persist(seoulArea);
        em.persist(busanArea);

        gangnamSigungu = SigunguCode.create("11680", "강남구", seoulArea);
        haeundaeSigungu = SigunguCode.create("21140", "해운대구", busanArea);
        em.persist(gangnamSigungu);
        em.persist(haeundaeSigungu);
    }

    @Nested
    @DisplayName("메인 페이지 숙소 조회")
    class GetAccommodationsTest {

        @Test
        @DisplayName("성공 - 지역별 숙소 목록 조회")
        void getAccommodations_success() {
            // given
            Accommodation acc1 = createAccommodation("서울 숙소1", gangnamSigungu, 127.0, 37.5);
            Accommodation acc2 = createAccommodation("서울 숙소2", gangnamSigungu, 127.1, 37.5);
            Accommodation acc3 = createAccommodation("부산 숙소1", haeundaeSigungu, 129.0, 35.1);
            em.persist(acc1);
            em.persist(acc2);
            em.persist(acc3);

            createPriceAndImage(acc1, PEAK, WEEKEND, 150000);
            createPriceAndImage(acc2, PEAK, WEEKEND, 120000);
            createPriceAndImage(acc3, PEAK, WEEKEND, 100000);

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            statisticsService.refreshStats();

            // when
            List<MainAccResDto> result = accommodationService.getAccommodations(member.getId());

            // then
            assertThat(result).hasSize(2);

            MainAccResDto seoulResult = result.stream()
                                              .filter(r -> r.areaName().equals("서울"))
                                              .findFirst()
                                              .orElseThrow();
            assertThat(seoulResult.accommodations()).hasSize(2);

            MainAccResDto busanResult = result.stream()
                                              .filter(r -> r.areaName().equals("부산"))
                                              .findFirst()
                                              .orElseThrow();
            assertThat(busanResult.accommodations()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 한 지역에 8개 초과 숙소가 있을 때 8개만 반환")
        void getAccommodations_limit_8() {
            // given
            for (int i = 1; i <= 10; i++) {
                Accommodation acc = createAccommodation("서울 숙소" + i, gangnamSigungu, 127.0, 37.5);
                em.persist(acc);
                createPriceAndImage(acc, PEAK, WEEKEND, 100000 + (i * 10000));
            }

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            statisticsService.refreshStats();

            // when
            List<MainAccResDto> result = accommodationService.getAccommodations(null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).accommodations()).hasSize(8);
        }
    }

    @Nested
    @DisplayName("검색 숙소 조회(필터링)")
    class GetFilteredPagingAccommodationsTest {

        @Test
        @DisplayName("성공 - 지역 필터링")
        void filterByAreaCode() {
            // given
            Accommodation seoulAcc = createAccommodation("서울 숙소", gangnamSigungu, 127.0, 37.5);
            Accommodation busanAcc = createAccommodation("부산 숙소", haeundaeSigungu, 129.0, 35.1);
            em.persist(seoulAcc);
            em.persist(busanAcc);

            createPriceAndImage(seoulAcc, PEAK, WEEKEND, 150000);
            createPriceAndImage(busanAcc, PEAK, WEEKEND, 100000);

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            AccSearchCondDto searchDto = new AccSearchCondDto("11", null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            PageResponseDto<FilteredAccListResDto> result = accommodationService.getFilteredPagingAccommodations(searchDto, member.getId(), pageable);

            // then
            assertThat(result.getContents()).hasSize(1);
            assertThat(result.getContents().get(0).title()).isEqualTo("서울 숙소");
            assertThat(result.getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 가격 필터링")
        void filterByPriceRange() {
            // given
            Accommodation acc1 = createAccommodation("저렴한 숙소", gangnamSigungu, 127.0, 37.5);
            Accommodation acc2 = createAccommodation("중간 숙소", gangnamSigungu, 127.1, 37.5);
            Accommodation acc3 = createAccommodation("비싼 숙소", gangnamSigungu, 127.2, 37.5);
            em.persist(acc1);
            em.persist(acc2);
            em.persist(acc3);

            createPriceAndImage(acc1, PEAK, WEEKEND, 80000);
            createPriceAndImage(acc2, PEAK, WEEKEND, 150000);
            createPriceAndImage(acc3, PEAK, WEEKEND, 300000);

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            AccSearchCondDto searchDto = new AccSearchCondDto(null, null, 100000, 200000);

            // when
            PageResponseDto<FilteredAccListResDto> result = accommodationService.getFilteredPagingAccommodations(searchDto, null, PageRequest.of(0, 10));

            // then
            List<FilteredAccListResDto> contents = result.getContents();
            assertThat(contents).hasSize(1);
            assertThat(contents.get(0).title()).isEqualTo("중간 숙소");
            assertThat(contents.get(0).price()).isEqualTo(150000);
        }

        @Test
        @DisplayName("성공 - 페이징 확인")
        void pagination() {
            // given
            for (int i = 1; i <= 25; i++) {
                Accommodation acc = createAccommodation("숙소" + i, gangnamSigungu, 127.0, 37.5);
                em.persist(acc);
                createPriceAndImage(acc, PEAK, WEEKEND, 100000);
            }

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            AccSearchCondDto searchDto = new AccSearchCondDto(null, null, null, null);

            // when
            PageResponseDto<FilteredAccListResDto> page1 = accommodationService.getFilteredPagingAccommodations(searchDto, null, PageRequest.of(0, 10));
            PageResponseDto<FilteredAccListResDto> page2 = accommodationService.getFilteredPagingAccommodations(searchDto, null, PageRequest.of(1, 10));
            PageResponseDto<FilteredAccListResDto> page3 = accommodationService.getFilteredPagingAccommodations(searchDto, null, PageRequest.of(2, 10));

            // then
            assertThat(page1.getContents()).hasSize(10);
            assertThat(page2.getContents()).hasSize(10);
            assertThat(page3.getContents()).hasSize(5);
            assertThat(page1.getTotalCount()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("특정 숙소 상세 조회")
    class GetDetailAccommodationTest {

        @Test
        @DisplayName("성공 - 상세 숙소 조회 (회원)")
        void getDetailAccommodation_with_member() {
            // given
            Accommodation acc = createAccommodation("럭셔리 숙소", gangnamSigungu, 127.0, 37.5);
            em.persist(acc);

            createPriceAndImage(acc, PEAK, WEEKEND, 200000);
            createPriceAndImage(acc, OFF, WEEKDAY, 100000);

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            // when
            DetailAccommodationResDto result = accommodationService.getDetailAccommodation(acc.getId(), member.getId());

            // then
            assertThat(result.accommodationId()).isEqualTo(acc.getId());
            assertThat(result.title()).isEqualTo("럭셔리 숙소");
            assertThat(result.price()).isEqualTo(200000);
            assertThat(result.images().thumbnail()).isNotNull();

            // ViewHistoryEvent 발행 확인
            long eventCount = applicationEvents.stream(ViewHistoryEvent.class).count();
            assertThat(eventCount).isEqualTo(1);

            ViewHistoryEvent event = applicationEvents.stream(ViewHistoryEvent.class)
                                                      .findFirst()
                                                      .orElseThrow();
            assertThat(event.accommodationId()).isEqualTo(acc.getId());
            assertThat(event.memberId()).isEqualTo(member.getId());
        }

        @Test
        @DisplayName("성공 - 상세 숙소 조회 (비회원, 이벤트 발행 안함)")
        void getDetailAccommodation_without_member() {
            // given
            Accommodation acc = createAccommodation("숙소", gangnamSigungu, 127.0, 37.5);
            em.persist(acc);

            createPriceAndImage(acc, OFF, WEEKDAY, 100000);

            given(dateManager.getSeason(any(LocalDate.class))).willReturn(OFF);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKDAY);

            // when
            DetailAccommodationResDto result = accommodationService.getDetailAccommodation(acc.getId(), null);

            // then
            assertThat(result.accommodationId()).isEqualTo(acc.getId());

            // ViewHistoryEvent 발행 안됨
            long eventCount = applicationEvents.stream(ViewHistoryEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 숙소 조회")
        void getDetailAccommodation_not_found() {
            // given
            Long nonExistentId = 999L;
            given(dateManager.getSeason(any(LocalDate.class))).willReturn(PEAK);
            given(dateManager.getDayType(any(LocalDate.class))).willReturn(WEEKEND);

            // when & then
            assertThatThrownBy(() -> accommodationService.getDetailAccommodation(nonExistentId, member.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("id=999 숙소 조회 실패");
        }
    }

    @Nested
    @DisplayName("최근 본 숙소 조회")
    class GetRecentViewAccommodationsTest {

        @Test
        @DisplayName("성공 - 최근 본 숙소 날짜별 그룹화")
        void getRecentViewAccommodations_grouped_by_date() {
            // given
            Accommodation acc1 = createAccommodation("숙소1", gangnamSigungu, 127.0, 37.5);
            Accommodation acc2 = createAccommodation("숙소2", gangnamSigungu, 127.1, 37.5);
            Accommodation acc3 = createAccommodation("숙소3", gangnamSigungu, 127.2, 37.5);
            em.persist(acc1);
            em.persist(acc2);
            em.persist(acc3);

            createPriceAndImage(acc1, PEAK, WEEKEND, 100000);
            createPriceAndImage(acc2, PEAK, WEEKEND, 120000);
            createPriceAndImage(acc3, PEAK, WEEKEND, 150000);

            LocalDateTime today = LocalDateTime.now();
            LocalDateTime yesterday = today.minusDays(1);

            ViewHistory vh1 = ViewHistory.create(member, acc1, today);
            ViewHistory vh2 = ViewHistory.create(member, acc2, today.minusHours(1));
            ViewHistory vh3 = ViewHistory.create(member, acc3, yesterday);
            em.persist(vh1);
            em.persist(vh2);
            em.persist(vh3);

            // when
            List<ViewHistoryResDto> result = accommodationService.getRecentViewAccommodations(member.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).date()).isEqualTo(today.toLocalDate());
            assertThat(result.get(0).accommodations()).hasSize(2);
            assertThat(result.get(1).date()).isEqualTo(yesterday.toLocalDate());
            assertThat(result.get(1).accommodations()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 30일 이전 데이터는 조회되지 않음")
        void getRecentViewAccommodations_only_last_30_days() {
            // given
            Accommodation acc1 = createAccommodation("최근 숙소", gangnamSigungu, 127.0, 37.5);
            Accommodation acc2 = createAccommodation("오래된 숙소", gangnamSigungu, 127.1, 37.5);
            em.persist(acc1);
            em.persist(acc2);

            createPriceAndImage(acc1, PEAK, WEEKEND, 100000);
            createPriceAndImage(acc2, PEAK, WEEKEND, 100000);

            ViewHistory recent = ViewHistory.create(member, acc1, LocalDateTime.now().minusDays(10));
            ViewHistory old = ViewHistory.create(member, acc2, LocalDateTime.now().minusDays(31));
            em.persist(recent);
            em.persist(old);

            // when
            List<ViewHistoryResDto> result = accommodationService.getRecentViewAccommodations(member.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).accommodations()).hasSize(1);
            assertThat(result.get(0).accommodations().get(0).title()).isEqualTo("최근 숙소");
        }
    }

    @Nested
    @DisplayName("특정 숙소 가격 조회")
    class GetAccommodationPriceTest {

        @Test
        @DisplayName("성공 - 성수기 주말 가격 조회")
        void getAccommodationPrice_peak_weekend() {
            // given
            Accommodation acc = createAccommodation("숙소", gangnamSigungu, 127.0, 37.5);
            em.persist(acc);

            createPrice(acc, PEAK, WEEKEND, 200000);
            createPrice(acc, PEAK, WEEKDAY, 150000);
            createPrice(acc, OFF, WEEKEND, 120000);
            createPrice(acc, OFF, WEEKDAY, 100000);

            LocalDate date = LocalDate.of(2025, 8, 2);
            given(dateManager.getSeason(date)).willReturn(PEAK);
            given(dateManager.getDayType(date)).willReturn(WEEKEND);

            // when
            AccommodationPriceResDto result = accommodationService.getAccommodationPrice(acc.getId(), date);

            // then
            assertThat(result.accommodationId()).isEqualTo(acc.getId());
            assertThat(result.date()).isEqualTo(date);
            assertThat(result.price()).isEqualTo(200000);
        }

        @Test
        @DisplayName("성공 - 비성수기 평일 가격 조회")
        void getAccommodationPrice_off_weekday() {
            // given
            Accommodation acc = createAccommodation("숙소", gangnamSigungu, 127.0, 37.5);
            em.persist(acc);

            createPrice(acc, PEAK, WEEKEND, 200000);
            createPrice(acc, OFF, WEEKDAY, 80000);

            LocalDate date = LocalDate.of(2025, 3, 4);
            given(dateManager.getSeason(date)).willReturn(OFF);
            given(dateManager.getDayType(date)).willReturn(WEEKDAY);

            // when
            AccommodationPriceResDto result = accommodationService.getAccommodationPrice(acc.getId(), date);

            // then
            assertThat(result.price()).isEqualTo(80000);
        }
    }

    private Accommodation createAccommodation(String title, SigunguCode sigunguCode, double mapX, double mapY) {
        return AccommodationFixture.create(title, sigunguCode, mapX, mapY);
    }

    private void createPriceAndImage(Accommodation acc, Season season, DayType dayType, int price) {
        createPrice(acc, season, dayType, price);
        createImage(acc);
    }

    private void createPrice(Accommodation acc, Season season, DayType dayType, int price) {
        AccommodationPrice accPrice = AccommodationPrice.create(acc, season, dayType, price);
        em.persist(accPrice);
    }

    private void createImage(Accommodation acc) {
        AccommodationImage image = AccommodationImage.thumbnailOf(acc, "https://example.com/image.jpg");
        em.persist(image);
    }
}