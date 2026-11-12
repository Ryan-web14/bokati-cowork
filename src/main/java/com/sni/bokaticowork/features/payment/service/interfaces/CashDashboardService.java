package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.CashPositionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReportResponse;

import java.time.LocalDate;

/** Pilotage des caisses : position en temps reel et etats (lot C6 du plan finance). */
public interface CashDashboardService {

    CashPositionResponse position(String locationCode);

    /**
     * Un etat : {@code daily-blotter} (brouillard d'une caisse sur un jour), {@code sessions},
     * {@code variances}, {@code transfers}, {@code counts}, {@code outflows-by-nature},
     * {@code pending-documents}.
     */
    CashReportResponse report(String kind, String registerCode, LocalDate from, LocalDate to);

    String toCsv(CashReportResponse report);

    /** Alertes periodiques : caisses non comptees, comptages inopines en retard. */
    void runPeriodicChecks();
}
