package project.airbnb.clone.common.events.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import project.airbnb.clone.service.accommodation.ViewHistoryService;

@Component
@RequiredArgsConstructor
public class RecentViewListener {

    private final ViewHistoryService viewHistoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRecentViewEvent(ViewHistoryEvent event) {
//        viewHistoryService.addHistory(event.memberId(), event.accommodationId());
        viewHistoryService.saveRecentView(event.accommodationId(), event.memberId());
    }
}
