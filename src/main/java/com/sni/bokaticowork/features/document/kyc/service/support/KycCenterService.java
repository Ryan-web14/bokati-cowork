package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Lot KG7 : pilotage du centre KYC. Tableau de bord, calendrier et relance des renouvellements,
 * doublons de pieces. Les volumes KYC restent faibles : calculs en memoire sur les dossiers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KycCenterService {

    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final Set<KycCaseStatus> WAITING_REVIEW = EnumSet.of(KycCaseStatus.SUBMITTED, KycCaseStatus.UNDER_REVIEW);
    private static final Set<KycCaseStatus> CLOSED = EnumSet.of(KycCaseStatus.APPROVED, KycCaseStatus.REJECTED, KycCaseStatus.EXPIRED);
    private static final Set<KycDocumentVerificationStatus> LIVE = EnumSet.of(KycDocumentVerificationStatus.PENDING,
            KycDocumentVerificationStatus.VERIFIED);
    private static final int BLOCKED_DAYS = 14;
    private static final int REMIND_COOLDOWN_DAYS = 7;

    private final KycCaseRepository caseRepository;
    private final KycDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;

    // ------------------------------------------------------------------ tableau de bord

    public record AgeBuckets(long underTwoDays, long twoToSevenDays, long overSevenDays) {
    }

    public record AgentLoad(Long userId, String name, long assignedOpen, long decidedInPeriod) {
    }

    public record CaseRef(String code, KycCaseStatus status, String ownerType, Long ownerId, Instant since, String detail) {
    }

    public record CenterDashboard(LocalDate from, LocalDate to, Map<KycCaseStatus, Long> byStatus, AgeBuckets waitingReview,
                                  Double averageProcessingHours, Double medianProcessingHours, long decidedInPeriod,
                                  List<CaseRef> overdue, List<AgentLoad> agents, List<CaseRef> blocked, long duplicateGroups) {
    }

    /**
     * @param from debut de la periode (decisions), defaut il y a 30 jours
     * @param to   fin de la periode, defaut aujourd'hui
     */
    public CenterDashboard dashboard(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZONE) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        Instant periodStart = start.atStartOfDay(ZONE).toInstant();
        Instant periodEnd = end.plusDays(1).atStartOfDay(ZONE).toInstant();
        Instant now = Instant.now();
        List<KycCase> cases = caseRepository.findAll();

        Map<KycCaseStatus, Long> byStatus = new EnumMap<>(KycCaseStatus.class);
        for (KycCaseStatus status : KycCaseStatus.values()) {
            byStatus.put(status, 0L);
        }
        cases.forEach(c -> byStatus.merge(c.getStatus(), 1L, Long::sum));

        long under2 = 0;
        long twoTo7 = 0;
        long over7 = 0;
        for (KycCase c : cases) {
            if (!WAITING_REVIEW.contains(c.getStatus())) {
                continue;
            }
            Instant since = c.getSubmittedAt() != null ? c.getSubmittedAt() : c.getStartedAt();
            long hours = since == null ? 0 : Duration.between(since, now).toHours();
            if (hours < 48) {
                under2++;
            } else if (hours <= 24 * 7) {
                twoTo7++;
            } else {
                over7++;
            }
        }

        List<KycCase> decided = cases.stream()
                .filter(c -> (c.getStatus() == KycCaseStatus.APPROVED || c.getStatus() == KycCaseStatus.REJECTED)
                        && c.getReviewedAt() != null && !c.getReviewedAt().isBefore(periodStart) && c.getReviewedAt().isBefore(periodEnd))
                .toList();
        List<Double> durations = decided.stream()
                .filter(c -> c.getSubmittedAt() != null && !c.getReviewedAt().isBefore(c.getSubmittedAt()))
                .map(c -> Duration.between(c.getSubmittedAt(), c.getReviewedAt()).toMinutes() / 60.0)
                .sorted()
                .toList();
        Double average = durations.isEmpty() ? null : round(durations.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        Double median = durations.isEmpty() ? null : round(median(durations));

        List<CaseRef> overdue = cases.stream()
                .filter(c -> c.getSlaDeadline() != null && c.getSlaDeadline().isBefore(now) && !CLOSED.contains(c.getStatus()))
                .sorted((a, b) -> a.getSlaDeadline().compareTo(b.getSlaDeadline()))
                .map(c -> ref(c, c.getSlaDeadline(), "Échéance dépassée de "
                        + Duration.between(c.getSlaDeadline(), now).toDays() + " jour(s)"))
                .toList();

        Map<Long, long[]> load = new HashMap<>();
        cases.stream().filter(c -> c.getAssignedTo() != null && !CLOSED.contains(c.getStatus()))
                .forEach(c -> load.computeIfAbsent(c.getAssignedTo(), k -> new long[2])[0]++);
        decided.stream().filter(c -> c.getReviewedBy() != null)
                .forEach(c -> load.computeIfAbsent(c.getReviewedBy(), k -> new long[2])[1]++);
        Map<Long, String> names = names(load.keySet());
        List<AgentLoad> agents = load.entrySet().stream()
                .map(e -> new AgentLoad(e.getKey(), names.get(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.assignedOpen() + b.decidedInPeriod(), a.assignedOpen() + a.decidedInPeriod()))
                .toList();

        Map<Long, Long> piecesByCase = documentRepository.findAll().stream()
                .filter(d -> d.getKycCase() != null)
                .collect(Collectors.groupingBy(d -> d.getKycCase().getId(), Collectors.counting()));
        Instant blockedBefore = now.minus(BLOCKED_DAYS, ChronoUnit.DAYS);
        List<CaseRef> blocked = new ArrayList<>();
        for (KycCase c : cases) {
            if (c.getStatus() == KycCaseStatus.PENDING_CORRECTION && c.getReviewedAt() != null && c.getReviewedAt().isBefore(blockedBefore)) {
                blocked.add(ref(c, c.getReviewedAt(), "En attente de correction depuis "
                        + Duration.between(c.getReviewedAt(), now).toDays() + " jours"));
            } else if (!CLOSED.contains(c.getStatus()) && piecesByCase.getOrDefault(c.getId(), 0L) == 0
                    && c.getStartedAt() != null && c.getStartedAt().isBefore(blockedBefore)) {
                blocked.add(ref(c, c.getStartedAt(), "Aucune pièce déposée depuis l'ouverture ("
                        + Duration.between(c.getStartedAt(), now).toDays() + " jours)"));
            }
        }

        return new CenterDashboard(start, end, byStatus, new AgeBuckets(under2, twoTo7, over7), average, median, decided.size(),
                overdue, agents, blocked, duplicates().size());
    }

    // ------------------------------------------------------------------ renouvellements

    public record RenewalItem(Long kycDocumentId, String kycCaseCode, String documentType, String documentNumber,
                              String ownerType, Long ownerId, LocalDate expiryDate, long daysLeft, Instant lastRemindedAt) {
    }

    public record RenewalWeek(LocalDate weekStart, List<RenewalItem> items) {
    }

    /** Pieces vivantes qui expirent dans les {@code days} jours, par semaine (lundi). */
    public List<RenewalWeek> renewals(int days) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate limit = today.plusDays(Math.max(1, Math.min(days, 366)));
        Map<LocalDate, List<RenewalItem>> weeks = new TreeMap<>();
        for (KycDocument piece : documentRepository.findAllByExpiryDateBetween(today, limit)) {
            if (!LIVE.contains(piece.getStatus())) {
                continue;
            }
            LocalDate week = piece.getExpiryDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeks.computeIfAbsent(week, k -> new ArrayList<>()).add(item(piece, today));
        }
        return weeks.entrySet().stream()
                .map(e -> new RenewalWeek(e.getKey(), e.getValue().stream()
                        .sorted((a, b) -> a.expiryDate().compareTo(b.expiryDate())).toList()))
                .toList();
    }

    public record RemindResult(int reminded, int skippedRecentlyReminded) {
    }

    /**
     * Relance groupee : un rappel d'echeance par piece vivante qui expire dans les {@code days} jours
     * (ou seulement les pieces citees), sauf si elle a deja ete relancee dans les 7 derniers jours.
     */
    @Transactional
    public RemindResult remind(int days, Collection<Long> kycDocumentIds) {
        LocalDate today = LocalDate.now(ZONE);
        Instant now = Instant.now();
        Instant cooldown = now.minus(REMIND_COOLDOWN_DAYS, ChronoUnit.DAYS);
        int reminded = 0;
        int skipped = 0;
        for (KycDocument piece : documentRepository.findAllByExpiryDateBetween(today, today.plusDays(Math.max(1, Math.min(days, 366))))) {
            if (!LIVE.contains(piece.getStatus())
                    || (kycDocumentIds != null && !kycDocumentIds.isEmpty() && !kycDocumentIds.contains(piece.getId()))) {
                continue;
            }
            if (piece.getRenewalRemindedAt() != null && piece.getRenewalRemindedAt().isAfter(cooldown)) {
                skipped++;
                continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("daysUntilExpiry", ChronoUnit.DAYS.between(today, piece.getExpiryDate()));
            payload.put("documentCode", piece.getDocument() == null ? null : piece.getDocument().getCode());
            payload.put("documentType", piece.getDocumentType());
            payload.put("kycCaseCode", piece.getKycCase() == null ? null : piece.getKycCase().getCode());
            payload.put("expiryDate", piece.getExpiryDate().toString());
            payload.put("ownerType", piece.getOwnerType());
            payload.put("ownerId", piece.getOwnerId());
            payload.put("manual", true);
            outboxService.publish("KYC_DOCUMENT_EXPIRY_REMINDER", "KYC_DOCUMENT", String.valueOf(piece.getId()), payload);
            piece.setRenewalRemindedAt(now);
            documentRepository.save(piece);
            reminded++;
        }
        return new RemindResult(reminded, skipped);
    }

    // ------------------------------------------------------------------ doublons

    public record DuplicatePiece(Long kycDocumentId, String kycCaseCode, String ownerType, Long ownerId, String documentNumber,
                                 KycDocumentVerificationStatus status) {
    }

    public record DuplicateGroup(String documentType, String normalizedNumber, List<DuplicatePiece> pieces) {
    }

    /** Meme type et meme numero normalise sur au moins deux titulaires differents. Signal, jamais blocage. */
    public List<DuplicateGroup> duplicates() {
        Map<String, List<KycDocument>> groups = new LinkedHashMap<>();
        for (KycDocument piece : documentRepository.findAll()) {
            String number = normalize(piece.getDocumentNumber());
            if (number == null || piece.getStatus() == KycDocumentVerificationStatus.EXPIRED) {
                continue;
            }
            groups.computeIfAbsent(piece.getDocumentType() + "|" + number, k -> new ArrayList<>()).add(piece);
        }
        List<DuplicateGroup> result = new ArrayList<>();
        groups.forEach((key, pieces) -> {
            long owners = pieces.stream().map(p -> p.getOwnerType() + ":" + p.getOwnerId()).distinct().count();
            if (owners < 2) {
                return;
            }
            String[] parts = key.split("\\|", 2);
            result.add(new DuplicateGroup(parts[0], parts[1], pieces.stream()
                    .map(p -> new DuplicatePiece(p.getId(), p.getKycCase() == null ? null : p.getKycCase().getCode(),
                            p.getOwnerType() == null ? null : p.getOwnerType().name(), p.getOwnerId(), p.getDocumentNumber(), p.getStatus()))
                    .toList()));
        });
        return result;
    }

    /** Codes des dossiers d'autres titulaires qui ont la meme piece (meme type, meme numero). */
    public List<String> duplicateOf(KycDocument piece) {
        String number = normalize(piece.getDocumentNumber());
        if (number == null) {
            return List.of();
        }
        return documentRepository.findAll().stream()
                .filter(other -> !Objects.equals(other.getId(), piece.getId()))
                .filter(other -> other.getStatus() != KycDocumentVerificationStatus.EXPIRED)
                .filter(other -> Objects.equals(other.getDocumentType(), piece.getDocumentType()))
                .filter(other -> !(Objects.equals(other.getOwnerType(), piece.getOwnerType()) && Objects.equals(other.getOwnerId(), piece.getOwnerId())))
                .filter(other -> number.equals(normalize(other.getDocumentNumber())))
                .map(other -> other.getKycCase() == null ? "#" + other.getId() : other.getKycCase().getCode())
                .distinct()
                .toList();
    }

    /** « M 0123-45 » et « m012345 » : meme numero (casse, accents, espaces, ponctuation ignores). */
    static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String bare = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return bare.isEmpty() ? null : bare;
    }

    // ------------------------------------------------------------------ utilitaires

    private RenewalItem item(KycDocument piece, LocalDate today) {
        return new RenewalItem(piece.getId(), piece.getKycCase() == null ? null : piece.getKycCase().getCode(), piece.getDocumentType(),
                piece.getDocumentNumber(), piece.getOwnerType() == null ? null : piece.getOwnerType().name(), piece.getOwnerId(),
                piece.getExpiryDate(), ChronoUnit.DAYS.between(today, piece.getExpiryDate()), piece.getRenewalRemindedAt());
    }

    private static CaseRef ref(KycCase c, Instant since, String detail) {
        return new CaseRef(c.getCode(), c.getStatus(), c.getOwnerType() == null ? null : c.getOwnerType().name(), c.getOwnerId(), since, detail);
    }

    private Map<Long, String> names(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(u -> u.getId(), u -> {
            String name = ((u.getFirstname() == null ? "" : u.getFirstname()) + " " + (u.getLastname() == null ? "" : u.getLastname())).trim();
            return name.isEmpty() ? String.valueOf(u.getEmail()) : name;
        }, (a, b) -> a));
    }

    private static double median(List<Double> sorted) {
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
