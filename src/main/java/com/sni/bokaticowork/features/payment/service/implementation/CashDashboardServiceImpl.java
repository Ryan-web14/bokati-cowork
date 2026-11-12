package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.response.CashPositionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReportResponse;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashHandoverStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashResponsibilityTransferStatus;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashTransferStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashTransferOrder;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.*;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDashboardService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashFlowRules;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Pilotage des caisses (lot C6) : position de tresorerie en temps reel, etats et alertes de
 * comptage (caisse non comptee, comptage inopine en retard).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CashDashboardServiceImpl implements CashDashboardService {

    private static final String DECIDE = "CASH_VARIANCE:DECIDE";

    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final CashTransferOrderRepository transferRepository;
    private final CashVarianceReviewRepository reviewRepository;
    private final CashCountRepository countRepository;
    private final CashHandoverRepository handoverRepository;
    private final CashResponsibilityTransferRepository responsibilityRepository;
    private final CashOperationNatureRepository natureRepository;
    private final CashSessionSummarySupport summarySupport;
    private final CashActorResolver actors;
    private final CashEmailNotifier emailNotifier;

    // ------------------------------------------------------------------ position

    @Override
    public CashPositionResponse position(String locationCode) {
        Instant now = Instant.now();
        boolean canSeeExpected = actors.hasAuthority(DECIDE);
        List<CashTransferOrder> inTransit = transferRepository.findByStatusOrderBySentAtAsc(CashTransferStatus.IN_TRANSIT);
        List<CashVarianceReview> pendingReviews = reviewRepository.findByStatusOrderByCreatedAtAsc(CashVarianceStatus.PENDING);
        List<CashPositionResponse.RegisterPosition> positions = new ArrayList<>();
        for (CashRegister register : registerRepository.findAll()) {
            if (register.getStatus() == CashRegisterStatus.CLOSED) {
                continue;
            }
            if (StringUtils.hasText(locationCode) && !locationCode.trim().equals(register.getLocationCode())) {
                continue;
            }
            positions.add(position(register, now, canSeeExpected, inTransit, pendingReviews));
        }
        positions.sort(Comparator.comparing((CashPositionResponse.RegisterPosition position) -> String.valueOf(position.locationCode()))
                .thenComparing(CashPositionResponse.RegisterPosition::name));
        Map<String, List<CashPositionResponse.RegisterPosition>> bySite = new TreeMap<>();
        positions.forEach(position -> bySite.computeIfAbsent(position.locationCode() == null ? "-" : position.locationCode(),
                key -> new ArrayList<>()).add(position));
        List<CashPositionResponse.SiteTotal> sites = bySite.entrySet().stream()
                .map(entry -> total(entry.getKey(), entry.getValue())).toList();
        return new CashPositionResponse(now, positions, sites, total(null, positions));
    }

    private CashPositionResponse.RegisterPosition position(CashRegister register, Instant now, boolean canSeeExpected,
                                                           List<CashTransferOrder> inTransit, List<CashVarianceReview> pendingReviews) {
        Optional<CashSession> session = register.getId() == null ? Optional.empty()
                : sessionRepository.findBlockingSessionByCashRegisterId(register.getId());
        boolean hidden = Boolean.TRUE.equals(register.getBlindClose()) && !canSeeExpected;
        BigDecimal expected = session.map(live -> money(summarySupport.expectedClosingAmount(live)))
                .orElse(register.getLastCountAmount());
        List<String> alerts = new ArrayList<>();
        Long daysSinceCount = register.getLastCountAt() == null ? null : ChronoUnit.DAYS.between(register.getLastCountAt(), now);
        boolean automatic = register.getRegisterType() == CashRegisterType.AUTOMATIC || Boolean.TRUE.equals(register.getSystemManaged());
        if (!automatic) {
            if (register.getLastCountAt() == null) {
                alerts.add("Jamais comptee");
            } else if (register.getNotCountedAlertDays() != null && daysSinceCount != null && daysSinceCount >= register.getNotCountedAlertDays()) {
                alerts.add("Non comptee depuis " + daysSinceCount + " jours");
            }
        }
        BigDecimal occupancy = null;
        if (register.getMaxCashAmount() != null && register.getMaxCashAmount().signum() > 0 && expected != null) {
            occupancy = expected.multiply(BigDecimal.valueOf(100)).divide(register.getMaxCashAmount(), 1, RoundingMode.HALF_UP);
            if (occupancy.compareTo(BigDecimal.valueOf(100)) > 0) {
                alerts.add("Plafond de detention depasse");
            } else if (occupancy.compareTo(BigDecimal.valueOf(90)) >= 0) {
                alerts.add("Plafond de detention presque atteint");
            }
        }
        List<CashTransferOrder> incoming = inTransit.stream().filter(order -> order.getToRegister().equals(register.getRegisterCode())).toList();
        List<CashTransferOrder> outgoing = inTransit.stream().filter(order -> order.getFromRegister().equals(register.getRegisterCode())).toList();
        incoming.stream().filter(order -> order.getSentAt().plus(Duration.ofHours(2)).isBefore(now)).findFirst()
                .ifPresent(order -> alerts.add("Transfert " + order.getTransferNumber() + " en transit depuis plus de 2 h"));
        int reviews = (int) pendingReviews.stream().filter(review -> register.getRegisterCode().equals(review.getRegisterCode())).count();
        int visas = session.map(live -> movementRepository.findByCashSession_IdAndStatus(live.getId(), CashMovementStatus.PENDING).size()).orElse(0);
        Instant lastSpotCheck = automatic ? null : countRepository.findByRegisterCodeOrderByCreatedAtDesc(register.getRegisterCode()).stream()
                .filter(count -> count.getMoment() == CashCountMoment.SPOT_CHECK)
                .map(CashCount::getCreatedAt).findFirst().orElse(null);
        boolean spotOverdue = !automatic && register.getSpotCheckEveryDays() != null && register.getSpotCheckEveryDays() > 0
                && (lastSpotCheck == null ? register.getCreatedAt() != null
                && register.getCreatedAt().plus(Duration.ofDays(register.getSpotCheckEveryDays())).isBefore(now)
                : lastSpotCheck.plus(Duration.ofDays(register.getSpotCheckEveryDays())).isBefore(now));
        if (spotOverdue) {
            alerts.add("Comptage inopine en retard");
        }
        session.filter(live -> live.getStatus() == CashSessionStatus.SUSPENDED).ifPresent(live -> alerts.add("Session suspendue"));
        return new CashPositionResponse.RegisterPosition(register.getRegisterCode(), register.getName(),
                register.getRegisterType() == null ? null : register.getRegisterType().name(), register.getStatus() == null ? null : register.getStatus().name(),
                register.getLocationCode(), session.map(CashSession::getSessionNumber).orElse(null),
                session.map(live -> live.getStatus().name()).orElse(null), session.map(CashSession::getOpenedBy).orElse(null),
                hidden ? null : expected, hidden, register.getLastCountAmount(), register.getLastCountAt(), daysSinceCount,
                register.getMaxCashAmount(), hidden ? null : occupancy, incoming.size(), sum(incoming), outgoing.size(), sum(outgoing),
                reviews, visas, lastSpotCheck, spotOverdue, alerts);
    }

    private static CashPositionResponse.SiteTotal total(String site, List<CashPositionResponse.RegisterPosition> positions) {
        return new CashPositionResponse.SiteTotal(site, positions.size(),
                (int) positions.stream().filter(position -> position.sessionNumber() != null).count(),
                positions.stream().map(CashPositionResponse.RegisterPosition::expectedBalance).filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                positions.stream().map(CashPositionResponse.RegisterPosition::lastCountAmount).filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                positions.stream().map(CashPositionResponse.RegisterPosition::transfersOutAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                positions.stream().mapToInt(CashPositionResponse.RegisterPosition::pendingReviews).sum(),
                positions.stream().mapToInt(position -> position.alerts().size()).sum());
    }

    // ------------------------------------------------------------------ etats

    @Override
    public CashReportResponse report(String kind, String registerCode, LocalDate from, LocalDate to) {
        String code = StringUtils.hasText(registerCode) ? registerCode.trim() : null;
        LocalDate today = LocalDate.now(CashRegisterAccessGuard.ZONE);
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("La fin de la periode precede son debut");
        }
        Instant fromInstant = start.atStartOfDay(CashRegisterAccessGuard.ZONE).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(CashRegisterAccessGuard.ZONE).toInstant();
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("registerCode", code);
        parameters.put("from", start);
        parameters.put("to", end);
        String resolved = kind == null ? "" : kind.trim().toLowerCase();
        return switch (resolved) {
            case "daily-blotter" -> dailyBlotter(code, start, fromInstant, toInstant, parameters);
            case "sessions" -> sessions(code, fromInstant, toInstant, parameters);
            case "variances" -> variances(code, fromInstant, toInstant, parameters);
            case "transfers" -> transfers(code, fromInstant, toInstant, parameters);
            case "counts" -> counts(code, parameters);
            case "outflows-by-nature" -> outflowsByNature(code, fromInstant, toInstant, parameters);
            case "pending-documents" -> pendingDocuments(parameters);
            default -> throw new BadRequestException("Etat inconnu : " + kind + " (daily-blotter, sessions, variances, transfers, counts,"
                    + " outflows-by-nature, pending-documents)");
        };
    }

    /** Brouillard de caisse journalier : tous les mouvements d'une caisse sur un jour, soldes de debut et de fin. */
    private CashReportResponse dailyBlotter(String code, LocalDate day, Instant from, Instant to, Map<String, Object> parameters) {
        if (code == null) {
            throw new BadRequestException("Le brouillard se tire pour une caisse : precisez registerCode");
        }
        registerRepository.findByRegisterCode(code).orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + code));
        List<CashMovement> movements = movementRepository.findByRegisterBetween(code, from, to);
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal opening = movements.stream().filter(movement -> movement.getMovementType() == com.sni.bokaticowork.features.payment.enums.CashMovementType.OPENING_FLOAT)
                .map(CashMovement::getAmount).findFirst().orElse(BigDecimal.ZERO);
        BigDecimal balance = opening;
        for (CashMovement movement : movements) {
            BigDecimal signed = CashFlowRules.signedAmount(movement);
            if (movement.getMovementType() != com.sni.bokaticowork.features.payment.enums.CashMovementType.OPENING_FLOAT) {
                balance = balance.add(signed);
                if (signed.signum() > 0) {
                    in = in.add(signed);
                } else {
                    out = out.add(signed.negate());
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("heure", movement.getCreatedAt());
            row.put("numero", movement.getMovementNumber());
            row.put("session", movement.getCashSession().getSessionNumber());
            row.put("type", movement.getMovementType().name());
            row.put("nature", movement.getNatureCode() == null ? movement.getFlowCategory() : movement.getNatureCode());
            row.put("piece", movement.getDocumentNumber());
            row.put("libelle", movement.getReason());
            row.put("entree", signed.signum() > 0 ? signed : null);
            row.put("sortie", signed.signum() < 0 ? signed.negate() : null);
            row.put("statut", movement.getStatus().name());
            row.put("solde", balance);
            row.put("auteur", movement.getCreatedBy());
            rows.add(row);
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("soldeDebut", opening);
        totals.put("entrees", in);
        totals.put("sorties", out);
        totals.put("soldeFin", balance);
        return report("daily-blotter", "Brouillard de caisse du " + day, parameters,
                List.of("heure", "numero", "session", "type", "nature", "piece", "libelle", "entree", "sortie", "statut", "solde", "auteur"),
                rows, totals);
    }

    private CashReportResponse sessions(String code, Instant from, Instant to, Map<String, Object> parameters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal varianceTotal = BigDecimal.ZERO;
        for (CashSession session : sessionRepository.findOpenedBetween(code, from, to)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("session", session.getSessionNumber());
            row.put("caisse", session.getCashRegister().getRegisterCode());
            row.put("caissier", session.getOpenedBy());
            row.put("ouverture", session.getOpenedAt());
            row.put("cloture", session.getClosedAt());
            row.put("dureeMinutes", session.getOpenedAt() == null ? null
                    : Duration.between(session.getOpenedAt(), session.getClosedAt() == null ? Instant.now() : session.getClosedAt()).toMinutes());
            row.put("statut", session.getStatus().name());
            row.put("fondsOuverture", session.getOpeningAmount());
            row.put("compte", session.getCountedClosingAmount());
            row.put("ecart", session.getVarianceAmount());
            row.put("decision", session.getVarianceDecision() == null ? null : session.getVarianceDecision().name());
            row.put("reviseur", session.getReviewedBy());
            rows.add(row);
            if (session.getVarianceAmount() != null) {
                varianceTotal = varianceTotal.add(session.getVarianceAmount());
            }
        }
        return report("sessions", "Historique des sessions", parameters,
                List.of("session", "caisse", "caissier", "ouverture", "cloture", "dureeMinutes", "statut", "fondsOuverture", "compte",
                        "ecart", "decision", "reviseur"), rows, Map.of("sessions", rows.size(), "ecartCumule", varianceTotal));
    }

    private CashReportResponse variances(String code, Instant from, Instant to, Map<String, Object> parameters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> byCashier = new TreeMap<>();
        for (CashVarianceReview review : reviewRepository.findBetween(code, from, to)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ecart", review.getReviewNumber());
            row.put("caisse", review.getRegisterCode());
            row.put("session", review.getSessionNumber());
            row.put("origine", review.getKind().name());
            row.put("caissier", review.getCashier());
            row.put("montant", review.getAmount());
            row.put("statut", review.getStatus().name());
            row.put("decision", review.getDecision() == null ? null : review.getDecision().name());
            row.put("decidePar", review.getDecidedBy());
            row.put("motif", review.getReason());
            rows.add(row);
            if (review.getCashier() != null) {
                byCashier.merge(review.getCashier(), review.getAmount(), BigDecimal::add);
            }
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("ecarts", rows.size());
        totals.put("cumulParCaissier", byCashier);
        return report("variances", "Etat des ecarts", parameters,
                List.of("ecart", "caisse", "session", "origine", "caissier", "montant", "statut", "decision", "decidePar", "motif"), rows, totals);
    }

    private CashReportResponse transfers(String code, Instant from, Instant to, Map<String, Object> parameters) {
        List<CashTransferOrder> orders = code == null
                ? transferRepository.findAll().stream().filter(order -> !order.getSentAt().isBefore(from) && order.getSentAt().isBefore(to)).toList()
                : transferRepository.findByRegister(code).stream().filter(order -> !order.getSentAt().isBefore(from) && order.getSentAt().isBefore(to)).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CashTransferOrder order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("transfert", order.getTransferNumber());
            row.put("nature", order.getKind().name());
            row.put("de", order.getFromRegister());
            row.put("vers", order.getToRegister());
            row.put("envoye", order.getSentAmount());
            row.put("recu", order.getReceivedAmount());
            row.put("statut", order.getStatus().name());
            row.put("envoyePar", order.getSentBy());
            row.put("envoyeLe", order.getSentAt());
            row.put("recuPar", order.getReceivedBy());
            row.put("recuLe", order.getReceivedAt());
            rows.add(row);
        }
        return report("transfers", "Etat des transferts", parameters,
                List.of("transfert", "nature", "de", "vers", "envoye", "recu", "statut", "envoyePar", "envoyeLe", "recuPar", "recuLe"), rows,
                Map.of("transferts", rows.size(), "enTransit", orders.stream().filter(order -> order.getStatus() == CashTransferStatus.IN_TRANSIT)
                        .map(CashTransferOrder::getSentAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private CashReportResponse counts(String code, Map<String, Object> parameters) {
        Instant now = Instant.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CashRegister register : registerRepository.findAll()) {
            if (register.getStatus() == CashRegisterStatus.CLOSED || register.getRegisterType() == CashRegisterType.AUTOMATIC
                    || (code != null && !code.equals(register.getRegisterCode()))) {
                continue;
            }
            List<CashCount> counts = countRepository.findByRegisterCodeOrderByCreatedAtDesc(register.getRegisterCode());
            CashCount last = counts.stream().filter(count -> "VALID".equals(count.getStatus())).findFirst().orElse(null);
            Instant lastSpot = counts.stream().filter(count -> count.getMoment() == CashCountMoment.SPOT_CHECK).map(CashCount::getCreatedAt)
                    .findFirst().orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caisse", register.getRegisterCode());
            row.put("libelle", register.getName());
            row.put("dernierComptage", register.getLastCountAt());
            row.put("montant", register.getLastCountAmount());
            row.put("compteur", last == null ? null : last.getCountedBy());
            row.put("moment", last == null ? null : last.getMoment().name());
            row.put("joursDepuis", register.getLastCountAt() == null ? null : ChronoUnit.DAYS.between(register.getLastCountAt(), now));
            row.put("dernierInopine", lastSpot);
            row.put("inopineEnRetard", register.getSpotCheckEveryDays() != null && (lastSpot == null
                    || lastSpot.plus(Duration.ofDays(register.getSpotCheckEveryDays())).isBefore(now)));
            row.put("billetsAbimes", last == null ? null : last.getDamagedTotal());
            rows.add(row);
        }
        return report("counts", "Etat des comptages", parameters,
                List.of("caisse", "libelle", "dernierComptage", "montant", "compteur", "moment", "joursDepuis", "dernierInopine",
                        "inopineEnRetard", "billetsAbimes"), rows, Map.of("caisses", rows.size()));
    }

    private CashReportResponse outflowsByNature(String code, Instant from, Instant to, Map<String, Object> parameters) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (CashMovement movement : movementRepository.findByRegisterBetween(code, from, to)) {
            BigDecimal signed = CashFlowRules.signedAmount(movement);
            if (signed.signum() >= 0 || movement.getMovementType() == com.sni.bokaticowork.features.payment.enums.CashMovementType.TRANSFER_OUT) {
                continue;
            }
            String nature = movement.getNatureCode() != null ? movement.getNatureCode()
                    : movement.getFlowCategory() != null ? movement.getFlowCategory() : movement.getMovementType().name();
            totals.merge(nature, signed.negate(), BigDecimal::add);
            counts.merge(nature, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        totals.forEach((nature, amount) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nature", nature);
            row.put("libelle", natureRepository.findByCode(nature).map(found -> found.getLabel()).orElse(nature));
            row.put("operations", counts.get(nature));
            row.put("montant", amount);
            rows.add(row);
        });
        return report("outflows-by-nature", "Sorties par nature", parameters, List.of("nature", "libelle", "operations", "montant"), rows,
                Map.of("total", totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    /** PV en attente : passations, remises de caisse a signer, clotures d'office contestables. */
    private CashReportResponse pendingDocuments(Map<String, Object> parameters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        handoverRepository.findByStatusOrderByCreatedAtAsc(CashHandoverStatus.PENDING).forEach(handover -> rows.add(Map.of(
                "piece", handover.getHandoverNumber(), "nature", "Passation", "caisse", handover.getRegisterCode(),
                "attendu", "Confirmation de " + handover.getIncoming(), "depuis", handover.getCreatedAt())));
        responsibilityRepository.findByStatusOrderByCreatedAtAsc(CashResponsibilityTransferStatus.PENDING_SIGNATURES).forEach(transfer -> rows.add(Map.of(
                "piece", transfer.getTransferNumber(), "nature", "Remise de caisse", "caisse", transfer.getRegisterCode(),
                "attendu", transfer.getIncomingSignedAt() == null ? "Signature de " + transfer.getToUser() : "Signature du sortant",
                "depuis", transfer.getCreatedAt())));
        reviewRepository.findByStatusOrderByCreatedAtAsc(CashVarianceStatus.PENDING).stream()
                .filter(review -> review.getContestDeadline() != null)
                .forEach(review -> rows.add(Map.of("piece", review.getReviewNumber(), "nature", "Cloture d'office", "caisse", review.getRegisterCode(),
                        "attendu", review.getContestedAt() == null ? "Delai de contestation jusqu'au " + review.getContestDeadline() : "Decision apres contestation",
                        "depuis", review.getCreatedAt())));
        return report("pending-documents", "Proces-verbaux en attente", parameters, List.of("piece", "nature", "caisse", "attendu", "depuis"),
                rows, Map.of("enAttente", rows.size()));
    }

    private CashReportResponse report(String kind, String title, Map<String, Object> parameters, List<String> columns,
                                      List<Map<String, Object>> rows, Map<String, Object> totals) {
        return new CashReportResponse(kind, title, Instant.now(), actors.current(), parameters, columns, rows, totals);
    }

    @Override
    public String toCsv(CashReportResponse report) {
        StringBuilder csv = new StringBuilder();
        csv.append(csvLine(new ArrayList<>(report.columns())));
        for (Map<String, Object> row : report.rows()) {
            List<Object> values = new ArrayList<>();
            report.columns().forEach(column -> values.add(row.get(column)));
            csv.append(csvLine(values));
        }
        csv.append("\r\n").append(csvLine(List.of("Edite le " + report.generatedAt() + (report.generatedBy() == null ? "" : " par " + report.generatedBy()))));
        return csv.toString();
    }

    private static String csvLine(List<?> values) {
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                line.append(';');
            }
            Object value = values.get(index);
            String text = value == null ? "" : value instanceof BigDecimal amount ? amount.stripTrailingZeros().toPlainString() : value.toString();
            if (text.contains(";") || text.contains("\"") || text.contains("\n")) {
                text = "\"" + text.replace("\"", "\"\"") + "\"";
            }
            line.append(text);
        }
        return line.append("\r\n").toString();
    }

    // ------------------------------------------------------------- alertes

    @Override
    @Transactional
    public void runPeriodicChecks() {
        Instant now = Instant.now();
        for (CashRegister register : registerRepository.findAll()) {
            try {
                if (register.getStatus() != CashRegisterStatus.ACTIVE || register.getRegisterType() == CashRegisterType.AUTOMATIC
                        || Boolean.TRUE.equals(register.getSystemManaged()) || register.getNotCountedAlertDays() == null) {
                    continue;
                }
                Instant reference = register.getLastCountAt() != null ? register.getLastCountAt() : register.getCreatedAt();
                if (reference == null || reference.plus(Duration.ofDays(register.getNotCountedAlertDays())).isAfter(now)) {
                    continue;
                }
                if (register.getNotCountedAlertedAt() != null && register.getNotCountedAlertedAt().plus(Duration.ofDays(1)).isAfter(now)) {
                    continue;
                }
                register.setNotCountedAlertedAt(now);
                registerRepository.save(register);
                emailNotifier.notifySupervisor("Caisse non comptee · " + register.getRegisterCode(),
                        "La caisse " + register.getName() + " n'a pas ete comptee depuis "
                                + ChronoUnit.DAYS.between(reference, now) + " jours. Faites-la compter (comptage inopine ou cloture comptee).",
                        register.getRegisterCode(), "/payments/cash-registers/" + register.getRegisterCode());
            } catch (RuntimeException exception) {
                log.error("Caisse · alerte de comptage en echec pour {} : {}", register.getRegisterCode(), exception.getMessage(), exception);
            }
        }
    }

    private static BigDecimal sum(List<CashTransferOrder> orders) {
        return orders.stream().map(CashTransferOrder::getSentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount == null ? null : amount.setScale(4, RoundingMode.HALF_UP);
    }
}
