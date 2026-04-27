package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduleOverdueWorker {

    private final PaymentScheduleService paymentScheduleService;

    @Scheduled(fixedDelayString = "${bokati.billing.workers.schedule-overdue-delay-ms:3600000}")
    public void markOverdueInstallments() {
        int marked = paymentScheduleService.markOverdueInstallments();
        if (marked > 0) {
            log.info("Marked {} payment schedule installments as overdue", marked);
        }
    }
}