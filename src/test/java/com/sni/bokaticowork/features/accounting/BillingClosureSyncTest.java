package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.accounting.BillingPeriodClosedEvent;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.repository.AccPeriodRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.BillingClosureSync;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Complements F12 : clore un mois de facturation propose (ou fait) le verrouillage de la periode comptable. */
class BillingClosureSyncTest {

    private final AccountingSettings settings = mock(AccountingSettings.class);
    private final AccPeriodRepository periods = mock(AccPeriodRepository.class);
    private final AccountingPeriodService periodService = mock(AccountingPeriodService.class);
    private final AccountingNotifier notifier = mock(AccountingNotifier.class);
    private final BillingClosureSync sync = new BillingClosureSync(settings, periods, periodService, notifier);
    private final BillingPeriodClosedEvent october = new BillingPeriodClosedEvent("MONTH", "2026-10", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

    @BeforeEach
    void setUp() {
        when(settings.enabled()).thenReturn(true);
        when(periods.findByCode("2026-10")).thenReturn(Optional.of(AccPeriod.builder().code("2026-10").status(PeriodStatus.OPEN).build()));
    }

    @Test
    void proposesByDefault() {
        when(settings.text(eq("accounting.period.billing-closure-sync"), anyString())).thenReturn("PROPOSE");
        sync.onBillingPeriodClosed(october);
        verify(notifier).notifyAccountants(contains("Verrouiller la periode comptable 2026-10"), anyString(), eq("2026-10"), anyString(), any());
        verify(periodService, never()).lock(anyString());
    }

    @Test
    void locksWhenAsked() {
        when(settings.text(eq("accounting.period.billing-closure-sync"), anyString())).thenReturn("LOCK");
        sync.onBillingPeriodClosed(october);
        verify(periodService).lock("2026-10");
    }

    @Test
    void ignoresDaysAndDisabledSync() {
        when(settings.text(eq("accounting.period.billing-closure-sync"), anyString())).thenReturn("NONE");
        sync.onBillingPeriodClosed(october);
        sync.onBillingPeriodClosed(new BillingPeriodClosedEvent("DAY", "2026-10-31", LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31)));
        verify(notifier, never()).notifyAccountants(anyString(), anyString(), anyString(), anyString(), any());
        verify(periodService, never()).lock(anyString());
    }
}
