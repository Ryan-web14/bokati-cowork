package com.sni.bokaticowork.core.outbox.service.interfaces;

import com.sni.bokaticowork.core.outbox.model.OutboxEvent;

public interface OutboxEventProcessor {

    boolean supports(OutboxEvent event);

    void process(OutboxEvent event);
}
