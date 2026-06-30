package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.implementation.BillingPeriodClosureServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPeriodClosureWorker {

    private final BillingPeriodClosureServiceImpl closureService;

    /** Clôture journalière — chaque jour à 23h30 */
    @Scheduled(cron = "${bokati.billing.workers.daily-closure-cron:0 30 23 * * *}")
    public void runDailyClosure() {
        log.info("Running daily billing period closure");
        closureService.computeAndSave("DAY", LocalDate.now().minusDays(1), "SCHEDULER");
    }

    /** Clôture mensuelle — 1er de chaque mois à 00h15 */
    @Scheduled(cron = "${bokati.billing.workers.monthly-closure-cron:0 15 0 1 * *}")
    public void runMonthlyClosure() {
        log.info("Running monthly billing period closure");
        closureService.computeAndSave("MONTH", YearMonth.now().minusMonths(1).atDay(1), "SCHEDULER");
    }

    /** Clôture annuelle — 1er janvier à 01h00 */
    @Scheduled(cron = "${bokati.billing.workers.annual-closure-cron:0 0 1 1 1 *}")
    public void runAnnualClosure() {
        log.info("Running annual billing period closure");
        closureService.computeAndSave("YEAR",
                LocalDate.of(LocalDate.now().getYear() - 1, 1, 1), "SCHEDULER");
    }
}
