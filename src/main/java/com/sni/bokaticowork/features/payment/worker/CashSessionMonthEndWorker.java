package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.core.async.ScheduledJobRuns;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Fin de mois des caisses · une session restee ouverte d'un mois sur l'autre est close par le
 * systeme et remplacee, avec son rapport envoye a l'administration, a la gerance et au responsable.
 *
 * <p>Passe chaque nuit (00:05 par defaut) et clot toute session ouverte avant le premier du mois
 * en cours : le 1er au matin, celles du mois ecoule ; un passage manque (serveur arrete) est
 * rattrape au suivant. Une session a la fois, chacune dans sa propre transaction · un echec
 * n'arrete pas les autres. Un seul passage par jour, meme a plusieurs instances.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashSessionMonthEndWorker {

    static final String JOB = "CASH_SESSION_MONTH_END";
    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final CashSessionRepository sessionRepository;
    private final CashRegisterService cashRegisterService;
    private final ScheduledJobRuns jobRuns;

    @Scheduled(cron = "${bokati.payment.cash-session.month-end-cron:0 5 0 * * *}", zone = "Africa/Brazzaville")
    public void closeLeftOpenSessions() {
        String runKey = LocalDate.now(ZONE).toString();
        if (!jobRuns.claim(JOB, runKey)) {
            return;
        }
        int closed = 0;
        int failed = 0;
        try {
            Instant cutoff = LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
            List<String> sessions = sessionRepository.findOpenSessionNumbersOpenedBefore(cutoff);
            for (String sessionNumber : sessions) {
                try {
                    var next = cashRegisterService.closeForMonthEnd(sessionNumber);
                    closed++;
                    log.info("Caisse · session {} close en fin de mois, remplacee par {}", sessionNumber, next.sessionNumber());
                } catch (RuntimeException ex) {
                    failed++;
                    log.error("Caisse · session {} non close en fin de mois : {}", sessionNumber, ex.getMessage(), ex);
                }
            }
            jobRuns.finish(JOB, runKey, closed + " session(s) close(s) et renouvelee(s), " + failed + " en echec");
        } catch (RuntimeException ex) {
            jobRuns.finish(JOB, runKey, "Echec : " + ex.getMessage());
            throw ex;
        }
    }
}
