package project.airbnb.clone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.consts.DayType;
import project.airbnb.clone.consts.Season;
import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo;
import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.DetailReviewDto;
import project.airbnb.clone.repository.dto.DetailAccommodationQueryDto;
import project.airbnb.clone.repository.dto.ImageDataQueryDto;
import project.airbnb.clone.repository.query.AccommodationQueryRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final DateManager dateManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AccommodationQueryRepository accommodationQueryRepository;

    public AccommodationCommonInfo getAccCommonInfo(Long accId) {
        String key = "accommodation:commonInfo:" + accId;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof AccommodationCommonInfo commonInfo) {
            return commonInfo;
        }

        LocalDate now = LocalDate.now();
        Season season = dateManager.getSeason(now);
        DayType dayType = dateManager.getDayType(now);

        DetailAccommodationQueryDto detail = accommodationQueryRepository.findAccommodation(accId, null, season, dayType)
                                                                         .orElseThrow(() -> AccommodationExceptions.notFoundById(accId));
        List<String> amenities = accommodationQueryRepository.findAmenities(accId);
        List<DetailReviewDto> reviews = accommodationQueryRepository.findReviews(accId);
        List<ImageDataQueryDto> images = accommodationQueryRepository.findImages(accId);

        AccommodationCommonInfo result = AccommodationCommonInfo.from(detail, amenities, reviews, images);

        long baseTtlMs = Duration.ofHours(1).toMillis();
        long jitterRange = Duration.ofMinutes(1).toMillis() + 1;

        long ttlMs = baseTtlMs + ThreadLocalRandom.current().nextLong(jitterRange);
        redisTemplate.opsForValue().set(key, result, ttlMs, TimeUnit.MILLISECONDS);

        return result;
    }

    public void evictAccCommonInfo(Long accId) {
        redisTemplate.delete("accommodation:commonInfo:" + accId);
    }
}
