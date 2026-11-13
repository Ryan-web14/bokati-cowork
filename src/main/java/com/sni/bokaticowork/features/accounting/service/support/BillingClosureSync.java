package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.core.accounting.BillingPeriodClosedEvent;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.repository.AccPeriodRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;
import java.util.Optional;

/**
 * Synchronisation avec la cloture de facturation (plan, section 4.2) : clore un mois de facturation
 * propose de verrouiller la periode comptable du mois, sans l'imposer. Parametre
 * {@code accounting.period.billing-closure-sync} : {@code PROPOSE} (defaut, notification aux
 * comptables), {@code LOCK} (verrouillage, et notification du motif s'il est refuse) ou {@code NONE}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingClosureSync {

    private final AccountingSettings settings;
    private final AccPeriodRepository periods;
    private final AccountingPeriodService periodService;
    private final AccountingNotifier notifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBillingPeriodClosed(BillingPeriodClosedEvent event) {
        if (!"MONTH".equals(event.periodType()) || !settings.enabled()) {
            return;
        }
        String mode = settings.text("accounting.period.billing-closure-sync", "PROPOSE").trim().toUpperCase(Locale.ROOT);
        if ("NONE".equals(mode)) {
            return;
        }
        Optional<AccPeriod> period = periods.findByCode(event.periodLabel());
        if (period.isEmpty() || period.get().getStatus() != PeriodStatus.OPEN) {
            return;
        }
        String code = period.get().getCode();
        if ("LOCK".equals(mode)) {
            try {
                periodService.lock(code);
                notifier.notifyAccountants("Periode comptable " + code + " verrouillee",
                        "La periode de facturation " + code + " est close : la periode comptable " + code
                                + " a ete verrouillee. Seule la saisie comptable y est encore admise.", code, "/accounting/periods", null);
                return;
            } catch (RuntimeException exception) {
                notifier.notifyAccountants("Periode comptable " + code + " non verrouillee",
                        "La periode de facturation " + code + " est close, mais la periode comptable n'a pas pu etre verrouillee : "
                                + exception.getMessage(), code, "/accounting/periods", null);
                return;
            }
        }
        notifier.notifyAccountants("Verrouiller la periode comptable " + code + " ?",
                "La periode de facturation " + code + " est close (totaux scelles). Vous pouvez verrouiller la periode comptable " + code
                        + " : les ecritures automatiques iront alors sur la periode ouverte suivante.", code, "/accounting/periods", null);
    }
}
