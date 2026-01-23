package project.airbnb.clone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.consts.DayType;
import project.airbnb.clone.consts.Season;
import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo;
import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.DetailReviewDto;
import project.airbnb.clone.repository.dto.DetailAccommodationQueryDto;
import project.airbnb.clone.repository.dto.ImageDataQueryDto;
import project.airbnb.clone.repository.query.AccommodationQueryRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final DateManager dateManager;
    private final AccommodationQueryRepository accommodationQueryRepository;

//    @Cacheable(value = "accCommonInfo")
    public AccommodationCommonInfo getAccCommonInfo(Long accId) {
        LocalDate now = LocalDate.now();
        Season season = dateManager.getSeason(now);
        DayType dayType = dateManager.getDayType(now);

        DetailAccommodationQueryDto detail = accommodationQueryRepository.findAccommodation(accId, null, season, dayType)
                                                                         .orElseThrow(() -> AccommodationExceptions.notFoundById(accId));
        List<String> amenities = accommodationQueryRepository.findAmenities(accId);
        List<DetailReviewDto> reviews = accommodationQueryRepository.findReviews(accId);
        List<ImageDataQueryDto> images = accommodationQueryRepository.findImages(accId);

        return AccommodationCommonInfo.from(detail, amenities, reviews, images);
    }

    @CacheEvict(value = "accCommonInfo")
    public void evictAccCommonInfo(Long accId) { }
}
