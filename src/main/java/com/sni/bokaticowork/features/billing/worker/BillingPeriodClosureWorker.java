package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.implementation.BillingPeriodClosureServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPeriodClosureWorker implements SchedulingConfigurer {

    private static final String DAILY_CRON_KEY = "bokati.billing.workers.daily-closure-cron";
    private static final String DEFAULT_DAILY_CRON = "0 30 23 * * *";
    private static final String MONTHLY_CRON_KEY = "bokati.billing.workers.monthly-closure-cron";
    private static final String DEFAULT_MONTHLY_CRON = "0 15 0 1 * *";
    private static final String ANNUAL_CRON_KEY = "bokati.billing.workers.annual-closure-cron";
    private static final String DEFAULT_ANNUAL_CRON = "0 0 1 1 1 *";

    private SettingTriggers triggers;
    private ObjectProvider<BillingPeriodClosureWorker> self;

    private final BillingPeriodClosureServiceImpl closureService;

    /** Clôture journalière · chaque jour à 23h30 */
    public void runDailyClosure() {
        log.info("Running daily billing period closure");
        closureService.computeAndSave("DAY", LocalDate.now().minusDays(1), "SCHEDULER");
    }

    /** Clôture mensuelle · 1er de chaque mois à 00h15 */
    public void runMonthlyClosure() {
        log.info("Running monthly billing period closure");
        closureService.computeAndSave("MONTH", YearMonth.now().minusMonths(1).atDay(1), "SCHEDULER");
    }

    /** Clôture annuelle · 1er janvier à 01h00 */
    public void runAnnualClosure() {
        log.info("Running annual billing period closure");
        closureService.computeAndSave("YEAR",
                LocalDate.of(LocalDate.now().getYear() - 1, 1, 1), "SCHEDULER");
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BillingPeriodClosureWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().runDailyClosure(), triggers.cron(DAILY_CRON_KEY, DEFAULT_DAILY_CRON));
        registrar.addTriggerTask(() -> self.getObject().runMonthlyClosure(), triggers.cron(MONTHLY_CRON_KEY, DEFAULT_MONTHLY_CRON));
        registrar.addTriggerTask(() -> self.getObject().runAnnualClosure(), triggers.cron(ANNUAL_CRON_KEY, DEFAULT_ANNUAL_CRON));
    }
}
