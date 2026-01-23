package project.airbnb.clone.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.history.ViewHistory;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.repository.jpa.AccommodationRepository;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.jpa.ViewHistoryRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ViewHistoryService {

    private final MemberRepository memberRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final AccommodationRepository accommodationRepository;

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "member:history:";
    private static final int MAX_HISTORY_COUNT = 50;
    private static final long EXPIRE_DAYS = 30;

    @Transactional
    public void saveRecentView(Long accommodationId, Long memberId) {
        int updated = viewHistoryRepository.updateViewedAt(accommodationId, memberId, LocalDateTime.now());

        if (updated == 0) {
            Accommodation accommodation = accommodationRepository.findById(accommodationId)
                                                                 .orElseThrow(() -> AccommodationExceptions.notFoundById(accommodationId));
            Member member = memberRepository.findById(memberId)
                                            .orElseThrow(() -> MemberExceptions.notFoundById(memberId));

            viewHistoryRepository.save(ViewHistory.ofNow(accommodation, member));
        }
    }

    public void addHistory(Long memberId, Long accommodationId) {
        String key = KEY_PREFIX + memberId;
        double now = (double) System.currentTimeMillis();

        updateRecentView(key, accommodationId, now);
        removeExpiredHistory(key, now);
        limitHistorySize(key);
        refreshKeyExpiration(key);
    }

    private void updateRecentView(String key, Long accommodationId, double score) {
        redisTemplate.opsForZSet().add(key, accommodationId.toString(), score);
    }

    private void removeExpiredHistory(String key, double now) {
        long thirtyDaysAgo = (long) now - Duration.ofDays(EXPIRE_DAYS).toMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, thirtyDaysAgo);
    }

    private void limitHistorySize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > MAX_HISTORY_COUNT) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY_COUNT - 1);
        }
    }

    private void refreshKeyExpiration(String key) {
        redisTemplate.expire(key, Duration.ofDays(EXPIRE_DAYS));
    }

    public Map<Long, LocalDateTime> getRecentViewIdsWithTime(Long memberId) {
        String key = KEY_PREFIX + memberId;

        Set<TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (typedTuples == null || typedTuples.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, LocalDateTime> result = new LinkedHashMap<>();

        for (TypedTuple<String> tuple : typedTuples) {
            Long id = Long.valueOf(tuple.getValue());

            LocalDateTime time = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(tuple.getScore().longValue()), ZoneId.systemDefault());
            result.put(id, time);
        }
        return result;
    }
}
