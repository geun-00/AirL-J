package project.airbnb.clone.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.events.view.ViewHistoryEvent;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.consts.DayType;
import project.airbnb.clone.consts.Season;
import project.airbnb.clone.dto.PageResponseDto;
import project.airbnb.clone.dto.accommodation.*;
import project.airbnb.clone.dto.accommodation.DetailAccommodationResDto.DetailImageDto;
import project.airbnb.clone.dto.accommodation.DetailAccommodationResDto.DetailReviewDto;
import project.airbnb.clone.repository.dto.DetailAccommodationQueryDto;
import project.airbnb.clone.repository.dto.ImageDataQueryDto;
import project.airbnb.clone.repository.dto.MainAccListQueryDto;
import project.airbnb.clone.repository.query.AccommodationQueryRepository;
import project.airbnb.clone.service.DateManager;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationService {

    private final DateManager dateManager;
    private final ApplicationEventPublisher eventPublisher;
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

    public DetailAccommodationResDto getDetailAccommodation(Long accId, Long memberId) {
        LocalDate now = LocalDate.now();
        Season season = dateManager.getSeason(now);
        DayType dayType = dateManager.getDayType(now);

        DetailAccommodationQueryDto detailAccQueryDto = accommodationQueryRepository.findAccommodation(accId, memberId, season, dayType)
                                                                                    .orElseThrow(() -> AccommodationExceptions.notFoundById(accId));
        List<ImageDataQueryDto> images = accommodationQueryRepository.findImages(accId);
        List<String> amenities = accommodationQueryRepository.findAmenities(accId);
        List<DetailReviewDto> reviews = accommodationQueryRepository.findReviews(accId);

        if (memberId != null) {
            eventPublisher.publishEvent(new ViewHistoryEvent(accId, memberId));
        }

        String thumbnail = images.stream()
                                 .filter(ImageDataQueryDto::isThumbnail)
                                 .map(ImageDataQueryDto::imageUrl)
                                 .findFirst()
                                 .orElse(null);
        List<String> others = images.stream()
                                    .filter(dto -> !dto.isThumbnail())
                                    .map(ImageDataQueryDto::imageUrl)
                                    .toList();
        DetailImageDto detailImageDto = new DetailImageDto(thumbnail, others);

        return DetailAccommodationResDto.from(detailAccQueryDto, detailImageDto, amenities, reviews);
    }

    public List<ViewHistoryResDto> getRecentViewAccommodations(Long memberId) {
        return accommodationQueryRepository.findViewHistories(memberId)
                                           .stream()
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
