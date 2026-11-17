package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashAnomalyFlagRepository;
import com.sni.bokaticowork.features.payment.repository.CashCountRepository;
import com.sni.bokaticowork.features.payment.repository.CashHandoverRepository;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ce qu'une session de caisse donne a voir (17/11/2026) : le detail de ses encaissements, sa
 * chronologie et ses indicateurs en direct. Lecture seule.
 *
 * <p>Cloture a l'aveugle respectee : tant que la session n'est pas comptee, le solde attendu n'est
 * montre qu'a qui peut decider d'un ecart ({@code CASH_VARIANCE:DECIDE}).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashSessionInsightService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final Set<CashSessionStatus> UNCOUNTED = EnumSet.of(CashSessionStatus.OPENING, CashSessionStatus.OPEN,
            CashSessionStatus.PAUSED, CashSessionStatus.SUSPENDED, CashSessionStatus.COUNTING, CashSessionStatus.RECOUNT);

    private final CashSessionRepository sessionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CashMovementRepository movementRepository;
    private final CashCountRepository countRepository;
    private final CashVarianceReviewRepository varianceRepository;
    private final CashAnomalyFlagRepository anomalyRepository;
    private final CashHandoverRepository handoverRepository;
    private final CashSessionSummarySupport summarySupport;
    private final TransactionContextResolver contextResolver;
    private final CashActorResolver actors;

    // ---- Encaissements ------------------------------------------------------------------------

    /** Une transaction rattachee a la session. */
    public record PaymentLine(String transactionNumber, String paymentMethod, String status, BigDecimal amount,
                              BigDecimal tenderedAmount, BigDecimal changeAmount, String receivedBy, Instant at,
                              String customerType, String customerCode, String customerName,
                              String sourceType, String sourceCode, String sourceLabel, String receiptNumber,
                              boolean refund) {
    }

    public record MethodTotal(String paymentMethod, long count, BigDecimal amount) {
    }

    public record HourTotal(int hour, long count, BigDecimal amount) {
    }

    public record SessionPayments(String sessionNumber, List<PaymentLine> lines, List<MethodTotal> byMethod,
                                  List<HourTotal> byHour, long paymentCount, BigDecimal paymentTotal,
                                  long refundCount, BigDecimal refundTotal) {
    }

    public SessionPayments payments(String sessionNumber, String paymentMethod, String receivedBy) {
        CashSession session = session(sessionNumber);
        List<PaymentLine> lines = transactionRepository.findAllByCashSessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCEEDED || t.getStatus() == PaymentTransactionStatus.REFUNDED
                        || t.getStatus() == PaymentTransactionStatus.REVERSED)
                .filter(t -> !StringUtils.hasText(paymentMethod) || (t.getPaymentMethod() != null
                        && t.getPaymentMethod().name().equalsIgnoreCase(paymentMethod.trim())))
                .filter(t -> !StringUtils.hasText(receivedBy) || receivedBy.trim().equalsIgnoreCase(t.getReceivedBy()))
                .map(this::line)
                .toList();
        List<PaymentLine> payments = lines.stream().filter(l -> !l.refund()).toList();
        List<PaymentLine> refunds = lines.stream().filter(PaymentLine::refund).toList();
        Map<String, List<PaymentLine>> byMethod = payments.stream()
                .collect(Collectors.groupingBy(PaymentLine::paymentMethod, LinkedHashMap::new, Collectors.toList()));
        Map<Integer, List<PaymentLine>> byHour = payments.stream()
                .filter(l -> l.at() != null)
                .collect(Collectors.groupingBy(l -> l.at().atZone(ZONE).getHour(), TreeMap::new, Collectors.toList()));
        return new SessionPayments(session.getSessionNumber(), lines,
                byMethod.entrySet().stream().map(e -> new MethodTotal(e.getKey(), e.getValue().size(), sum(e.getValue()))).toList(),
                byHour.entrySet().stream().map(e -> new HourTotal(e.getKey(), e.getValue().size(), sum(e.getValue()))).toList(),
                payments.size(), sum(payments), refunds.size(), sum(refunds));
    }

    private PaymentLine line(PaymentTransaction t) {
        PaymentIntent intent = t.getPaymentIntent();
        boolean refund = t.getStatus() != PaymentTransactionStatus.SUCCEEDED;
        String customerType = intent == null ? null : intent.getCustomerType();
        String customerCode = intent == null ? null : intent.getCustomerCode();
        String sourceType = intent == null ? null : intent.getSourceType();
        String sourceCode = intent == null ? null : intent.getSourceCode();
        String customerName = null;
        String sourceLabel = null;
        try {
            var party = contextResolver.resolveParty(customerType, customerCode);
            customerName = party == null ? null : party.name();
            var source = contextResolver.resolveSource(sourceType, sourceCode);
            sourceLabel = source == null ? null : source.label();
        } catch (RuntimeException ignored) {
            // libelles facultatifs
        }
        return new PaymentLine(t.getTransactionNumber(), t.getPaymentMethod() == null ? null : t.getPaymentMethod().name(),
                t.getStatus().name(), t.getAmount(), t.getTenderedAmount(), t.getChangeAmount(), t.getReceivedBy(),
                t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt(), customerType, customerCode, customerName,
                sourceType, sourceCode, sourceLabel, t.getReceiptNumber(), refund);
    }

    // ---- Chronologie --------------------------------------------------------------------------

    /** Un evenement de la session, horodate, avec son auteur. */
    public record TimelineEvent(Instant at, String kind, String label, String actor, BigDecimal amount, String reference) {
    }

    public List<TimelineEvent> timeline(String sessionNumber) {
        CashSession s = session(sessionNumber);
        boolean hidden = expectedHidden(s);
        List<TimelineEvent> events = new ArrayList<>();
        add(events, s.getOpenedAt(), "OPENED", "Ouverture de la session", s.getOpenedBy(), s.getOpeningAmount(), null);
        if (s.getOpeningVariance() != null && s.getOpeningVariance().signum() != 0) {
            add(events, s.getOpenedAt(), "OPENING_VARIANCE", "Écart à l'ouverture" + reason(s.getOpeningVarianceReason()),
                    s.getOpenedBy(), s.getOpeningVariance(), null);
        }
        if (s.getPausedAt() != null) {
            add(events, s.getPausedAt(), "PAUSED", "Mise en pause", s.getOpenedBy(), null, null);
        }
        if (s.getSuspendedAt() != null) {
            add(events, s.getSuspendedAt(), "SUSPENDED", "Suspension" + reason(s.getSuspensionReason()), s.getSuspendedBy(), null, null);
        }
        for (CashMovement m : movementRepository.findByCashSession_IdOrderByCreatedAtAsc(s.getId())) {
            String label = movementLabel(m);
            if (m.getStatus() == CashMovementStatus.CANCELLED) {
                label += " · annulé" + (m.getCancelledBy() == null ? "" : " par " + m.getCancelledBy()) + reason(m.getCancelReason());
            }
            add(events, m.getCreatedAt(), "MOVEMENT_" + m.getMovementType().name(), label, m.getCreatedBy(), m.getAmount(), m.getMovementNumber());
        }
        countRepository.findBySessionNumberOrderByCreatedAtAsc(s.getSessionNumber()).forEach(c ->
                add(events, c.getCreatedAt(), "COUNT", "Comptage " + (c.getMoment() == null ? "" : c.getMoment().name())
                                + (hidden || c.getVariance() == null ? "" : " · écart " + c.getVariance().stripTrailingZeros().toPlainString()),
                        c.getCountedBy(), c.getTotal(), c.getCountNumber()));
        varianceRepository.findBySessionNumberOrderByCreatedAtAsc(s.getSessionNumber()).forEach(v -> {
            add(events, v.getCreatedAt(), "VARIANCE", "Écart " + (v.getKind() == null ? "" : v.getKind().name()) + reason(v.getReason()),
                    v.getCashier(), v.getAmount(), v.getReviewNumber());
            if (v.getDecidedAt() != null) {
                add(events, v.getDecidedAt(), "VARIANCE_DECIDED", "Décision sur l'écart : " + v.getDecision() + reason(v.getDecisionNote()),
                        v.getDecidedBy(), v.getAmount(), v.getReviewNumber());
            }
        });
        anomalyRepository.findByCashSession_IdOrderByDetectedAtAsc(s.getId()).forEach(a ->
                add(events, a.getDetectedAt(), "ANOMALY", "Anomalie " + a.getAnomalyType() + " (" + a.getSeverity() + ")"
                        + reason(a.getDescription()), "SYSTEM", null, a.getFlagNumber()));
        handoverRepository.findByFromSessionOrToSessionOrderByCreatedAtAsc(s.getSessionNumber(), s.getSessionNumber()).forEach(h ->
                add(events, h.getCreatedAt(), "HANDOVER", "Passation de " + h.getOutgoing() + " à " + h.getIncoming()
                        + " (" + h.getStatus() + ")", h.getOutgoing(), h.getOutgoingAmount(), h.getHandoverNumber()));
        add(events, s.getClosingRequestedAt(), "CLOSING_REQUESTED", "Clôture demandée", s.getClosedBy(), null, null);
        add(events, s.getClosedAt(), "CLOSED", "Clôture" + (Boolean.TRUE.equals(s.getForcedClose()) ? " forcée" : ""),
                s.getClosedBy(), hidden ? null : s.getCountedClosingAmount(), null);
        add(events, s.getReviewedAt(), "REVIEWED", "Revue de la session", s.getReviewedBy(), null, null);
        events.sort(Comparator.comparing(TimelineEvent::at));
        return events;
    }

    private static void add(List<TimelineEvent> events, Instant at, String kind, String label, String actor, BigDecimal amount, String reference) {
        if (at != null) {
            events.add(new TimelineEvent(at, kind, label, actor, amount, reference));
        }
    }

    private static String movementLabel(CashMovement m) {
        String base = switch (m.getMovementType()) {
            case OPENING_FLOAT -> "Fonds de caisse";
            case PAYMENT -> "Encaissement";
            case REFUND -> "Remboursement";
            case CASH_IN -> "Entrée d'espèces";
            case CASH_OUT -> "Sortie d'espèces";
            case SAFE_DEPOSIT -> "Dépôt au coffre";
            case TRANSFER_IN -> "Transfert reçu";
            case TRANSFER_OUT -> "Transfert envoyé";
            case ADJUSTMENT -> "Ajustement";
            case CLOSING_COUNT -> "Comptage de clôture";
            case EXCHANGE -> "Échange de monnaie";
            case VARIANCE -> "Écart constaté";
        };
        String document = m.getDocumentNumber() == null ? "" : " " + m.getDocumentNumber();
        return base + document + reason(m.getReason());
    }

    private static String reason(String reason) {
        return StringUtils.hasText(reason) ? " · " + reason.trim() : "";
    }

    // ---- Indicateurs --------------------------------------------------------------------------

    public record Indicators(String sessionNumber, String status, BigDecimal expectedBalance, boolean expectedHidden,
                             long paymentCount, long customersServed, BigDecimal paymentTotal, BigDecimal averageTicket,
                             long refundCount, BigDecimal refundTotal, long movementCount, Instant lastMovementAt,
                             long activeMinutes, long pauseMinutes, BigDecimal detentionCap, BigDecimal detentionRemaining,
                             long openAnomalies) {
    }

    public Indicators indicators(String sessionNumber) {
        CashSession s = session(sessionNumber);
        boolean hidden = expectedHidden(s);
        BigDecimal expected = money(summarySupport.expectedClosingAmount(s));
        SessionPayments payments = payments(s.getSessionNumber(), null, null);
        long customers = payments.lines().stream().filter(l -> !l.refund())
                .map(l -> l.customerType() + ":" + l.customerCode()).filter(Objects::nonNull).distinct().count();
        BigDecimal average = payments.paymentCount() == 0 ? BigDecimal.ZERO
                : payments.paymentTotal().divide(BigDecimal.valueOf(payments.paymentCount()), 2, RoundingMode.HALF_UP);
        List<CashMovement> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(s.getId());
        Instant last = movements.isEmpty() ? null : movements.get(movements.size() - 1).getCreatedAt();
        Instant end = s.getClosedAt() != null ? s.getClosedAt() : Instant.now();
        long pause = (s.getPauseSeconds() == null ? 0 : s.getPauseSeconds())
                + (s.getPausedAt() != null && s.getStatus() == CashSessionStatus.PAUSED ? Duration.between(s.getPausedAt(), Instant.now()).getSeconds() : 0);
        long active = s.getOpenedAt() == null ? 0 : Math.max(0, Duration.between(s.getOpenedAt(), end).getSeconds() - pause) / 60;
        BigDecimal cap = s.getCashRegister() == null ? null : s.getCashRegister().getMaxCashAmount();
        BigDecimal remaining = cap == null || cap.signum() <= 0 || hidden ? null : cap.subtract(expected).max(BigDecimal.ZERO);
        long anomalies = anomalyRepository.findByCashSession_IdOrderByDetectedAtAsc(s.getId()).stream()
                .filter(a -> a.getReviewedAt() == null).count();
        return new Indicators(s.getSessionNumber(), s.getStatus().name(), hidden ? null : expected, hidden,
                payments.paymentCount(), customers, payments.paymentTotal(), average, payments.refundCount(), payments.refundTotal(),
                movements.size(), last, active, pause / 60, cap, remaining, anomalies);
    }

    // ---- Outils -------------------------------------------------------------------------------

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new ResourceNotFoundException("Session de caisse introuvable");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable"));
    }

    private boolean expectedHidden(CashSession session) {
        if (session.getCashRegister() == null || !Boolean.TRUE.equals(session.getCashRegister().getBlindClose())) {
            return false;
        }
        return UNCOUNTED.contains(session.getStatus()) && (actors == null || !actors.hasAuthority("CASH_VARIANCE:DECIDE"));
    }

    private static BigDecimal sum(List<PaymentLine> lines) {
        return lines.stream().map(PaymentLine::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static boolean isOutflow(CashMovementType type) {
        return type == CashMovementType.REFUND || type == CashMovementType.CASH_OUT || type == CashMovementType.TRANSFER_OUT
                || type == CashMovementType.SAFE_DEPOSIT;
    }
}
