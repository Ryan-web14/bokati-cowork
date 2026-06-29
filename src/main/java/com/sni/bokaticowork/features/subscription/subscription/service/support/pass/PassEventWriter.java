package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEvent;
import com.sni.bokaticowork.features.subscription.subscription.model.PassStatusHistory;
import com.sni.bokaticowork.features.subscription.repository.PassEventRepository;
import com.sni.bokaticowork.features.subscription.repository.PassStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassEventWriter {

    private final PassEventRepository eventRepository;
    private final PassStatusHistoryRepository historyRepository;

    public void writeEvent(Pass pass, PassEventType type, String payloadJson) {
        eventRepository.save(PassEvent.builder()
                .pass(pass)
                .eventType(type.name())
                .payloadJson(payloadJson)
                .build());
    }

    public void writeHistory(Pass pass, PassStatus from, PassStatus to,
                              String reason, String changedBy) {
        historyRepository.save(PassStatusHistory.builder()
                .pass(pass)
                .fromStatus(from == null ? null : from.name())
                .toStatus(to.name())
                .reason(reason)
                .changedBy(changedBy)
                .build());
    }
}
