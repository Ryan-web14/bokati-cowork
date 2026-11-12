package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashAbsenceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDelegationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashSealRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAbsenceResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDelegationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReplacementSuggestionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSealResponse;
import com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceKind;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashDelegation;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashRegisterHistory;
import com.sni.bokaticowork.features.payment.model.CashSeal;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashStaffAbsence;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.CashDelegationRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterAssignmentRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterHistoryRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSealRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashStaffAbsenceRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStaffService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashCountRecorder;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Personnel de caisse (lot C2) : absences et remplacements, scelles, delegations des reviseurs.
 *
 * <p>Une caisse n'attend jamais le retour d'une personne : une absence validee cree l'interim du
 * remplacant pour la periode, bloque l'ouverture par l'absent, et le premier jour suspend une
 * session qu'il aurait laissee ouverte.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CashStaffServiceImpl implements CashStaffService {

    static final String ABSENCE_REASON = "ABSENCE_REASON";
    static final String DEPARTURE = "DEPARTURE";
    static final int OPEN_ENDED_INTERIM_DAYS = 30;
    static final int REMINDER_DAYS = 2;
    private static final String MANAGE = "CASH_STAFF:MANAGE";
    private static final Set<String> DELEGABLE = Set.of("CASH_VARIANCE_DECIDE", "CASH_MOVEMENT_APPROVE");

    private final CashStaffAbsenceRepository absenceRepository;
    private final CashRegisterAssignmentRepository assignmentRepository;
    private final CashRegisterRepository registerRepository;
    private final CashRegisterHistoryRepository historyRepository;
    private final CashSessionRepository sessionRepository;
    private final CashVarianceReviewRepository reviewRepository;
    private final CashSealRepository sealRepository;
    private final CashDelegationRepository delegationRepository;
    private final CashCountRecorder countRecorder;
    private final CashRegisterAccessGuard accessGuard;
    private final CashReferenceLists referenceLists;
    private final CashActorResolver actors;
    private final CashEmailNotifier emailNotifier;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashSessionLifecycleService lifecycle;

    // ------------------------------------------------------------------ absences

    @Override
    public CashAbsenceResponse declareAbsence(CashAbsenceRequest request) {
        String actor = actors.resolve(null);
        String user = StringUtils.hasText(request.userEmail()) ? email(request.userEmail()) : actor.toLowerCase(Locale.ROOT);
        boolean manager = actors.hasAuthority(MANAGE);
        if (!CashActorResolver.samePerson(user, actor) && !manager) {
            throw new ForbiddenException("Seul un responsable declare l'absence d'une autre personne");
        }
        if (request.endsOn() != null && request.endsOn().isBefore(request.startsOn())) {
            throw new BadRequestException("La fin de l'absence precede son debut");
        }
        String kind = request.kindCode().trim().toUpperCase(Locale.ROOT);
        String reason = referenceLists.describe(ABSENCE_REASON, kind, request.reasonText());
        Instant start = startOf(request.startsOn());
        List<String> registers = assignmentRepository.findLiveByUser(user).stream()
                .filter(assignment -> assignment.getRole() != CashAssignmentRole.VIEWER)
                .filter(assignment -> assignment.getValidTo() == null || assignment.getValidTo().isAfter(start))
                .map(CashRegisterAssignment::getRegisterCode)
                .distinct().toList();
        String replacement = StringUtils.hasText(request.replacementEmail()) ? email(request.replacementEmail()) : null;
        List<String> warnings = new ArrayList<>();
        if (replacement == null && !DEPARTURE.equals(kind)) {
            for (String register : registers) {
                List<CashReplacementSuggestionResponse> suggestions = replacementSuggestions(register, request.startsOn(), request.endsOn());
                if (!suggestions.isEmpty()) {
                    replacement = suggestions.get(0).userEmail();
                    warnings.add("Remplacant propose d'office : " + replacement);
                    break;
                }
            }
        }
        if (replacement != null && CashActorResolver.samePerson(replacement, user)) {
            throw new BadRequestException("Le remplacant doit etre une autre personne que l'absent");
        }
        if (replacement == null && !registers.isEmpty() && !DEPARTURE.equals(kind)) {
            warnings.add("Aucun remplacant : le fonds suivra la regle choisie ("
                    + (request.noReplacementPolicy() == null ? "remise a la caisse mere" : request.noReplacementPolicy().name()) + ")");
        }
        CashStaffAbsence absence = absenceRepository.save(CashStaffAbsence.builder()
                .absenceNumber(sequenceGenerator.next("cash_staff_absence"))
                .userEmail(user)
                .kindCode(kind)
                .reason(reason)
                .startsOn(request.startsOn())
                .endsOn(request.endsOn())
                .replacementUser(replacement)
                .registerCodes(String.join(",", registers))
                .noReplacementPolicy(replacement == null ? request.noReplacementPolicy() : null)
                .status(CashAbsenceStatus.PLANNED)
                .unplanned(Boolean.TRUE.equals(request.unplanned()))
                .declaredBy(actor)
                .forced(Boolean.TRUE.equals(request.force()))
                .forcedNote(trim(request.forceNote()))
                .build());
        if (manager) {
            // Declaree par un responsable : validee d'office.
            warnings.addAll(apply(absence, actor));
        } else {
            emailNotifier.notifySupervisor("Absence de caisse a valider · " + absence.getAbsenceNumber(),
                    user + " declare une absence du " + absence.getStartsOn()
                            + (absence.getEndsOn() == null ? "" : " au " + absence.getEndsOn()) + " (" + reason + ")"
                            + (replacement == null ? ", sans remplacant." : ", remplacant propose : " + replacement + "."),
                    absence.getAbsenceNumber(), "/payments/cash-registers/absences");
        }
        return toResponse(absence, warnings);
    }

    @Override
    public CashAbsenceResponse approveAbsence(String absenceNumber) {
        CashStaffAbsence absence = absence(absenceNumber);
        if (absence.getStatus() != CashAbsenceStatus.PLANNED) {
            throw new BadRequestException("Cette absence n'attend pas de validation");
        }
        String actor = actors.resolve(null);
        if (CashActorResolver.samePerson(actor, absence.getUserEmail())) {
            throw new ForbiddenException("On ne valide pas sa propre absence");
        }
        List<String> warnings = apply(absence, actor);
        return toResponse(absence, warnings);
    }

    /** Valide l'absence : interim du remplacant, fin des affectations d'un partant, suspension. */
    private List<String> apply(CashStaffAbsence absence, String actor) {
        List<String> warnings = new ArrayList<>();
        Instant start = startOf(absence.getStartsOn());
        if (DEPARTURE.equals(absence.getKindCode())) {
            List<String> pending = pendingForDeparture(absence.getUserEmail());
            if (!pending.isEmpty() && !Boolean.TRUE.equals(absence.getForced())) {
                throw new BadRequestException("Depart bloque tant que restent en cours : " + String.join(" ; ", pending)
                        + ". Un responsable peut le forcer en indiquant la suite donnee.");
            }
            if (!pending.isEmpty() && !StringUtils.hasText(absence.getForcedNote())) {
                throw new BadRequestException("Indiquez la suite donnee aux elements en cours pour forcer le depart");
            }
            for (CashRegisterAssignment assignment : assignmentRepository.findLiveByUser(absence.getUserEmail())) {
                if (assignment.getValidTo() == null || assignment.getValidTo().isAfter(start)) {
                    if (assignment.getQuality() == CashAssignmentQuality.HOLDER) {
                        warnings.add("La caisse " + assignment.getRegisterCode() + " perd son titulaire : faites une remise de caisse");
                    }
                    assignment.setValidTo(start.isAfter(assignment.getValidFrom()) ? start : assignment.getValidFrom().plusSeconds(1));
                    assignmentRepository.save(assignment);
                    history(assignment.getRegisterCode(), "assignments", assignment.getUserEmail() + " " + assignment.getQuality(),
                            "fin au " + absence.getStartsOn() + " (depart, " + absence.getAbsenceNumber() + ")", actor);
                }
            }
        } else if (absence.getReplacementUser() != null) {
            Instant end = absence.getEndsOn() == null ? start.plus(Duration.ofDays(OPEN_ENDED_INTERIM_DAYS))
                    : startOf(absence.getEndsOn().plusDays(1));
            for (String register : split(absence.getRegisterCodes())) {
                assignmentRepository.save(CashRegisterAssignment.builder()
                        .registerCode(register).userEmail(absence.getReplacementUser())
                        .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.INTERIM)
                        .validFrom(start).validTo(end).source("ABSENCE").sourceReference(absence.getAbsenceNumber())
                        .grantedBy(actor).note("Remplacement de " + absence.getUserEmail()).build());
                history(register, "assignments", null, "Interim de " + absence.getReplacementUser() + " pendant l'absence de "
                        + absence.getUserEmail() + " (" + absence.getAbsenceNumber() + ")", actor);
            }
            if (absence.getEndsOn() == null) {
                warnings.add("Absence sans fin connue : l'interim court " + OPEN_ENDED_INTERIM_DAYS + " jours, a prolonger si besoin");
            }
            emailNotifier.notifyUser(absence.getReplacementUser(), "Vous remplacez " + absence.getUserEmail(),
                    "Vous tenez " + (split(absence.getRegisterCodes()).size() > 1 ? "les caisses " : "la caisse ")
                            + absence.getRegisterCodes() + " du " + absence.getStartsOn()
                            + (absence.getEndsOn() == null ? "" : " au " + absence.getEndsOn()) + ".",
                    absence.getAbsenceNumber(), "/payments/cash-registers/absences");
        }
        absence.setStatus(CashAbsenceStatus.APPROVED);
        absence.setApprovedBy(actor);
        absence.setApprovedAt(Instant.now());
        absenceRepository.save(absence);
        if (!absence.getStartsOn().isAfter(LocalDate.now(CashRegisterAccessGuard.ZONE))) {
            activate(absence);
        }
        return warnings;
    }

    /** Premier jour de l'absence : la session encore ouverte de l'absent est suspendue. */
    private void activate(CashStaffAbsence absence) {
        absence.setStatus(CashAbsenceStatus.ACTIVE);
        absenceRepository.save(absence);
        sessionRepository.findBlockingSessionByOpenedBy(absence.getUserEmail()).ifPresent(session -> {
            if (EnumSet.of(CashSessionStatus.OPEN, CashSessionStatus.OPENING, CashSessionStatus.PAUSED).contains(session.getStatus())) {
                lifecycle.suspendBySystem(session.getSessionNumber(), "Absence de " + absence.getUserEmail()
                        + " (" + absence.getAbsenceNumber() + ") : la session doit etre close d'office par un superviseur");
            }
        });
    }

    private List<String> pendingForDeparture(String user) {
        List<String> pending = new ArrayList<>();
        sessionRepository.findBlockingSessionByOpenedBy(user)
                .ifPresent(session -> pending.add("session " + session.getSessionNumber() + " non close"));
        reviewRepository.findByStatusOrderByCreatedAtAsc(CashVarianceStatus.PENDING).stream()
                .filter(review -> CashActorResolver.samePerson(review.getCashier(), user))
                .forEach(review -> pending.add("ecart " + review.getReviewNumber() + " non decide"));
        return pending;
    }

    @Override
    public CashAbsenceResponse cancelAbsence(String absenceNumber) {
        CashStaffAbsence absence = absence(absenceNumber);
        if (absence.getStatus() == CashAbsenceStatus.ENDED || absence.getStatus() == CashAbsenceStatus.CANCELLED) {
            throw new BadRequestException("Cette absence est deja terminee");
        }
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, absence.getUserEmail()) && !actors.hasAuthority(MANAGE)) {
            throw new ForbiddenException("Seuls l'absent et un responsable annulent cette absence");
        }
        endInterims(absence, Instant.now(), actor);
        absence.setStatus(CashAbsenceStatus.CANCELLED);
        return toResponse(absenceRepository.save(absence), List.of());
    }

    @Override
    public CashAbsenceResponse endAbsence(String absenceNumber) {
        CashStaffAbsence absence = absence(absenceNumber);
        if (absence.getStatus() != CashAbsenceStatus.ACTIVE && absence.getStatus() != CashAbsenceStatus.APPROVED) {
            throw new BadRequestException("Cette absence n'est pas en cours");
        }
        String actor = actors.resolve(null);
        endInterims(absence, Instant.now(), actor);
        absence.setStatus(CashAbsenceStatus.ENDED);
        absence.setEndsOn(LocalDate.now(CashRegisterAccessGuard.ZONE));
        return toResponse(absenceRepository.save(absence), List.of("Au retour, le titulaire reprend la caisse par une passation avec l'interimaire"));
    }

    private void endInterims(CashStaffAbsence absence, Instant at, String actor) {
        for (String register : split(absence.getRegisterCodes())) {
            assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register).stream()
                    .filter(assignment -> "ABSENCE".equals(assignment.getSource())
                            && absence.getAbsenceNumber().equals(assignment.getSourceReference())
                            && assignment.getRevokedAt() == null)
                    .forEach(assignment -> {
                        if (assignment.getValidFrom().isAfter(at)) {
                            assignment.setRevokedAt(at);
                            assignment.setRevokedBy(actor);
                        } else if (assignment.getValidTo() == null || assignment.getValidTo().isAfter(at)) {
                            assignment.setValidTo(at.isAfter(assignment.getValidFrom()) ? at : assignment.getValidFrom().plusSeconds(1));
                        }
                        assignmentRepository.save(assignment);
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashAbsenceResponse> absences(String userEmail, CashAbsenceStatus status) {
        List<CashStaffAbsence> list = StringUtils.hasText(userEmail) ? absenceRepository.findByUser(userEmail.trim())
                : absenceRepository.findByStatusInOrderByStartsOnAsc(status == null
                ? EnumSet.of(CashAbsenceStatus.PLANNED, CashAbsenceStatus.APPROVED, CashAbsenceStatus.ACTIVE) : EnumSet.of(status));
        return list.stream().filter(absence -> status == null || absence.getStatus() == status)
                .map(absence -> toResponse(absence, List.of())).toList();
    }

    /**
     * Remplacants possibles d'une caisse sur une periode : ses remplacants par ordre de preference,
     * puis les caissiers des autres caisses du site ; sans les personnes absentes sur la periode.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CashReplacementSuggestionResponse> replacementSuggestions(String registerCode, LocalDate from, LocalDate to) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode == null ? null : registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
        LocalDate start = from == null ? LocalDate.now(CashRegisterAccessGuard.ZONE) : from;
        LocalDate end = to == null ? start : to;
        Instant at = startOf(start);
        Map<String, CashReplacementSuggestionResponse> suggestions = new LinkedHashMap<>();
        Set<String> holders = new java.util.HashSet<>();
        assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode()).stream()
                .filter(assignment -> assignment.isEffectiveAt(at))
                .forEach(assignment -> {
                    if (assignment.getQuality() == CashAssignmentQuality.HOLDER) {
                        holders.add(assignment.getUserEmail().toLowerCase(Locale.ROOT));
                    }
                });
        assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode()).stream()
                .filter(assignment -> assignment.getQuality() == CashAssignmentQuality.BACKUP && assignment.isEffectiveAt(at)
                        && assignment.getRole() != CashAssignmentRole.VIEWER)
                .filter(assignment -> !absentDuring(assignment.getUserEmail(), start, end))
                .forEach(assignment -> suggestions.putIfAbsent(assignment.getUserEmail().toLowerCase(Locale.ROOT),
                        new CashReplacementSuggestionResponse(assignment.getUserEmail(), "BACKUP", assignment.getBackupRank(), register.getRegisterCode())));
        if (StringUtils.hasText(register.getLocationCode())) {
            for (CashRegister sibling : registerRepository.findByLocationCodeOrderByNameAsc(register.getLocationCode())) {
                if (sibling.getRegisterCode().equals(register.getRegisterCode()) || sibling.getStatus() == CashRegisterStatus.CLOSED) {
                    continue;
                }
                assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(sibling.getRegisterCode()).stream()
                        .filter(assignment -> assignment.isEffectiveAt(at) && assignment.getRole() != CashAssignmentRole.VIEWER)
                        .filter(assignment -> !holders.contains(assignment.getUserEmail().toLowerCase(Locale.ROOT)))
                        .filter(assignment -> !absentDuring(assignment.getUserEmail(), start, end))
                        .forEach(assignment -> suggestions.putIfAbsent(assignment.getUserEmail().toLowerCase(Locale.ROOT),
                                new CashReplacementSuggestionResponse(assignment.getUserEmail(), "SAME_SITE", null, sibling.getRegisterCode())));
            }
        }
        return List.copyOf(suggestions.values());
    }

    private boolean absentDuring(String user, LocalDate from, LocalDate to) {
        return absenceRepository.findByUser(user).stream()
                .filter(absence -> absence.getStatus() == CashAbsenceStatus.APPROVED || absence.getStatus() == CashAbsenceStatus.ACTIVE
                        || absence.getStatus() == CashAbsenceStatus.PLANNED)
                .anyMatch(absence -> !absence.getStartsOn().isAfter(to) && (absence.getEndsOn() == null || !absence.getEndsOn().isBefore(from)));
    }

    // ------------------------------------------------------------------ scelles

    @Override
    public CashSealResponse seal(String registerCode, CashSealRequest request) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode == null ? null : registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BadRequestException("Caisse fermee");
        }
        sessionRepository.findBlockingSessionByCashRegisterId(register.getId()).ifPresent(session -> {
            throw new BadRequestException("La session " + session.getSessionNumber() + " doit etre close avant la mise sous scelles");
        });
        if (sealRepository.findByRegisterCodeOrderByCreatedAtDesc(register.getRegisterCode()).stream().anyMatch(seal -> seal.getUnsealedAt() == null)) {
            throw new BadRequestException("La caisse est deja sous scelles");
        }
        String actor = actors.resolve(null);
        String witness = witness(request, actor);
        if (!StringUtils.hasText(request.envelopeNumber())) {
            throw new BadRequestException("Indiquez le numero de l'enveloppe scellee");
        }
        String number = sequenceGenerator.next("cash_seal");
        CashCount count = countRecorder.record(register, null, CashCountMoment.SEAL, request.count(), actor,
                register.getLastCountAmount(), number, null, witness);
        CashSeal seal = sealRepository.save(CashSeal.builder()
                .sealNumber(number).registerCode(register.getRegisterCode()).envelopeNumber(request.envelopeNumber().trim())
                .amount(count.getTotal()).countNumber(count.getCountNumber()).sealedBy(actor).witness(witness)
                .depositedIn(trim(request.depositedIn())).absenceNumber(trim(request.absenceNumber())).build());
        String before = register.getStatus().name();
        register.setStatus(CashRegisterStatus.SUSPENDED);
        register.setActive(false);
        register.setStatusReason("Sous scelles (" + number + ", enveloppe " + seal.getEnvelopeNumber() + ")");
        register.setLastCountAt(Instant.now());
        register.setLastCountAmount(count.getTotal());
        registerRepository.save(register);
        history(register.getRegisterCode(), "status", before, "SUSPENDED (scelles " + number + ")", actor);
        return toResponse(seal);
    }

    @Override
    public CashSealResponse unseal(String registerCode, CashSealRequest request) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode == null ? null : registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
        CashSeal seal = sealRepository.findByRegisterCodeOrderByCreatedAtDesc(register.getRegisterCode()).stream()
                .filter(candidate -> candidate.getUnsealedAt() == null).findFirst()
                .orElseThrow(() -> new BadRequestException("La caisse n'est pas sous scelles"));
        String actor = actors.resolve(null);
        String witness = witness(request, actor);
        CashCount count = countRecorder.record(register, null, CashCountMoment.UNSEAL, request.count(), actor,
                seal.getAmount(), seal.getSealNumber(), null, witness);
        seal.setUnsealedAt(Instant.now());
        seal.setUnsealedBy(actor);
        seal.setUnsealWitness(witness);
        seal.setUnsealCount(count.getCountNumber());
        seal.setUnsealAmount(count.getTotal());
        sealRepository.save(seal);
        BigDecimal difference = count.getTotal().subtract(seal.getAmount());
        if (difference.signum() != 0) {
            reviewRepository.save(CashVarianceReview.builder()
                    .reviewNumber(sequenceGenerator.next("cash_variance_review"))
                    .registerCode(register.getRegisterCode())
                    .kind(CashVarianceKind.UNSEAL)
                    .amount(difference)
                    .countNumber(count.getCountNumber())
                    .cashier(seal.getSealedBy())
                    .reason("Ecart a l'ouverture du scelle " + seal.getSealNumber())
                    .status(CashVarianceStatus.PENDING)
                    .build());
            emailNotifier.notifySupervisor("Ecart a l'ouverture d'un scelle · " + seal.getSealNumber(),
                    "Le scelle " + seal.getSealNumber() + " de la caisse " + register.getName() + " contenait " + plain(count.getTotal())
                            + " XAF au lieu de " + plain(seal.getAmount()) + " XAF.", seal.getSealNumber(), "/payments/cash-registers/" + register.getRegisterCode());
        }
        register.setStatus(CashRegisterStatus.ACTIVE);
        register.setActive(true);
        register.setStatusReason(null);
        register.setLastCountAt(Instant.now());
        register.setLastCountAmount(count.getTotal());
        registerRepository.save(register);
        history(register.getRegisterCode(), "status", "SUSPENDED", "ACTIVE (ouverture du scelle " + seal.getSealNumber() + ")", actor);
        return toResponse(seal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashSealResponse> seals(String registerCode) {
        return sealRepository.findByRegisterCodeOrderByCreatedAtDesc(registerCode == null ? null : registerCode.trim()).stream()
                .map(this::toResponse).toList();
    }

    private String witness(CashSealRequest request, String actor) {
        String witness = email(request.witnessEmail());
        if (CashActorResolver.samePerson(witness, actor)) {
            throw new BadRequestException("Le temoin doit etre une autre personne : le comptage se fait a deux");
        }
        return witness;
    }

    // --------------------------------------------------------------- delegations

    @Override
    public CashDelegationResponse delegate(CashDelegationRequest request) {
        String actor = actors.resolve(null);
        String delegate = email(request.delegateEmail());
        if (CashActorResolver.samePerson(actor, delegate)) {
            throw new BadRequestException("On ne se delegue pas a soi-meme");
        }
        if (request.endsOn().isBefore(request.startsOn())) {
            throw new BadRequestException("La fin de la delegation precede son debut");
        }
        List<String> permissions = request.permissions().stream()
                .map(permission -> permission.trim().toUpperCase(Locale.ROOT).replace(':', '_')).distinct().toList();
        for (String permission : permissions) {
            if (!DELEGABLE.contains(permission)) {
                throw new BadRequestException("Permission non delegable : " + permission);
            }
            if (!actors.hasAuthority(permission)) {
                throw new ForbiddenException("Vous ne pouvez deleguer que ce que vous avez : " + permission);
            }
        }
        CashDelegation delegation = delegationRepository.save(CashDelegation.builder()
                .delegator(actor).delegate(delegate).permissions(String.join(",", permissions))
                .startsOn(request.startsOn()).endsOn(request.endsOn()).note(trim(request.note())).createdBy(actor).build());
        emailNotifier.notifyUser(delegate, "Delegation de caisse", actor + " vous delegue ses revues de caisse du "
                + request.startsOn() + " au " + request.endsOn() + " (" + String.join(", ", permissions) + ").", null,
                "/payments/cash-registers");
        return toResponse(delegation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashDelegationResponse> delegations() {
        return delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc().stream().map(this::toResponse).toList();
    }

    @Override
    public CashDelegationResponse cancelDelegation(Long id) {
        CashDelegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation introuvable : " + id));
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, delegation.getDelegator()) && !actors.hasAuthority(MANAGE)) {
            throw new ForbiddenException("Seul celui qui a delegue annule la delegation");
        }
        delegation.setCancelledAt(Instant.now());
        return toResponse(delegationRepository.save(delegation));
    }

    // -------------------------------------------------------- traitement periodique

    @Override
    public void runPeriodicChecks() {
        LocalDate today = LocalDate.now(CashRegisterAccessGuard.ZONE);
        for (CashStaffAbsence absence : absenceRepository.findByStatusInOrderByStartsOnAsc(
                EnumSet.of(CashAbsenceStatus.APPROVED, CashAbsenceStatus.ACTIVE))) {
            try {
                if (absence.getStatus() == CashAbsenceStatus.APPROVED && !absence.getStartsOn().isAfter(today)) {
                    activate(absence);
                } else if (absence.getStatus() == CashAbsenceStatus.ACTIVE && absence.getEndsOn() != null && absence.getEndsOn().isBefore(today)) {
                    absence.setStatus(CashAbsenceStatus.ENDED);
                    absenceRepository.save(absence);
                } else if (absence.getStatus() == CashAbsenceStatus.APPROVED && absence.getReminderSentAt() == null
                        && !absence.getStartsOn().minusDays(REMINDER_DAYS).isAfter(today)) {
                    remind(absence);
                }
            } catch (RuntimeException exception) {
                log.error("Caisse · absence {} non traitee : {}", absence.getAbsenceNumber(), exception.getMessage(), exception);
            }
        }
        alertLongSessions();
    }

    private void remind(CashStaffAbsence absence) {
        sessionRepository.findBlockingSessionByOpenedBy(absence.getUserEmail()).ifPresent(session ->
                emailNotifier.notifyUser(absence.getUserEmail(), "Avant votre absence · " + absence.getAbsenceNumber(),
                        "Votre absence commence le " + absence.getStartsOn() + ". Closez votre session " + session.getSessionNumber()
                                + (absence.getReplacementUser() == null ? " et remettez le fonds a la caisse mere"
                                : " par une passation a " + absence.getReplacementUser()) + " avant de partir.",
                        absence.getAbsenceNumber(), "/payments/cash-registers/sessions/" + session.getSessionNumber()));
        absence.setReminderSentAt(Instant.now());
        absenceRepository.save(absence);
    }

    /** Sessions ouvertes depuis plus longtemps que la duree maximale de leur caisse : une alerte. */
    private void alertLongSessions() {
        Instant now = Instant.now();
        for (CashSession session : sessionRepository.findByStatuses(List.of(CashSessionStatus.OPEN, CashSessionStatus.PAUSED))) {
            CashRegister register = session.getCashRegister();
            if (Boolean.TRUE.equals(register.getSystemManaged()) || session.getMaxDurationAlertedAt() != null || session.getOpenedAt() == null) {
                continue;
            }
            int hours = register.getMaxSessionHours() == null ? 24 : register.getMaxSessionHours();
            if (session.getOpenedAt().plus(Duration.ofHours(hours)).isBefore(now)) {
                session.setMaxDurationAlertedAt(now);
                sessionRepository.save(session);
                String message = "La session " + session.getSessionNumber() + " de " + session.getOpenedBy() + " sur la caisse "
                        + register.getName() + " est ouverte depuis plus de " + hours + " heures : comptez-la et closez-la.";
                emailNotifier.notifyUser(session.getOpenedBy(), "Session de caisse trop longue · " + session.getSessionNumber(),
                        message, session.getSessionNumber(), "/payments/cash-registers/sessions/" + session.getSessionNumber());
                emailNotifier.notifyRegisterManager(register, "Session de caisse trop longue · " + session.getSessionNumber(),
                        message, session.getSessionNumber(), "/payments/cash-registers/sessions/" + session.getSessionNumber());
            }
        }
    }

    // ------------------------------------------------------------------ outils

    private CashStaffAbsence absence(String absenceNumber) {
        return absenceRepository.findByAbsenceNumber(absenceNumber == null ? null : absenceNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable : " + absenceNumber));
    }

    private void history(String registerCode, String field, String oldValue, String newValue, String actor) {
        historyRepository.save(CashRegisterHistory.builder().registerCode(registerCode).field(field)
                .oldValue(oldValue).newValue(newValue).changedBy(actor).changedAt(Instant.now()).build());
    }

    private CashAbsenceResponse toResponse(CashStaffAbsence absence, List<String> warnings) {
        return new CashAbsenceResponse(absence.getAbsenceNumber(), absence.getUserEmail(), absence.getKindCode(),
                referenceLists.label(ABSENCE_REASON, absence.getKindCode()), absence.getReason(), absence.getStartsOn(),
                absence.getEndsOn(), absence.getReplacementUser(), split(absence.getRegisterCodes()), absence.getNoReplacementPolicy(),
                absence.getStatus(), absence.getUnplanned(), absence.getDeclaredBy(), absence.getApprovedBy(), absence.getApprovedAt(),
                absence.getForced(), absence.getForcedNote(), warnings);
    }

    private CashSealResponse toResponse(CashSeal seal) {
        return new CashSealResponse(seal.getSealNumber(), seal.getRegisterCode(), seal.getEnvelopeNumber(), seal.getAmount(),
                seal.getCountNumber(), seal.getSealedBy(), seal.getWitness(), seal.getDepositedIn(), seal.getCreatedAt(),
                seal.getUnsealedAt(), seal.getUnsealedBy(), seal.getUnsealWitness(), seal.getUnsealCount(), seal.getUnsealAmount(),
                seal.getUnsealAmount() == null ? null : seal.getUnsealAmount().subtract(seal.getAmount()), seal.getAbsenceNumber());
    }

    private CashDelegationResponse toResponse(CashDelegation delegation) {
        LocalDate today = LocalDate.now(CashRegisterAccessGuard.ZONE);
        boolean active = delegation.getCancelledAt() == null && !delegation.getStartsOn().isAfter(today) && !delegation.getEndsOn().isBefore(today);
        return new CashDelegationResponse(delegation.getId(), delegation.getDelegator(), delegation.getDelegate(),
                split(delegation.getPermissions()), delegation.getStartsOn(), delegation.getEndsOn(), delegation.getNote(), active,
                delegation.getCancelledAt());
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(CashRegisterAccessGuard.ZONE).toInstant();
    }

    private static List<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private static String email(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Courriel manquant");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
