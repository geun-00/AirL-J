package project.airbnb.clone.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.events.view.ViewHistoryEvent;
import project.airbnb.clone.consts.DayType;
import project.airbnb.clone.consts.Season;
import project.airbnb.clone.dto.PageResponseDto;
import project.airbnb.clone.dto.accommodation.*;
import project.airbnb.clone.repository.dto.MainAccListQueryDto;
import project.airbnb.clone.repository.dto.ReservedDateQueryDto;
import project.airbnb.clone.repository.query.AccommodationQueryRepository;
import project.airbnb.clone.repository.query.ReservationQueryRepository;
import project.airbnb.clone.repository.query.WishlistQueryRepository;
import project.airbnb.clone.service.CacheService;
import project.airbnb.clone.service.DateManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static project.airbnb.clone.dto.accommodation.DetailAccommodationResDto.WishlistInfo;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationService {

    private final DateManager dateManager;
    private final CacheService cacheService;
    private final ViewHistoryService viewHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final WishlistQueryRepository wishlistQueryRepository;
    private final ReservationQueryRepository reservationQueryRepository;
    private final AccommodationQueryRepository accommodationQueryRepository;

    public List<MainAccResDto> getAccommodations(Long memberId) {
        LocalDate now = LocalDate.now();
        Season season = dateManager.getSeason(now);
        DayType dayType = dateManager.getDayType(now);

        List<MainAccListQueryDto> accommodations = accommodationQueryRepository.getAreaAccommodations(season, dayType, memberId);

        return accommodations
                .stream()
                .collect(groupingBy(
                        MainAccListQueryDto::getAreaKey,
                        mapping(MainAccListResDto::from, toList())
                ))
                .entrySet()
                .stream()
                .map(entry -> new MainAccResDto(
                        entry.getKey().areaName(),
                        entry.getKey().areaCode(),
                        entry.getValue())
                )
                .toList();
    }

    public PageResponseDto<FilteredAccListResDto> getFilteredPagingAccommodations(AccSearchCondDto searchDto, Long memberId, Pageable pageable) {
        LocalDate now = LocalDate.now();
        Season season = dateManager.getSeason(now);
        DayType dayType = dateManager.getDayType(now);

        Page<FilteredAccListResDto> result = accommodationQueryRepository.getFilteredPagingAccommodations(searchDto, memberId, pageable, season, dayType);

        return PageResponseDto.<FilteredAccListResDto>builder()
                              .contents(result.getContent())
                              .pageNumber(pageable.getPageNumber())
                              .pageSize(pageable.getPageSize())
                              .total(result.getTotalElements())
                              .build();
    }

    @Transactional
    public DetailAccommodationResDto getDetailAccommodation(Long accId, Long memberId) {
        AccommodationCommonInfo commonInfo = cacheService.getAccCommonInfo(accId);

        WishlistInfo wishlistInfo = WishlistInfo.empty();
        List<ReservedDateQueryDto> reservedDates = reservationQueryRepository.findReservedDatesByAccommodationId(accId);

        if (memberId != null) {
            eventPublisher.publishEvent(new ViewHistoryEvent(accId, memberId));
            wishlistInfo = wishlistQueryRepository.getWishlistInfo(accId, memberId)
                                                  .orElse(WishlistInfo.empty());
        }

        return DetailAccommodationResDto.from(commonInfo, wishlistInfo, reservedDates);
    }

    public List<ViewHistoryResDto> getRecentViewAccommodations(Long memberId) {
        Map<Long, LocalDateTime> viewInfoMap = viewHistoryService.getRecentViewIdsWithTime(memberId);
        if (viewInfoMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> accIds = viewInfoMap.keySet().stream().toList();
        Map<Long, WishlistInfo> wishlistMap = wishlistQueryRepository.getWishlistInfos(accIds, memberId);

        List<ViewHistoryDto> historyDtos = accIds.stream()
                                                 .map(id -> {
                                                     AccommodationCommonInfo commonInfo = cacheService.getAccCommonInfo(id);
                                                     WishlistInfo wishInfo = wishlistMap.getOrDefault(id, WishlistInfo.empty());

                                                     return ViewHistoryDto.builder()
                                                                          .accommodationId(id)
                                                                          .viewDate(viewInfoMap.get(id))
                                                                          .title(commonInfo.getTitle())
                                                                          .avgRate(commonInfo.getAvgRate())
                                                                          .thumbnailUrl(commonInfo.getImages().getThumbnail())
                                                                          .isInWishlist(wishInfo.isInWishlist())
                                                                          .wishlistId(wishInfo.wishlistId())
                                                                          .wishlistName(wishInfo.wishlistName())
                                                                          .build();
                                                 })
                                                 .toList();

        return historyDtos.stream()
                          .collect(Collectors.groupingBy(
                                  dto -> dto.viewDate().toLocalDate(),
                                  LinkedHashMap::new,
                                  Collectors.toList()
                          ))
                          .entrySet()
                          .stream()
                          .map(e -> new ViewHistoryResDto(e.getKey(), e.getValue()))
                          .toList();
    }

    public AccommodationPriceResDto getAccommodationPrice(Long accId, LocalDate date) {
        Season season = dateManager.getSeason(date);
        DayType dayType = dateManager.getDayType(date);
        int price = accommodationQueryRepository.getAccommodationPrice(accId, season, dayType);

        return new AccommodationPriceResDto(accId, date, price);
    }
}
