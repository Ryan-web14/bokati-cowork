package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.features.support.enums.TicketEventType;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.SupportTicketEvent;
import com.sni.bokaticowork.features.support.repository.SupportTicketEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SupportTicketEventWriter {

    private final SupportTicketEventRepository repository;

    @Transactional
    public SupportTicketEvent write(SupportTicket ticket, TicketEventType type,
                                    String actorType, String actorId, String description) {
        return repository.save(SupportTicketEvent.builder()
                .ticket(ticket)
                .eventType(type)
                .actorType(actorType)
                .actorId(actorId)
                .description(description)
                .build());
    }

    @Transactional
    public SupportTicketEvent writeSystem(SupportTicket ticket, TicketEventType type, String description) {
        return write(ticket, type, "SYSTEM", null, description);
    }
}
