package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashDeviceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReferenceValueRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterScheduleRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterStatusRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateResponsibilityTransferRequest;
import com.sni.bokaticowork.features.payment.dto.request.EmergencyAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.UpdateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashFormDefaultsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPendingItemResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReferenceValueResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterAssignmentResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterDetailResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterHistoryResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashResponsibilityTransferResponse;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashCapBehavior;
import com.sni.bokaticowork.features.payment.enums.CashDeviceStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashResponsibilityTransferStatus;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashRegisterDevice;
import com.sni.bokaticowork.features.payment.model.CashRegisterHistory;
import com.sni.bokaticowork.features.payment.model.CashRegisterSchedule;
import com.sni.bokaticowork.features.payment.model.CashReferenceValue;
import com.sni.bokaticowork.features.payment.model.CashResponsibilityTransfer;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashReferenceValueRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterAssignmentRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterDeviceRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterHistoryRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterScheduleRepository;
import com.sni.bokaticowork.features.payment.repository.CashRequestRepository;
import com.sni.bokaticowork.features.payment.repository.CashResponsibilityTransferRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterAdminService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterTypeDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Referentiel des caisses (lot C1) : fiche, cycle de vie, affectations, transferts de
 * responsabilite, postes, horaires, listes preenregistrees et valeurs proposees.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CashRegisterAdminServiceImpl implements CashRegisterAdminService {

    static final int DEFAULT_EMERGENCY_HOURS = 12;
    static final int MAX_EMERGENCY_HOURS = 24;
    private static final String REGISTERS_PATH = "/payments/cash-registers/";

    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final CashRequestRepository requestRepository;
    private final CashRegisterAssignmentRepository assignmentRepository;
    private final CashRegisterHistoryRepository historyRepository;
    private final CashRegisterScheduleRepository scheduleRepository;
    private final CashRegisterDeviceRepository deviceRepository;
    private final CashResponsibilityTransferRepository transferRepository;
    private final CashReferenceValueRepository referenceRepository;
    private final CashReferenceLists referenceLists;
    private final CashRegisterAccessGuard accessGuard;
    private final CashActorResolver actors;
    private final CashEmailNotifier emailNotifier;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ fiche

    @Override
    @Transactional(readOnly = true)
    public CashRegisterDetailResponse get(String registerCode) {
        return detail(register(registerCode));
    }

    @Override
    public CashRegisterDetailResponse update(String registerCode, UpdateCashRegisterRequest request) {
        CashRegister register = register(registerCode);
        assertEditable(register);
        String actor = actors.resolve(null);
        Instant now = Instant.now();
        Instant effectiveAt = request.effectiveAt() == null ? now : request.effectiveAt();
        String reason = trim(request.reason());
        List<CashRegisterHistory> changes = new ArrayList<>();

        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) {
                throw new BadRequestException("Le libelle de la caisse ne peut pas etre vide");
            }
            change(changes, register, "name", register.getName(), request.name().trim(), now, reason, register::setName);
        }
        if (request.locationCode() != null) {
            change(changes, register, "locationCode", register.getLocationCode(), trim(request.locationCode()), now, reason, register::setLocationCode);
        }
        if (request.businessEntityCode() != null) {
            change(changes, register, "businessEntityCode", register.getBusinessEntityCode(), trim(request.businessEntityCode()), now, reason, register::setBusinessEntityCode);
        }
        if (request.parentRegisterCode() != null) {
            String parent = trim(request.parentRegisterCode());
            validateParent(register, parent);
            change(changes, register, "parentRegisterCode", register.getParentRegisterCode(), parent, now, reason, register::setParentRegisterCode);
        }
        if (request.cashControlEnabled() != null) {
            changeValue(changes, register, "cashControlEnabled", register.getCashControlEnabled(), request.cashControlEnabled(), now, reason,
                    register::setCashControlEnabled);
        }
        if (request.maxCashAmount() != null) {
            changeValue(changes, register, "maxCashAmount", register.getMaxCashAmount(), amountOrNull(request.maxCashAmount()), now, reason,
                    register::setMaxCashAmount);
        }
        if (request.acceptedMethods() != null) {
            change(changes, register, "acceptedMethods", register.getAcceptedMethods(), joinMethods(request.acceptedMethods()), now, reason,
                    register::setAcceptedMethods);
        }
        if (request.acceptedCurrencies() != null) {
            String currencies = joinCurrencies(request.acceptedCurrencies());
            change(changes, register, "acceptedCurrencies", register.getAcceptedCurrencies(), currencies == null ? "XAF" : currencies, now, reason,
                    register::setAcceptedCurrencies);
        }
        if (request.capBehavior() != null) {
            changeValue(changes, register, "capBehavior", register.getCapBehavior(), request.capBehavior(), now, reason, register::setCapBehavior);
        }
        if (request.maxOutPerOperation() != null) {
            changeValue(changes, register, "maxOutPerOperation", register.getMaxOutPerOperation(), amountOrNull(request.maxOutPerOperation()), now, reason,
                    register::setMaxOutPerOperation);
        }
        if (request.maxOutPerDay() != null) {
            changeValue(changes, register, "maxOutPerDay", register.getMaxOutPerDay(), amountOrNull(request.maxOutPerDay()), now, reason,
                    register::setMaxOutPerDay);
        }
        if (request.standardFloat() != null) {
            changeValue(changes, register, "standardFloat", register.getStandardFloat(), amountOrNull(request.standardFloat()), now, reason,
                    register::setStandardFloat);
        }
        if (request.alertRecipients() != null) {
            change(changes, register, "alertRecipients", register.getAlertRecipients(), joinEmails(request.alertRecipients()), now, reason,
                    register::setAlertRecipients);
        }
        // Le compte et le journal changent a la date d'effet indiquee : elle est inscrite dans
        // l'historique, que la comptabilite (lot F3) lira pour dater ses ecritures.
        if (request.treasuryAccountNumber() != null) {
            change(changes, register, "treasuryAccountNumber", register.getTreasuryAccountNumber(), trim(request.treasuryAccountNumber()), effectiveAt, reason,
                    register::setTreasuryAccountNumber);
        }
        if (request.defaultJournalCode() != null) {
            change(changes, register, "defaultJournalCode", register.getDefaultJournalCode(), trim(request.defaultJournalCode()), effectiveAt, reason,
                    register::setDefaultJournalCode);
        }
        if (request.schedules() != null) {
            String before = describeSchedules(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc(register.getRegisterCode()));
            replaceSchedules(register.getRegisterCode(), request.schedules());
            String after = describeSchedules(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc(register.getRegisterCode()));
            if (!Objects.equals(before, after)) {
                changes.add(history(register, "schedules", before, after, now, reason));
            }
        }
        if (changes.isEmpty()) {
            return detail(register);
        }
        register.setUpdatedBy(actor);
        registerRepository.save(register);
        changes.forEach(change -> change.setChangedBy(actor));
        historyRepository.saveAll(changes);
        return detail(register);
    }

    @Override
    public CashRegisterDetailResponse suspend(String registerCode, CashRegisterStatusRequest request) {
        CashRegister register = register(registerCode);
        assertNotAutomatic(register);
        if (register.getStatus() != CashRegisterStatus.ACTIVE) {
            throw new BadRequestException("Seule une caisse active peut etre suspendue");
        }
        assertNoActivity(register, "suspendre");
        String reason = referenceLists.describe(CashReferenceLists.SUSPENSION_REASON,
                request == null ? null : request.reasonCode(), request == null ? null : request.reasonText());
        return changeStatus(register, CashRegisterStatus.SUSPENDED, reason);
    }

    @Override
    public CashRegisterDetailResponse reactivate(String registerCode) {
        CashRegister register = register(registerCode);
        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BadRequestException("Une caisse fermee ne se rouvre pas : creez-en une nouvelle");
        }
        if (register.getStatus() == CashRegisterStatus.ACTIVE) {
            return detail(register);
        }
        return changeStatus(register, CashRegisterStatus.ACTIVE, null);
    }

    @Override
    public CashRegisterDetailResponse close(String registerCode, CashRegisterStatusRequest request) {
        CashRegister register = register(registerCode);
        assertNotAutomatic(register);
        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            return detail(register);
        }
        assertNoActivity(register, "fermer");
        BigDecimal lastCount = register.getLastCountAmount();
        if (lastCount == null || lastCount.signum() != 0) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " ne peut etre fermee que si son dernier comptage vaut zero"
                    + (lastCount == null ? " : elle n'a jamais ete comptee" : " (dernier comptage : " + lastCount.stripTrailingZeros().toPlainString() + ")")
                    + ". Remettez les fonds a la caisse mere puis comptez-la.");
        }
        String reason = request == null || !StringUtils.hasText(request.reasonCode()) ? trim(request == null ? null : request.reasonText())
                : referenceLists.describe(CashReferenceLists.SUSPENSION_REASON, request.reasonCode(), request.reasonText());
        CashRegisterDetailResponse response = changeStatus(register, CashRegisterStatus.CLOSED, reason);
        // Les affectations s'eteignent avec la caisse : plus personne n'y est habilite.
        Instant now = Instant.now();
        String actor = actors.resolve(null);
        for (CashRegisterAssignment assignment : assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode())) {
            if (assignment.getRevokedAt() == null && (assignment.getValidTo() == null || assignment.getValidTo().isAfter(now))) {
                assignment.setRevokedAt(now);
                assignment.setRevokedBy(actor);
            }
        }
        return response;
    }

    private CashRegisterDetailResponse changeStatus(CashRegister register, CashRegisterStatus status, String reason) {
        String actor = actors.resolve(null);
        Instant now = Instant.now();
        CashRegisterHistory history = history(register, "status", register.getStatus().name(), status.name(), now, reason);
        history.setChangedBy(actor);
        register.setStatus(status);
        register.setActive(status == CashRegisterStatus.ACTIVE);
        register.setStatusReason(reason);
        register.setUpdatedBy(actor);
        if (status == CashRegisterStatus.CLOSED) {
            register.setClosedAt(now);
        }
        registerRepository.save(register);
        historyRepository.save(history);
        return detail(register);
    }

    /** Ni session en cours ni element en attente : condition pour suspendre ou fermer. */
    private void assertNoActivity(CashRegister register, String verb) {
        if (register.getId() != null) {
            sessionRepository.findBlockingSessionByCashRegisterId(register.getId()).ifPresent(session -> {
                throw new BadRequestException("Impossible de " + verb + " la caisse " + register.getRegisterCode()
                        + " : la session " + session.getSessionNumber() + " est en cours ou en revue");
            });
        }
        if (!movementRepository.findByRegisterCodeAndStatus(register.getRegisterCode(), CashMovementStatus.PENDING).isEmpty()) {
            throw new BadRequestException("Impossible de " + verb + " la caisse " + register.getRegisterCode()
                    + " : des mouvements attendent un visa");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterHistoryResponse> history(String registerCode) {
        register(registerCode);
        return historyRepository.findByRegisterCodeOrderByChangedAtDescIdDesc(registerCode.trim()).stream()
                .map(entry -> new CashRegisterHistoryResponse(entry.getField(), entry.getOldValue(), entry.getNewValue(),
                        entry.getChangedBy(), entry.getChangedAt(), entry.getEffectiveAt(), entry.getReason()))
                .toList();
    }

    // ------------------------------------------------------------ affectations

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterAssignmentResponse> assignments(String registerCode, boolean includeEnded) {
        CashRegister register = register(registerCode);
        Instant now = Instant.now();
        return assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode()).stream()
                .filter(assignment -> includeEnded || isLiveOrFuture(assignment, now))
                .map(assignment -> toResponse(assignment, now))
                .toList();
    }

    /**
     * Remplace la liste des affectations en cours et a venir de la caisse.
     *
     * <p>Une affectation absente de la nouvelle liste est revoquee (jamais supprimee) ; une
     * affectation qui y figure avec la meme personne, la meme qualite et le meme role est mise a
     * jour ; les autres sont creees.</p>
     */
    @Override
    public List<CashRegisterAssignmentResponse> replaceAssignments(String registerCode, List<CashRegisterAssignmentRequest> requests) {
        CashRegister register = register(registerCode);
        assertNotAutomatic(register);
        assertEditable(register);
        String actor = actors.resolve(null);
        Instant now = Instant.now();
        List<CashRegisterAssignmentRequest> wanted = requests == null ? List.of() : requests;

        List<CashRegisterAssignment> current = assignmentRepository
                .findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode()).stream()
                .filter(assignment -> isLiveOrFuture(assignment, now))
                .toList();
        String before = describeAssignments(current, now);

        List<CashRegisterAssignment> kept = new ArrayList<>();
        List<CashRegisterAssignment> candidates = new ArrayList<>(current);
        for (CashRegisterAssignmentRequest request : wanted) {
            String email = email(request.userEmail());
            Instant validFrom = request.validFrom() == null ? now : request.validFrom();
            if (request.validTo() != null && !request.validTo().isAfter(validFrom)) {
                throw new BadRequestException("La fin d'affectation de " + email + " doit suivre son debut");
            }
            if (request.quality() == CashAssignmentQuality.INTERIM && request.validTo() == null) {
                throw new BadRequestException("Un interim a une date de fin : precisez-la pour " + email);
            }
            if (request.quality() == CashAssignmentQuality.HOLDER && request.role() == CashAssignmentRole.VIEWER) {
                throw new BadRequestException("Un titulaire doit pouvoir tenir la caisse : le role lecture ne convient pas");
            }
            CashRegisterAssignment match = candidates.stream()
                    .filter(existing -> existing.getUserEmail().equalsIgnoreCase(email)
                            && existing.getQuality() == request.quality()
                            && existing.getRole() == request.role())
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                candidates.remove(match);
                match.setBackupRank(request.backupRank());
                match.setValidTo(request.validTo());
                if (request.validFrom() != null && match.getValidFrom().isAfter(now)) {
                    match.setValidFrom(request.validFrom());
                }
                match.setNote(trim(request.note()));
                kept.add(match);
            } else {
                kept.add(CashRegisterAssignment.builder()
                        .registerCode(register.getRegisterCode())
                        .userEmail(email)
                        .role(request.role())
                        .quality(request.quality())
                        .backupRank(request.backupRank())
                        .validFrom(validFrom)
                        .validTo(request.validTo())
                        .source("MANUAL")
                        .grantedBy(actor)
                        .note(trim(request.note()))
                        .build());
            }
        }
        assertHolderLimit(register, kept);
        for (CashRegisterAssignment removed : candidates) {
            removed.setRevokedAt(now);
            removed.setRevokedBy(actor);
        }
        assignmentRepository.saveAll(candidates);
        List<CashRegisterAssignment> saved = assignmentRepository.saveAll(kept);
        String after = describeAssignments(saved, now);
        if (!Objects.equals(before, after)) {
            CashRegisterHistory history = history(register, "assignments", before, after, now, null);
            history.setChangedBy(actor);
            historyRepository.save(history);
        }
        return saved.stream().map(assignment -> toResponse(assignment, now)).toList();
    }

    @Override
    public CashRegisterAssignmentResponse emergencyAssignment(String registerCode, EmergencyAssignmentRequest request) {
        CashRegister register = register(registerCode);
        assertNotAutomatic(register);
        assertEditable(register);
        String actor = actors.resolve(null);
        int hours = request.hours() == null ? DEFAULT_EMERGENCY_HOURS : request.hours();
        if (hours < 1 || hours > MAX_EMERGENCY_HOURS) {
            throw new BadRequestException("Une affectation d'urgence dure de 1 a " + MAX_EMERGENCY_HOURS + " heures");
        }
        Instant now = Instant.now();
        String email = email(request.userEmail());
        CashRegisterAssignment assignment = assignmentRepository.save(CashRegisterAssignment.builder()
                .registerCode(register.getRegisterCode())
                .userEmail(email)
                .role(CashAssignmentRole.CASHIER)
                .quality(CashAssignmentQuality.INTERIM)
                .validFrom(now)
                .validTo(now.plus(Duration.ofHours(hours)))
                .emergency(true)
                .source("EMERGENCY")
                .grantedBy(actor)
                .note(request.reason().trim())
                .build());
        CashRegisterHistory history = history(register, "assignments", null,
                "Affectation d'urgence de " + email + " pour " + hours + " h : " + request.reason().trim(), now, request.reason().trim());
        history.setChangedBy(actor);
        historyRepository.save(history);
        emailNotifier.notifyRegisterManager(register, "Affectation d'urgence · " + register.getRegisterCode(),
                actor + " a affecte " + email + " en urgence a la caisse " + register.getName() + " pour " + hours
                        + " heures. Motif : " + request.reason().trim() + ".",
                register.getRegisterCode(), REGISTERS_PATH + register.getRegisterCode());
        return toResponse(assignment, now);
    }

    private void assertHolderLimit(CashRegister register, List<CashRegisterAssignment> assignments) {
        int limit = Math.max(1, CashRegisterTypeDefaults.of(register.getRegisterType()).maxHolders());
        List<CashRegisterAssignment> holders = assignments.stream()
                .filter(assignment -> assignment.getQuality() == CashAssignmentQuality.HOLDER)
                .toList();
        for (CashRegisterAssignment holder : holders) {
            long overlapping = holders.stream().filter(other -> overlaps(holder, other)).count();
            if (overlapping > limit) {
                throw new BadRequestException(limit == 1
                        ? "Une seule personne peut etre titulaire de la caisse " + register.getRegisterCode() + " a un instant donne"
                        : "Le coffre " + register.getRegisterCode() + " a au plus " + limit + " titulaires a un instant donne");
            }
        }
    }

    private static boolean overlaps(CashRegisterAssignment first, CashRegisterAssignment second) {
        Instant firstEnd = first.getValidTo() == null ? Instant.MAX : first.getValidTo();
        Instant secondEnd = second.getValidTo() == null ? Instant.MAX : second.getValidTo();
        return first.getValidFrom().isBefore(secondEnd) && second.getValidFrom().isBefore(firstEnd);
    }

    // ------------------------------------------------------------------ postes

    @Override
    public CashRegisterDetailResponse decideDevice(String registerCode, String deviceCode, CashDeviceDecisionRequest request) {
        CashRegister register = register(registerCode);
        if (request.status() == CashDeviceStatus.PROPOSED) {
            throw new BadRequestException("Decidez d'autoriser ou de refuser le poste");
        }
        String actor = actors.resolve(null);
        String code = deviceCode == null ? null : deviceCode.trim();
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Code de poste manquant");
        }
        CashRegisterDevice device = deviceRepository.findByRegisterCodeAndDeviceCode(register.getRegisterCode(), code)
                .orElseGet(() -> CashRegisterDevice.builder()
                        .registerCode(register.getRegisterCode())
                        .deviceCode(code)
                        .firstSeenBy(actor)
                        .build());
        String before = device.getStatus() == null ? null : device.getStatus().name();
        device.setStatus(request.status());
        if (StringUtils.hasText(request.label())) {
            device.setLabel(request.label().trim());
        }
        device.setDecidedAt(Instant.now());
        device.setDecidedBy(actor);
        deviceRepository.save(device);
        CashRegisterHistory history = history(register, "device:" + code, before, request.status().name(), Instant.now(), null);
        history.setChangedBy(actor);
        historyRepository.save(history);
        return detail(register);
    }

    // ------------------------------------------------- transfert de responsabilite

    @Override
    @Transactional(readOnly = true)
    public List<CashPendingItemResponse> pendingItems(String registerCode) {
        return pendingItems(register(registerCode));
    }

    private List<CashPendingItemResponse> pendingItems(CashRegister register) {
        List<CashPendingItemResponse> items = new ArrayList<>();
        for (CashSession session : sessionRepository.findByRegisterCodeAndStatuses(register.getRegisterCode(),
                List.of(CashSessionStatus.OPEN, CashSessionStatus.SUSPENDED, CashSessionStatus.CLOSING_REVIEW))) {
            boolean open = session.getStatus() != CashSessionStatus.CLOSING_REVIEW;
            items.add(new CashPendingItemResponse(
                    open ? "OPEN_SESSION" : "VARIANCE_REVIEW",
                    session.getSessionNumber(),
                    open ? "Session ouverte par " + session.getOpenedBy() + " : a clore avant la remise"
                            : "Ecart de cloture en revue",
                    open ? null : session.getVarianceAmount(),
                    open,
                    open ? "MUST_SETTLE" : "TRANSFERRED"));
        }
        requestRepository.findByRegisterCodeAndStatus(register.getRegisterCode(), CashRequestStatus.PENDING)
                .forEach(request -> items.add(new CashPendingItemResponse("PENDING_REQUEST", request.getRequestNumber(),
                        "Demande " + request.getRequestType().name() + " en attente de decision", request.getAmount(), false, "TRANSFERRED")));
        movementRepository.findByRegisterCodeAndStatus(register.getRegisterCode(), CashMovementStatus.PENDING)
                .forEach(movement -> items.add(new CashPendingItemResponse("PENDING_MOVEMENT", movement.getMovementNumber(),
                        "Mouvement " + movement.getMovementType().name() + " en attente de visa", movement.getAmount(), false, "TRANSFERRED")));
        return items;
    }

    @Override
    public CashResponsibilityTransferResponse createResponsibilityTransfer(String registerCode, CreateResponsibilityTransferRequest request) {
        CashRegister register = register(registerCode);
        assertNotAutomatic(register);
        assertEditable(register);
        String actor = actors.resolve(null);
        Instant now = Instant.now();
        Instant effectiveAt = request.effectiveAt() == null ? now : request.effectiveAt();
        boolean permanent = Boolean.TRUE.equals(request.permanent());
        if (!permanent && (request.endsAt() == null || !request.endsAt().isAfter(effectiveAt))) {
            throw new BadRequestException("Un transfert temporaire a une date de fin posterieure a sa date d'effet");
        }
        String toUser = email(request.toUserEmail());
        CashRegisterAssignment currentHolder = currentHolder(register.getRegisterCode(), effectiveAt.isBefore(now) ? now : effectiveAt)
                .orElse(null);
        String fromUser = currentHolder == null ? null : currentHolder.getUserEmail();
        if (fromUser != null && fromUser.equalsIgnoreCase(toUser)) {
            throw new BadRequestException(toUser + " est deja titulaire de la caisse");
        }
        boolean outgoingAbsent = Boolean.TRUE.equals(request.outgoingAbsent()) || fromUser == null;
        String witness = request.witnessEmail() == null ? null : email(request.witnessEmail());
        if (outgoingAbsent && fromUser != null) {
            if (witness == null) {
                throw new BadRequestException("Le titulaire sortant est absent : le comptage se fait avec un temoin, indiquez-le");
            }
            if (witness.equalsIgnoreCase(actor) || witness.equalsIgnoreCase(toUser)) {
                throw new BadRequestException("Le temoin doit etre une autre personne que le superviseur et le nouveau titulaire");
            }
        }
        String reason = referenceLists.describe(CashReferenceLists.RESPONSIBILITY_TRANSFER_REASON, request.reasonCode(), request.reasonText());

        List<CashPendingItemResponse> items = pendingItems(register);
        List<String> blocking = items.stream().filter(item -> Boolean.TRUE.equals(item.blocking()))
                .map(CashPendingItemResponse::reference).toList();
        if (!blocking.isEmpty()) {
            throw new BadRequestException("A solder avant la remise de la caisse : " + String.join(", ", blocking)
                    + ". Faites clore la session (ou passez-la) puis recommencez.");
        }

        String number = sequenceGenerator.next("cash_responsibility_transfer");
        CashAssignmentRole role = currentHolder == null ? CashAssignmentRole.CASHIER : currentHolder.getRole();
        if (permanent) {
            if (currentHolder != null) {
                currentHolder.setValidTo(effectiveAt.isAfter(currentHolder.getValidFrom()) ? effectiveAt : currentHolder.getValidFrom().plusSeconds(1));
                assignmentRepository.save(currentHolder);
                if (Boolean.TRUE.equals(request.keepFormerAsBackup())) {
                    assignmentRepository.save(CashRegisterAssignment.builder()
                            .registerCode(register.getRegisterCode()).userEmail(fromUser)
                            .role(currentHolder.getRole()).quality(CashAssignmentQuality.BACKUP).backupRank(1)
                            .validFrom(effectiveAt).source("RESPONSIBILITY_TRANSFER").sourceReference(number)
                            .grantedBy(actor).build());
                }
            }
            // L'ancienne affectation de remplacant du nouveau titulaire s'eteint : il devient titulaire.
            assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode()).stream()
                    .filter(existing -> existing.getUserEmail().equalsIgnoreCase(toUser)
                            && existing.getQuality() == CashAssignmentQuality.BACKUP
                            && existing.isEffectiveAt(effectiveAt))
                    .forEach(existing -> {
                        existing.setValidTo(effectiveAt.isAfter(existing.getValidFrom()) ? effectiveAt : existing.getValidFrom().plusSeconds(1));
                        assignmentRepository.save(existing);
                    });
            assignmentRepository.save(CashRegisterAssignment.builder()
                    .registerCode(register.getRegisterCode()).userEmail(toUser).role(role)
                    .quality(CashAssignmentQuality.HOLDER).validFrom(effectiveAt)
                    .source("RESPONSIBILITY_TRANSFER").sourceReference(number).grantedBy(actor).build());
        } else {
            assignmentRepository.save(CashRegisterAssignment.builder()
                    .registerCode(register.getRegisterCode()).userEmail(toUser).role(role)
                    .quality(CashAssignmentQuality.INTERIM).validFrom(effectiveAt).validTo(request.endsAt())
                    .source("RESPONSIBILITY_TRANSFER").sourceReference(number).grantedBy(actor).build());
        }

        CashResponsibilityTransfer transfer = CashResponsibilityTransfer.builder()
                .transferNumber(number)
                .registerCode(register.getRegisterCode())
                .fromUser(fromUser)
                .toUser(toUser)
                .reasonCode(request.reasonCode().trim().toUpperCase(Locale.ROOT))
                .reasonText(reason)
                .permanent(permanent)
                .effectiveAt(effectiveAt)
                .endsAt(permanent ? null : request.endsAt())
                .expectedAmount(register.getLastCountAmount())
                .countedAmount(request.countedAmount() == null ? null : request.countedAmount().setScale(4, RoundingMode.HALF_UP))
                .witness(outgoingAbsent ? witness : null)
                .pendingItems(writeItems(items))
                .status(CashResponsibilityTransferStatus.PENDING_SIGNATURES)
                .createdBy(actor)
                .build();
        if (outgoingAbsent) {
            // Sortant absent : le superviseur qui organise la remise signe a sa place, avec le temoin.
            transfer.setOutgoingSignedBy(actor);
            transfer.setOutgoingSignedAt(now);
        }
        transfer = transferRepository.save(transfer);

        CashRegisterHistory history = history(register, "holder", fromUser,
                toUser + (permanent ? "" : " (interim jusqu'au " + request.endsAt() + ")"), now, reason);
        history.setEffectiveAt(effectiveAt);
        history.setChangedBy(actor);
        historyRepository.save(history);

        String message = "La caisse " + register.getName() + " (" + register.getRegisterCode() + ") passe de "
                + (fromUser == null ? "aucun titulaire" : fromUser) + " a " + toUser
                + (permanent ? " a titre definitif" : " jusqu'au " + request.endsAt())
                + ". Motif : " + reason + ". PV " + number + " a signer.";
        emailNotifier.notifyUser(toUser, "Remise de caisse a signer · " + number, message, number, REGISTERS_PATH + register.getRegisterCode());
        if (fromUser != null) {
            emailNotifier.notifyUser(fromUser, "Remise de caisse · " + number, message, number, REGISTERS_PATH + register.getRegisterCode());
        }
        return toResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashResponsibilityTransferResponse> responsibilityTransfers(String registerCode) {
        CashRegister register = register(registerCode);
        return transferRepository.findByRegisterCodeOrderByEffectiveAtDesc(register.getRegisterCode()).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public CashResponsibilityTransferResponse signResponsibilityTransfer(String transferNumber) {
        CashResponsibilityTransfer transfer = transferRepository.findByTransferNumber(transferNumber == null ? null : transferNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("PV de remise introuvable : " + transferNumber));
        if (transfer.getStatus() != CashResponsibilityTransferStatus.PENDING_SIGNATURES) {
            throw new BadRequestException("Ce PV n'attend plus de signature");
        }
        String actor = actors.resolve(null);
        Instant now = Instant.now();
        boolean signed = false;
        if (CashActorResolver.samePerson(actor, transfer.getToUser()) && transfer.getIncomingSignedAt() == null) {
            transfer.setIncomingSignedBy(actor);
            transfer.setIncomingSignedAt(now);
            signed = true;
        } else if (transfer.getOutgoingSignedAt() == null
                && (CashActorResolver.samePerson(actor, transfer.getFromUser()) || CashActorResolver.samePerson(actor, transfer.getWitness()))) {
            transfer.setOutgoingSignedBy(actor);
            transfer.setOutgoingSignedAt(now);
            signed = true;
        } else if (transfer.getWitness() != null && CashActorResolver.samePerson(actor, transfer.getWitness())) {
            signed = true;
        }
        if (!signed) {
            throw new ForbiddenException("Seuls le titulaire sortant (ou le temoin) et le nouveau titulaire signent ce PV");
        }
        if (transfer.getIncomingSignedAt() != null && transfer.getOutgoingSignedAt() != null) {
            transfer.setStatus(CashResponsibilityTransferStatus.SIGNED);
        }
        return toResponse(transferRepository.save(transfer));
    }

    private Optional<CashRegisterAssignment> currentHolder(String registerCode, Instant at) {
        return assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(registerCode).stream()
                .filter(assignment -> assignment.getQuality() == CashAssignmentQuality.HOLDER && assignment.isEffectiveAt(at))
                .findFirst();
    }

    // ------------------------------------------------------------------ listes

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<CashReferenceValueResponse>> referenceLists(boolean includeInactive) {
        Map<String, List<CashReferenceValueResponse>> lists = new LinkedHashMap<>();
        for (CashReferenceValue value : referenceRepository.findAllByOrderByListCodeAscSortOrderAscLabelAsc()) {
            if (includeInactive || Boolean.TRUE.equals(value.getActive())) {
                lists.computeIfAbsent(value.getListCode(), key -> new ArrayList<>()).add(toResponse(value));
            }
        }
        return lists;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashReferenceValueResponse> referenceList(String listCode, boolean includeInactive) {
        return referenceRepository.findByListCodeOrderBySortOrderAscLabelAsc(listCodeOf(listCode)).stream()
                .filter(value -> includeInactive || Boolean.TRUE.equals(value.getActive()))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Remplace le contenu d'une liste. Une valeur absente n'est pas supprimee mais desactivee :
     * les motifs deja enregistres gardent leur libelle.
     */
    @Override
    public List<CashReferenceValueResponse> replaceReferenceList(String listCode, List<CashReferenceValueRequest> values) {
        String list = listCodeOf(listCode);
        String actor = actors.resolve(null);
        Map<String, CashReferenceValue> existing = referenceRepository.findByListCodeOrderBySortOrderAscLabelAsc(list).stream()
                .collect(Collectors.toMap(CashReferenceValue::getCode, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        List<CashReferenceValue> saved = new ArrayList<>();
        int order = 10;
        for (CashReferenceValueRequest request : values == null ? List.<CashReferenceValueRequest>of() : values) {
            String code = request.code().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
            CashReferenceValue value = existing.remove(code);
            if (value == null) {
                value = CashReferenceValue.builder().listCode(list).code(code).systemValue(false).build();
            }
            value.setLabel(request.label().trim());
            value.setSortOrder(request.sortOrder() == null ? order : request.sortOrder());
            value.setActive(request.active() == null || request.active());
            value.setRequiresText(CashReferenceLists.OTHER.equals(code) || Boolean.TRUE.equals(request.requiresText()));
            value.setUpdatedBy(actor);
            saved.add(value);
            order += 10;
        }
        for (CashReferenceValue removed : existing.values()) {
            removed.setActive(false);
            removed.setUpdatedBy(actor);
            saved.add(removed);
        }
        referenceRepository.saveAll(saved);
        return referenceList(list, true);
    }

    // ------------------------------------------------------- valeurs proposees

    @Override
    @Transactional(readOnly = true)
    public CashFormDefaultsResponse formDefaults(String context, Map<String, String> parameters) {
        String resolved = context == null ? "" : context.trim().toUpperCase(Locale.ROOT);
        Map<String, String> params = parameters == null ? Map.of() : parameters;
        return switch (resolved) {
            case "REGISTER_CREATE" -> registerCreateDefaults(params);
            case "SESSION_OPEN" -> sessionOpenDefaults();
            default -> throw new BadRequestException("Contexte inconnu : " + context
                    + " (contextes disponibles : REGISTER_CREATE, SESSION_OPEN)");
        };
    }

    private CashFormDefaultsResponse registerCreateDefaults(Map<String, String> params) {
        CashRegisterType type = parseType(params.get("registerType"));
        String location = trim(params.get("locationCode"));
        CashRegisterTypeDefaults.Defaults defaults = CashRegisterTypeDefaults.of(type);
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, String> origins = new LinkedHashMap<>();
        Map<String, List<CashFormDefaultsResponse.Choice>> choices = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        values.put("registerType", type.name());
        origins.put("registerType", "LIST");
        List<CashRegister> siteRegisters = location == null ? List.of() : registerRepository.findByLocationCodeOrderByNameAsc(location);
        Optional<CashRegister> main = siteRegisters.stream()
                .filter(candidate -> candidate.getRegisterType() == CashRegisterType.MAIN && candidate.getStatus() != CashRegisterStatus.CLOSED)
                .findFirst();
        if (type != CashRegisterType.MAIN && type != CashRegisterType.AUTOMATIC) {
            main.ifPresentOrElse(parent -> {
                values.put("parentRegisterCode", parent.getRegisterCode());
                origins.put("parentRegisterCode", "DEDUCED");
            }, () -> {
                if (location != null) {
                    warnings.add("Le site " + location + " n'a pas encore de caisse principale : creez-la pour servir de caisse mere");
                }
            });
        }
        siteRegisters.stream().map(CashRegister::getBusinessEntityCode).filter(StringUtils::hasText).findFirst().ifPresent(entity -> {
            values.put("businessEntityCode", entity);
            origins.put("businessEntityCode", "DEDUCED");
        });
        if (type == CashRegisterType.MAIN && main.isPresent()) {
            warnings.add("Le site " + location + " a deja une caisse principale : " + main.get().getRegisterCode());
        }
        putInherited(values, origins, "capBehavior", defaults.capBehavior() == null ? null : defaults.capBehavior().name());
        putInherited(values, origins, "maxOutPerOperation", defaults.maxOutPerOperation());
        putInherited(values, origins, "maxOutPerDay", defaults.maxOutPerDay());
        putInherited(values, origins, "acceptedMethods", defaults.acceptedMethods().stream().map(Enum::name).toList());
        putInherited(values, origins, "acceptedCurrencies", defaults.acceptedCurrencies());
        values.put("registerCode", null);
        origins.put("registerCode", "GENERATED");

        choices.put("registerType", Arrays.stream(CashRegisterType.values())
                .filter(value -> value != CashRegisterType.AUTOMATIC)
                .map(value -> new CashFormDefaultsResponse.Choice(value.name(), typeLabel(value))).toList());
        choices.put("acceptedMethods", List.of(PaymentMethod.CASH, PaymentMethod.CHEQUE, PaymentMethod.BANK_TRANSFER,
                        PaymentMethod.MOBILE_MONEY_OFFLINE, PaymentMethod.CARD).stream()
                .map(method -> new CashFormDefaultsResponse.Choice(method.name(), methodLabel(method))).toList());
        choices.put("capBehavior", List.of(
                new CashFormDefaultsResponse.Choice(CashCapBehavior.ALERT.name(), "Alerte"),
                new CashFormDefaultsResponse.Choice(CashCapBehavior.BLOCK.name(), "Blocage")));
        choices.put("parentRegisterCode", siteRegisters.stream()
                .filter(candidate -> candidate.getStatus() != CashRegisterStatus.CLOSED && candidate.getRegisterType() != CashRegisterType.AUTOMATIC)
                .map(candidate -> new CashFormDefaultsResponse.Choice(candidate.getRegisterCode(), candidate.getName())).toList());
        return new CashFormDefaultsResponse("REGISTER_CREATE", values, origins, choices, warnings);
    }

    private CashFormDefaultsResponse sessionOpenDefaults() {
        String actor = actors.current();
        Instant now = Instant.now();
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, String> origins = new LinkedHashMap<>();
        Map<String, List<CashFormDefaultsResponse.Choice>> choices = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<CashRegisterAssignment> mine = accessGuard.effectiveAssignments(actor, now).stream()
                .filter(assignment -> assignment.getRole() != CashAssignmentRole.VIEWER)
                .toList();
        Map<String, CashRegister> registers = registerRepository.findByRegisterCodeIn(
                        mine.stream().map(CashRegisterAssignment::getRegisterCode).distinct().toList()).stream()
                .filter(register -> register.getStatus() == CashRegisterStatus.ACTIVE
                        && register.getRegisterType() != CashRegisterType.SAFE
                        && register.getRegisterType() != CashRegisterType.AUTOMATIC)
                .collect(Collectors.toMap(CashRegister::getRegisterCode, Function.identity(), (first, second) -> first));
        List<CashRegister> ordered = mine.stream().map(assignment -> registers.get(assignment.getRegisterCode()))
                .filter(Objects::nonNull).distinct().toList();
        choices.put("registerCode", ordered.stream()
                .map(register -> new CashFormDefaultsResponse.Choice(register.getRegisterCode(), register.getName())).toList());
        values.put("openedBy", actor);
        origins.put("openedBy", "DEDUCED");
        if (ordered.isEmpty()) {
            warnings.add("Vous n'etes affecte a aucune caisse active : demandez une affectation a un responsable");
            return new CashFormDefaultsResponse("SESSION_OPEN", values, origins, choices, warnings);
        }
        CashRegister proposed = ordered.get(0);
        values.put("registerCode", proposed.getRegisterCode());
        origins.put("registerCode", ordered.size() == 1 ? "DEDUCED" : "LIST");
        values.put("expectedOpeningAmount", proposed.getLastCountAmount());
        origins.put("expectedOpeningAmount", "DEDUCED");
        values.put("lastCountAt", proposed.getLastCountAt());
        values.put("withinSchedule", accessGuard.withinSchedule(proposed.getRegisterCode(), now));
        if (!accessGuard.withinSchedule(proposed.getRegisterCode(), now)) {
            warnings.add("La caisse " + proposed.getRegisterCode() + " est hors de ses horaires : un motif sera demande");
            choices.put("outOfHoursReasonCode", referenceList(CashReferenceLists.OUT_OF_HOURS_REASON, false).stream()
                    .map(value -> new CashFormDefaultsResponse.Choice(value.code(), value.label())).toList());
        }
        if (proposed.getLastCountAt() == null) {
            warnings.add("La caisse " + proposed.getRegisterCode() + " n'a jamais ete comptee");
        }
        return new CashFormDefaultsResponse("SESSION_OPEN", values, origins, choices, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterDetailResponse> myRegisters() {
        String actor = actors.current();
        Instant now = Instant.now();
        List<String> codes = accessGuard.effectiveAssignments(actor, now).stream()
                .map(CashRegisterAssignment::getRegisterCode).distinct().toList();
        Map<String, CashRegister> registers = registerRepository.findByRegisterCodeIn(codes).stream()
                .collect(Collectors.toMap(CashRegister::getRegisterCode, Function.identity(), (first, second) -> first));
        return codes.stream().map(registers::get).filter(Objects::nonNull).map(this::detail).toList();
    }

    // ------------------------------------------------------------------ outils

    CashRegisterDetailResponse detail(CashRegister register) {
        Instant now = Instant.now();
        List<CashRegisterAssignment> assignments = assignmentRepository
                .findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc(register.getRegisterCode());
        String holder = assignments.stream()
                .filter(assignment -> assignment.getQuality() == CashAssignmentQuality.HOLDER && assignment.isEffectiveAt(now))
                .map(CashRegisterAssignment::getUserEmail)
                .findFirst().orElse(null);
        List<CashRegisterDetailResponse.Schedule> schedules = scheduleRepository
                .findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc(register.getRegisterCode()).stream()
                .map(schedule -> new CashRegisterDetailResponse.Schedule(schedule.getDayOfWeek(), schedule.getOpensAt(), schedule.getClosesAt()))
                .toList();
        List<CashRegisterDetailResponse.Device> devices = deviceRepository
                .findByRegisterCodeOrderByFirstSeenAtAsc(register.getRegisterCode()).stream()
                .map(device -> new CashRegisterDetailResponse.Device(device.getDeviceCode(), device.getLabel(), device.getStatus(),
                        device.getFirstSeenAt(), device.getFirstSeenBy(), device.getDecidedAt(), device.getDecidedBy()))
                .toList();
        return new CashRegisterDetailResponse(
                register.getRegisterCode(),
                register.getName(),
                register.getRegisterType(),
                register.getStatus(),
                register.getStatusReason(),
                register.getLocationCode(),
                register.getBusinessEntityCode(),
                register.getDeviceCode(),
                register.getParentRegisterCode(),
                register.getSystemManaged(),
                register.getRestrictedToMethod(),
                split(register.getAcceptedMethods()),
                split(register.getAcceptedCurrencies()),
                register.getCashControlEnabled(),
                register.getMaxCashAmount(),
                register.getCapBehavior(),
                register.getMaxOutPerOperation(),
                register.getMaxOutPerDay(),
                register.getStandardFloat(),
                recipients(register),
                register.getTreasuryAccountNumber(),
                register.getDefaultJournalCode(),
                register.getLastCountAt(),
                register.getLastCountAmount(),
                holder,
                assignments.stream().filter(assignment -> isLiveOrFuture(assignment, now)).map(assignment -> toResponse(assignment, now)).toList(),
                schedules,
                devices,
                register.getCreatedAt(),
                register.getUpdatedAt(),
                register.getUpdatedBy(),
                register.getClosedAt());
    }

    private List<String> recipients(CashRegister register) {
        List<String> recipients = split(register.getAlertRecipients());
        if (recipients.isEmpty() && StringUtils.hasText(register.getManagerEmail())) {
            return List.of(register.getManagerEmail().trim());
        }
        return recipients;
    }

    private CashRegisterAssignmentResponse toResponse(CashRegisterAssignment assignment, Instant now) {
        return new CashRegisterAssignmentResponse(assignment.getId(), assignment.getRegisterCode(), assignment.getUserEmail(),
                assignment.getRole(), assignment.getQuality(), assignment.getBackupRank(), assignment.getValidFrom(),
                assignment.getValidTo(), assignment.getEmergency(), assignment.getSource(), assignment.getSourceReference(),
                assignment.getGrantedBy(), assignment.getNote(), assignment.getRevokedAt(), assignment.getRevokedBy(),
                assignment.isEffectiveAt(now));
    }

    private CashResponsibilityTransferResponse toResponse(CashResponsibilityTransfer transfer) {
        BigDecimal difference = transfer.getCountedAmount() == null || transfer.getExpectedAmount() == null ? null
                : transfer.getCountedAmount().subtract(transfer.getExpectedAmount());
        return new CashResponsibilityTransferResponse(transfer.getTransferNumber(), transfer.getRegisterCode(), transfer.getFromUser(),
                transfer.getToUser(), transfer.getReasonCode(),
                referenceLists.label(CashReferenceLists.RESPONSIBILITY_TRANSFER_REASON, transfer.getReasonCode()),
                transfer.getReasonText(), transfer.getPermanent(), transfer.getEffectiveAt(), transfer.getEndsAt(),
                transfer.getExpectedAmount(), transfer.getCountedAmount(), difference, transfer.getWitness(),
                readItems(transfer.getPendingItems()), transfer.getStatus(), transfer.getOutgoingSignedBy(), transfer.getOutgoingSignedAt(),
                transfer.getIncomingSignedBy(), transfer.getIncomingSignedAt(), transfer.getCreatedBy(), transfer.getCreatedAt());
    }

    private CashReferenceValueResponse toResponse(CashReferenceValue value) {
        return new CashReferenceValueResponse(value.getListCode(), value.getCode(), value.getLabel(), value.getSortOrder(),
                value.getActive(), value.getRequiresText(), value.getSystemValue(), value.getUsageCount());
    }

    private String writeItems(List<CashPendingItemResponse> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'enregistrer les elements en cours", exception);
        }
    }

    private List<CashPendingItemResponse> readItems(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, CashPendingItemResponse[].class));
        } catch (Exception exception) {
            return List.of();
        }
    }

    private void replaceSchedules(String registerCode, List<CashRegisterScheduleRequest> requests) {
        for (CashRegisterScheduleRequest request : requests) {
            if (request.dayOfWeek() == null || request.dayOfWeek() < 1 || request.dayOfWeek() > 7
                    || request.opensAt() == null || request.closesAt() == null || !request.closesAt().isAfter(request.opensAt())) {
                throw new BadRequestException("Horaire invalide : jour de 1 (lundi) a 7 (dimanche), fermeture apres l'ouverture");
            }
        }
        scheduleRepository.deleteByRegisterCode(registerCode);
        scheduleRepository.flush();
        scheduleRepository.saveAll(requests.stream()
                .sorted(Comparator.comparing(CashRegisterScheduleRequest::dayOfWeek).thenComparing(CashRegisterScheduleRequest::opensAt))
                .map(request -> CashRegisterSchedule.builder().registerCode(registerCode).dayOfWeek(request.dayOfWeek())
                        .opensAt(request.opensAt()).closesAt(request.closesAt()).build())
                .toList());
    }

    /** Horaires a la creation d'une caisse (appele par le service des caisses). */
    public void initializeRegister(CashRegister register, List<CashRegisterScheduleRequest> schedules, String holderEmail,
                                   List<String> backupEmails) {
        String actor = actors.resolve("SYSTEM");
        if (schedules != null && !schedules.isEmpty()) {
            replaceSchedules(register.getRegisterCode(), schedules);
        }
        Instant now = Instant.now();
        List<CashRegisterAssignment> created = new ArrayList<>();
        if (StringUtils.hasText(holderEmail)) {
            created.add(CashRegisterAssignment.builder().registerCode(register.getRegisterCode()).userEmail(email(holderEmail))
                    .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.HOLDER).validFrom(now)
                    .source("MANUAL").grantedBy(actor).build());
        }
        int rank = 1;
        for (String backup : backupEmails == null ? List.<String>of() : backupEmails) {
            if (!StringUtils.hasText(backup) || (holderEmail != null && backup.trim().equalsIgnoreCase(holderEmail.trim()))) {
                continue;
            }
            created.add(CashRegisterAssignment.builder().registerCode(register.getRegisterCode()).userEmail(email(backup))
                    .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.BACKUP).backupRank(rank++).validFrom(now)
                    .source("MANUAL").grantedBy(actor).build());
        }
        if (!created.isEmpty()) {
            assignmentRepository.saveAll(created);
        }
        CashRegisterHistory history = history(register, "created", null,
                register.getRegisterType() + " " + register.getName(), now, null);
        history.setChangedBy(actor);
        historyRepository.save(history);
    }

    /** Caisse mere par defaut : la caisse principale du site, sauf pour une caisse principale. */
    public String defaultParent(CashRegisterType type, String locationCode) {
        if (type == CashRegisterType.MAIN || type == CashRegisterType.AUTOMATIC || !StringUtils.hasText(locationCode)) {
            return null;
        }
        return registerRepository.findByLocationCodeOrderByNameAsc(locationCode.trim()).stream()
                .filter(candidate -> candidate.getRegisterType() == CashRegisterType.MAIN && candidate.getStatus() != CashRegisterStatus.CLOSED)
                .map(CashRegister::getRegisterCode)
                .findFirst().orElse(null);
    }

    public void validateParent(CashRegister register, String parentCode) {
        if (parentCode == null) {
            return;
        }
        if (parentCode.equalsIgnoreCase(register.getRegisterCode())) {
            throw new BadRequestException("Une caisse ne peut pas etre sa propre caisse mere");
        }
        CashRegister parent = registerRepository.findByRegisterCode(parentCode)
                .orElseThrow(() -> new BadRequestException("Caisse mere inconnue : " + parentCode));
        if (parent.getStatus() == CashRegisterStatus.CLOSED || parent.getRegisterType() == CashRegisterType.AUTOMATIC) {
            throw new BadRequestException("La caisse " + parentCode + " ne peut pas etre caisse mere");
        }
        // Pas de cycle : on remonte la chaine des caisses meres.
        String cursor = parent.getParentRegisterCode();
        int guard = 0;
        while (cursor != null && guard++ < 20) {
            if (cursor.equalsIgnoreCase(register.getRegisterCode())) {
                throw new BadRequestException("Ce choix de caisse mere creerait une boucle");
            }
            cursor = registerRepository.findByRegisterCode(cursor).map(CashRegister::getParentRegisterCode).orElse(null);
        }
    }

    private CashRegister register(String registerCode) {
        if (!StringUtils.hasText(registerCode)) {
            throw new BadRequestException("Code de caisse manquant");
        }
        return registerRepository.findByRegisterCode(registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
    }

    private void assertEditable(CashRegister register) {
        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " est fermee : elle reste consultable mais ne se modifie plus");
        }
    }

    private void assertNotAutomatic(CashRegister register) {
        if (Boolean.TRUE.equals(register.getSystemManaged()) || register.getRegisterType() == CashRegisterType.AUTOMATIC) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " est tenue par le systeme");
        }
    }

    private static boolean isLiveOrFuture(CashRegisterAssignment assignment, Instant now) {
        return assignment.getRevokedAt() == null && (assignment.getValidTo() == null || assignment.getValidTo().isAfter(now));
    }

    private CashRegisterHistory history(CashRegister register, String field, String oldValue, String newValue, Instant effectiveAt, String reason) {
        return CashRegisterHistory.builder()
                .registerCode(register.getRegisterCode())
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .effectiveAt(effectiveAt)
                .reason(reason)
                .build();
    }

    private void change(List<CashRegisterHistory> changes, CashRegister register, String field, String oldValue, String newValue,
                        Instant effectiveAt, String reason, java.util.function.Consumer<String> setter) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(history(register, field, oldValue, newValue, effectiveAt, reason));
            setter.accept(newValue);
        }
    }

    private <T> void changeValue(List<CashRegisterHistory> changes, CashRegister register, String field, T oldValue, T newValue,
                                 Instant effectiveAt, String reason, java.util.function.Consumer<T> setter) {
        boolean same = oldValue instanceof BigDecimal oldAmount && newValue instanceof BigDecimal newAmount
                ? oldAmount.compareTo(newAmount) == 0
                : Objects.equals(oldValue, newValue);
        if (!same) {
            changes.add(history(register, field, text(oldValue), text(newValue), effectiveAt, reason));
            setter.accept(newValue);
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal amount) {
            return amount.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private static BigDecimal amountOrNull(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.signum() < 0) {
            throw new BadRequestException("Un montant de caisse ne peut pas etre negatif");
        }
        // Zero efface la limite.
        return amount.signum() == 0 ? null : amount.setScale(4, RoundingMode.HALF_UP);
    }

    static String joinMethods(List<PaymentMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        return methods.stream().filter(Objects::nonNull).map(Enum::name).distinct().collect(Collectors.joining(","));
    }

    static String joinCurrencies(List<String> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            return null;
        }
        return currencies.stream().filter(StringUtils::hasText).map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct().collect(Collectors.joining(","));
    }

    static String joinEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return null;
        }
        String joined = emails.stream().filter(StringUtils::hasText).map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct().collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private static List<String> split(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private static String describeSchedules(List<CashRegisterSchedule> schedules) {
        if (schedules.isEmpty()) {
            return null;
        }
        return schedules.stream()
                .map(schedule -> schedule.getDayOfWeek() + " " + schedule.getOpensAt() + "-" + schedule.getClosesAt())
                .collect(Collectors.joining(", "));
    }

    private static String describeAssignments(List<CashRegisterAssignment> assignments, Instant now) {
        if (assignments.isEmpty()) {
            return null;
        }
        return assignments.stream()
                .filter(assignment -> isLiveOrFuture(assignment, now))
                .sorted(Comparator.comparing((CashRegisterAssignment assignment) -> assignment.getQuality().name())
                        .thenComparing(CashRegisterAssignment::getUserEmail))
                .map(assignment -> assignment.getUserEmail() + " " + assignment.getQuality() + "/" + assignment.getRole()
                        + (assignment.getValidTo() == null ? "" : " jusqu'au " + assignment.getValidTo()))
                .collect(Collectors.joining(", "));
    }

    private static String email(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Courriel de la personne manquant");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String listCodeOf(String listCode) {
        if (!StringUtils.hasText(listCode)) {
            throw new BadRequestException("Code de liste manquant");
        }
        return listCode.trim().toUpperCase(Locale.ROOT);
    }

    private static CashRegisterType parseType(String value) {
        if (!StringUtils.hasText(value)) {
            return CashRegisterType.COUNTER;
        }
        try {
            return CashRegisterType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Type de caisse inconnu : " + value);
        }
    }

    private static void putInherited(Map<String, Object> values, Map<String, String> origins, String field, Object value) {
        values.put(field, value);
        origins.put(field, "INHERITED");
    }

    static String typeLabel(CashRegisterType type) {
        return switch (type) {
            case COUNTER -> "Guichet";
            case MAIN -> "Caisse principale";
            case PETTY_CASH -> "Petite caisse a fonds fixe";
            case SAFE -> "Coffre";
            case AUTOMATIC -> "Caisse automatique";
        };
    }

    private static String methodLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Especes";
            case CHEQUE -> "Cheque";
            case BANK_TRANSFER -> "Virement declare";
            case MOBILE_MONEY_OFFLINE -> "Mobile money hors plateforme";
            case CARD -> "Carte";
            default -> method.name();
        };
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
