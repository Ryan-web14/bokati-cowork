package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.booking.service.support.BookingCompanyAccounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Lot B9e : releves mensuels des entreprises. Passe toutes les 6 heures
 * ({@code bokati.booking.company-statements.worker-delay-ms}) et emet le releve du mois precedent
 * des entreprises qui ne l'ont pas encore ; un releve deja emis n'est jamais refait.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompanyStatementWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.booking.company-statements.worker-delay-ms";
    private static final long DEFAULT_DELAY_MS = 21_600_000L;

    private final BookingCompanyAccounts accounts;

    private SettingTriggers triggers;
    private ObjectProvider<BookingCompanyStatementWorker> self;

    public void process() {
        try {
            var done = accounts.runStatements(null);
            long issued = done.stream().filter(statement -> statement.items() > 0).count();
            if (issued > 0) {
                log.info("Releves mensuels des entreprises : {} emis", issued);
            }
        } catch (Exception ex) {
            log.error("BookingCompanyStatementWorker : {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BookingCompanyStatementWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
