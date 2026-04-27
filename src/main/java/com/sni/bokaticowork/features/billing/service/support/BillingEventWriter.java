package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BillingEventWriter {

    private final OutboxService outboxService;

    public void write(BillingDocument document, String eventType, Map<String, Object> details) {
        outboxService.publish(
                eventType,
                "BILLING_DOCUMENT",
                document.getDocumentNumber(),
                Map.of(
                        "documentNumber", document.getDocumentNumber(),
                        "documentType", document.getDocumentType().name(),
                        "status", document.getStatus().name(),
                        "customerType", document.getCustomerType(),
                        "customerCode", document.getCustomerCode(),
                        "details", details == null ? Map.of() : details
                )
        );
    }
}
