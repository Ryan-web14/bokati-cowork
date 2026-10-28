package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.async.ScheduledJobRuns;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.utils.phone.PhoneNumbers;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chaque matin, nos paiements mobile money des deux derniers jours relus chez PawaPay.
 *
 * <p>Le rapprochement existait dans l'outil d'administration, mais il fallait penser a le lancer.
 * Un rappel perdu, un depot reste en attente alors que PawaPay l'a encaisse, et rien ne le disait
 * avant que le client reclame. Desormais :</p>
 * <ul>
 *   <li>ce qu'une relecture suffit a regler l'est aussitot (rappel perdu, echec non recu) ;</li>
 *   <li>le reste · montant different, paiement que PawaPay ne confirme pas, depot sans reponse ·
 *       part dans un rapport aux destinataires des alertes, avec l'action a mener ;</li>
 *   <li>un jour sans ecart ne produit aucun courriel, seulement une ligne de journal.</li>
 * </ul>
 *
 * <p>Deux jours plutot qu'un : un depot de 23 h 50 n'est souvent tranche que le lendemain. Un seul
 * dyno fait le passage du jour, voir {@link ScheduledJobRuns}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class MobileMoneyDailyReconciliation {

    static final String JOB = "MOBILE_MONEY_DAILY_RECONCILIATION";
    static final Duration WINDOW = Duration.ofHours(48);
    static final int MAX_LINES_IN_EMAIL = 50;
    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private static final Set<String> STILL_OPEN = Set.of("PROCESSING", "SUBMITTED_UNCONFIRMED", "CREATED", "ACCEPTED", "UNRESOLVED");

    private final MobileMoneyOperationsService operations;
    private final MobileMoneySupervisionService supervision;
    private final MobileMoneyAlertRecipients alertRecipients;
    private final OutboxService outboxService;
    private final ScheduledJobRuns jobRuns;

    /** Ce que le passage a donne · pour le journal, le rapport et les tests. */
    public record Report(int checked, boolean truncated, List<String> autoSettled,
                         List<MobileMoneyOperationsService.ReconciliationLine> remaining) {
    }

    @Scheduled(cron = "${bokati.payment.pawaypay.reconciliation-cron:0 0 6 * * *}", zone = "Africa/Brazzaville")
    public void runDaily() {
        String runKey = LocalDate.now(ZONE).toString();
        if (!jobRuns.claim(JOB, runKey)) {
            return;
        }
        try {
            Report report = reconcile(Instant.now());
            String summary = report.checked() + " depot(s) relu(s), " + report.autoSettled().size()
                    + " regle(s) par relecture, " + report.remaining().size() + " ecart(s) restant(s)";
            log.info("Rapprochement mobile money du {} · {}", runKey, summary);
            jobRuns.finish(JOB, runKey, summary);
        } catch (Exception ex) {
            log.error("Rapprochement mobile money du {} en echec : {}", runKey, ex.getMessage(), ex);
            jobRuns.finish(JOB, runKey, "Echec : " + ex.getMessage());
        }
    }

    Report reconcile(Instant now) {
        Instant from = now.minus(WINDOW);
        MobileMoneyOperationsService.Reconciliation first = operations.reconcile(from, now);

        List<String> autoSettled = new ArrayList<>();
        List<MobileMoneyOperationsService.ReconciliationLine> remaining = new ArrayList<>();
        for (MobileMoneyOperationsService.ReconciliationLine line : first.discrepancies()) {
            if (resolvedByRecheck(line)) {
                autoSettled.add(line.deposit().depositId());
            } else {
                remaining.add(line);
            }
        }

        Report report = new Report(first.checked(), first.truncated(), autoSettled, remaining);
        if (!autoSettled.isEmpty() || !remaining.isEmpty() || first.truncated()) {
            publish(report, from, now);
        }
        return report;
    }

    /**
     * Une relecture regle-t-elle l'ecart · seulement si aucun ecart n'est critique et que l'action
     * proposee commence par une relecture.
     *
     * <p>Un ecart critique (montant, devise, paiement enregistre que PawaPay ne confirme pas) ne se
     * regle pas tout seul · il faut quelqu'un, et il va dans le rapport.</p>
     */
    private boolean resolvedByRecheck(MobileMoneyOperationsService.ReconciliationLine line) {
        boolean critical = line.discrepancies().stream().anyMatch(d -> "CRITICAL".equals(d.severity()));
        boolean syncable = line.discrepancies().stream()
                .anyMatch(d -> d.suggestedAction() != null && d.suggestedAction().startsWith("SYNC"));
        if (critical || !syncable) {
            return false;
        }
        try {
            MobileMoneyDepositResponse after = supervision.recheck(line.deposit().depositId());
            return after != null && !STILL_OPEN.contains(after.status());
        } catch (Exception ex) {
            log.warn("Rapprochement · relecture du depot {} impossible : {}", line.deposit().depositId(), ex.getMessage());
            return false;
        }
    }

    private void publish(Report report, Instant from, Instant to) {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (MobileMoneyOperationsService.ReconciliationLine line : report.remaining()) {
            if (lines.size() >= MAX_LINES_IN_EMAIL) {
                break;
            }
            MobileMoneyDepositResponse deposit = line.deposit();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("depositId", deposit.depositId());
            row.put("intentNumber", deposit.intentNumber());
            row.put("status", deposit.status());
            row.put("amount", deposit.amount() == null ? "" : deposit.amount().stripTrailingZeros().toPlainString());
            row.put("currency", deposit.currency());
            row.put("phoneNumber", deposit.payer() == null || deposit.payer().accountDetails() == null
                    ? null : PhoneNumbers.mask(deposit.payer().accountDetails().phoneNumber()));
            MobileMoneyOperationsService.Discrepancy worst = line.discrepancies().stream()
                    .filter(d -> "CRITICAL".equals(d.severity())).findFirst()
                    .orElse(line.discrepancies().get(0));
            row.put("severity", worst.severity());
            row.put("message", worst.message());
            row.put("action", worst.suggestedAction());
            lines.add(row);
        }
        String day = LocalDate.ofInstant(to, ZONE).toString();
        for (String recipient : alertRecipients.recipients()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("adminEmail", recipient);
            payload.put("templateCode", JOB);
            payload.put("day", day);
            payload.put("from", from.toString());
            payload.put("to", to.toString());
            payload.put("checked", report.checked());
            payload.put("truncated", report.truncated());
            payload.put("autoSettledCount", report.autoSettled().size());
            payload.put("remainingCount", report.remaining().size());
            payload.put("lines", lines);
            payload.put("moreLines", Math.max(0, report.remaining().size() - lines.size()));
            outboxService.publish(JOB, "PAYMENT", JOB + ":" + day + ":" + recipient, payload);
        }
    }
}
