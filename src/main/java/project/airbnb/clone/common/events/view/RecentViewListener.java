package project.airbnb.clone.common.events.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import project.airbnb.clone.service.accommodation.ViewHistoryService;

@Component
@RequiredArgsConstructor
public class RecentViewListener {

    private final ViewHistoryService viewHistoryService;

//    @Async("viewHistoryExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRecentViewEvent(ViewHistoryEvent event) {
        viewHistoryService.saveRecentView(event.accommodationId(), event.memberId());
    }
}
