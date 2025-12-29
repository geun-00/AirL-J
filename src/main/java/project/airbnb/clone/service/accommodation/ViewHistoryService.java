package project.airbnb.clone.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.exceptions.factory.AccommodationExceptions;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.history.ViewHistory;
import project.airbnb.clone.repository.jpa.AccommodationRepository;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.jpa.ViewHistoryRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ViewHistoryService {

    private final MemberRepository memberRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final AccommodationRepository accommodationRepository;

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
}
