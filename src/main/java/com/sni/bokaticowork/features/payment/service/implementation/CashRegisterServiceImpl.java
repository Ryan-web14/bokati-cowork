package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.ApproveCashVarianceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CloseCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.dto.request.OpenCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashMetricsOverviewResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterMetricsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class CashRegisterServiceImpl implements CashRegisterService {

    private static final String CASH_CURRENCY = "XAF";
    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String PAYMENT_TRANSACTION_REFERENCE = "PAYMENT_TRANSACTION";
    private static final String AUTO_BUSINESS_CODE = "BIZ-AUTO-PAYMENT";
    private static final String AUTO_LOCATION_CODE = "AUTO";
    private static final BigDecimal DEFAULT_AUTO_SESSION_LIMIT = new BigDecimal("10000000");
    private static final DateTimeFormatter CASH_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashSessionSummarySupport summarySupport;
    private final CashRegisterMapper mapper;
    private final CashEmailNotifier emailNotifier;
    private final com.sni.bokaticowork.features.payment.repository.CashMovementAttachmentRepository attachmentRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.sni.bokaticowork.features.payment.service.support.CashActorResolver actors;
    private final com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard accessGuard;
    private final CashRegisterAdminServiceImpl registerAdmin;
    private final com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository natureRepository;
    @org.springframework.context.annotation.Lazy
    private final com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService lifecycle;

    private static final String CASH_SESSIONS_PATH = "/payments/cash-registers/sessions/";

    /** Delai du second visa d'une operation en attente ; au-dela, elle expire (lot C3). */
    @Value("${bokati.cash.visa.expiry-hours:24}")
    private int visaExpiryHours = 24;

    @Value("${bokati.payment.cash-session.auto-limit:10000000}")
    private BigDecimal autoSessionLimit;

    @Override
    public CashRegisterResponse createRegister(CreateCashRegisterRequest request) {
        com.sni.bokaticowork.features.payment.enums.CashRegisterType type = request.registerType() == null
                ? com.sni.bokaticowork.features.payment.enums.CashRegisterType.COUNTER : request.registerType();
        if (type == com.sni.bokaticowork.features.payment.enums.CashRegisterType.AUTOMATIC) {
            throw new BadRequestException("Une caisse automatique est creee par le systeme, pas a la main");
        }
        // Seuls le libelle et le type se choisissent : le reste est herite du type de caisse ou
        // deduit du site, et reste modifiable ensuite sur la fiche (lot C1).
        com.sni.bokaticowork.features.payment.service.support.CashRegisterTypeDefaults.Defaults defaults =
                com.sni.bokaticowork.features.payment.service.support.CashRegisterTypeDefaults.of(type);
        String businessEntityCode = generateBusinessCode(request.businessEntityCode(), request.name());
        String deviceCode = generateDeviceCode(request.deviceCode(), request.name(), businessEntityCode);
        String registerCode = generateRegisterCode(request.name(), businessEntityCode, deviceCode);
        String parent = trim(request.parentRegisterCode()) != null ? trim(request.parentRegisterCode())
                : registerAdmin.defaultParent(type, request.locationCode());
        CashRegister draft = CashRegister.builder().registerCode(registerCode).build();
        registerAdmin.validateParent(draft, parent);
        String acceptedMethods = CashRegisterAdminServiceImpl.joinMethods(request.acceptedMethods() != null
                ? request.acceptedMethods() : defaults.acceptedMethods());
        String acceptedCurrencies = CashRegisterAdminServiceImpl.joinCurrencies(request.acceptedCurrencies() != null
                ? request.acceptedCurrencies() : defaults.acceptedCurrencies());
        String recipients = CashRegisterAdminServiceImpl.joinEmails(request.alertRecipients());
        if (recipients == null && StringUtils.hasText(request.managerEmail())) {
            recipients = request.managerEmail().trim().toLowerCase(Locale.ROOT);
        }
        CashRegister register = registerRepository.save(CashRegister.builder()
                .registerCode(registerCode)
                .name(request.name().trim())
                .registerType(type)
                .status(com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE)
                .locationCode(trim(request.locationCode()))
                .businessEntityCode(businessEntityCode)
                .deviceCode(deviceCode)
                .parentRegisterCode(parent)
                .active(true)
                .cashControlEnabled(request.cashControlEnabled() == null || request.cashControlEnabled())
                .maxCashAmount(request.maxCashAmount() == null ? null : money(request.maxCashAmount()))
                .managerEmail(trim(request.managerEmail()))
                .acceptedMethods(acceptedMethods)
                .acceptedCurrencies(acceptedCurrencies == null ? CASH_CURRENCY : acceptedCurrencies)
                .capBehavior(request.capBehavior() != null ? request.capBehavior() : defaults.capBehavior())
                .maxOutPerOperation(positiveOrNull(request.maxOutPerOperation() != null ? request.maxOutPerOperation() : defaults.maxOutPerOperation()))
                .maxOutPerDay(positiveOrNull(request.maxOutPerDay() != null ? request.maxOutPerDay() : defaults.maxOutPerDay()))
                .standardFloat(positiveOrNull(request.standardFloat()))
                .alertRecipients(recipients)
                .updatedBy(actors.current())
                .build());
        registerAdmin.initializeRegister(register, request.schedules(), request.holderEmail(), request.backupEmails());
        return mapper.toCashRegisterResponse(register);
    }

    private BigDecimal positiveOrNull(BigDecimal amount) {
        return amount == null || amount.signum() <= 0 ? null : money(amount);
    }

    @Override
    public CashRegisterResponse activateRegister(String registerCode) {
        registerAdmin.reactivate(registerCode);
        return mapper.toCashRegisterResponse(register(registerCode));
    }

    @Override
    public CashRegisterResponse deactivateRegister(String registerCode) {
        CashRegister register = register(registerCode);
        if (register.getId() != null) {
            sessionRepository.findBlockingSessionByCashRegisterId(register.getId())
                    .ifPresent(session -> {
                        throw new BadRequestException("Cash register has an active session");
                    });
        }
        // Forme historique de la suspension : sans motif de la liste, la desactivation est tracee comme telle.
        if (register.getStatus() == com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.CLOSED) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " est fermee");
        }
        register.setStatus(com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.SUSPENDED);
        register.setActive(false);
        register.setStatusReason("Desactivation");
        register.setUpdatedBy(actors.current());
        return mapper.toCashRegisterResponse(registerRepository.save(register));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CashRegisterResponse> listRegisters(Boolean active, String locationCode, String businessEntityCode, String searchText, Pageable pageable) {
        return new PaginatedResponse<>(registerRepository.search(
                active,
                trim(locationCode),
                trim(businessEntityCode),
                trim(searchText),
                unsorted(pageable)
        ).map(mapper::toCashRegisterResponse));
    }

    @Override
    public CashSessionResponse openSession(OpenCashSessionRequest request) {
        CashRegister register = register(request.registerCode());
        assertManualEntryAllowed(register);
        String openedBy = actors.resolve(request.openedBy());
        // Caisse active, personne affectee, poste autorise, horaires (lot C1).
        com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard.OpeningClearance clearance =
                accessGuard.assertCanOpen(register, openedBy, request.deviceCode(),
                        request.outOfHoursReasonCode(), request.outOfHoursReasonText());
        sessionRepository.findBlockingSessionByCashRegisterId(register.getId())
                .ifPresent(session -> {
                    throw new BadRequestException("Cash register already has an active session");
                });
        sessionRepository.findBlockingSessionByOpenedBy(openedBy)
                .ifPresent(session -> {
                    throw new BadRequestException("Cashier already has an active cash session");
                });
        // Continuite du fonds (lot C2) : le fonds attendu est le dernier comptage de la caisse ; un
        // fonds compte different est un ecart d'ouverture, a motiver avant toute operation.
        BigDecimal counted = request.openingCount() != null
                ? openingCountTotal(register, request.openingCount())
                : money(request.openingAmount() == null ? BigDecimal.ZERO : request.openingAmount());
        BigDecimal expectedOpening = register.getLastCountAt() == null ? null
                : money(register.getLastCountAmount() == null ? BigDecimal.ZERO : register.getLastCountAmount());
        BigDecimal openingVariance = expectedOpening == null ? BigDecimal.ZERO.setScale(4) : counted.subtract(expectedOpening);
        if (openingVariance.signum() != 0 && request.openingCount() == null
                && Boolean.TRUE.equals(register.getCountLinesRequired())) {
            throw new BadRequestException("Le fonds differe du dernier comptage de la caisse ("
                    + expectedOpening.stripTrailingZeros().toPlainString() + " XAF) : saisissez le billetage d'ouverture");
        }
        CashSession session = sessionRepository.save(CashSession.builder()
                .sessionNumber(sequenceGenerator.next("cash_session"))
                .cashRegister(register)
                .status(openingVariance.signum() == 0 ? CashSessionStatus.OPEN : CashSessionStatus.OPENING)
                .openedBy(openedBy)
                .openingAmount(counted)
                .expectedOpeningAmount(expectedOpening)
                .openingVariance(openingVariance)
                .openedFromDevice(clearance.deviceCode())
                .outOfHoursReason(clearance.outOfHoursReason())
                .build());
        if (request.openingCount() != null) {
            lifecycle.recordOpeningCount(session, request.openingCount(), expectedOpening);
        }
        if (session.getOpeningAmount().signum() > 0) {
            saveMovement(session, CashMovementType.OPENING_FLOAT, session.getOpeningAmount(), CashDocumentType.CASH_VOUCHER,
                    session.getSessionNumber(), "OPENING_FLOAT", "CASH_SESSION", session.getSessionNumber(),
                    "INTERNAL", register.getBusinessEntityCode(), register.getName(), "Opening float", session.getOpenedBy(), null);
        }
        return mapper.toCashSessionResponse(session);
    }

    private BigDecimal openingCountTotal(CashRegister register, com.sni.bokaticowork.features.payment.dto.request.CashCountRequest count) {
        BigDecimal total = BigDecimal.ZERO;
        for (com.sni.bokaticowork.features.payment.dto.request.CashCountRequest.Line line
                : count.lines() == null ? java.util.List.<com.sni.bokaticowork.features.payment.dto.request.CashCountRequest.Line>of() : count.lines()) {
            if (line != null && line.value() != null && line.quantity() != null) {
                total = total.add(line.value().multiply(BigDecimal.valueOf(line.quantity())));
            }
        }
        if ((count.lines() == null || count.lines().isEmpty()) && count.totalAmount() != null) {
            total = count.totalAmount();
        }
        return money(total);
    }

    @Override
    public CashSessionResponse openSessionFromHandover(String registerCode, String incoming, BigDecimal amount,
                                                       String handoverNumber, String deviceCode) {
        CashRegister register = register(registerCode);
        // L'entrant doit pouvoir tenir la caisse : meme controle qu'a une ouverture ordinaire.
        accessGuard.assertCanOpen(register, incoming, deviceCode, "CATCH_UP", "Passation " + handoverNumber);
        sessionRepository.findBlockingSessionByOpenedBy(incoming).ifPresent(existing -> {
            throw new BadRequestException(incoming + " a deja une session active : " + existing.getSessionNumber());
        });
        BigDecimal opening = money(amount);
        CashSession session = sessionRepository.save(CashSession.builder()
                .sessionNumber(sequenceGenerator.next("cash_session"))
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy(incoming)
                .openingAmount(opening)
                .expectedOpeningAmount(opening)
                .openingVariance(BigDecimal.ZERO.setScale(4))
                .handoverNumber(handoverNumber)
                .openedFromDevice(deviceCode)
                .build());
        if (opening.signum() > 0) {
            saveMovement(session, CashMovementType.OPENING_FLOAT, opening, CashDocumentType.CASH_VOUCHER,
                    session.getSessionNumber(), "OPENING_FLOAT", "CASH_HANDOVER", handoverNumber,
                    "INTERNAL", register.getBusinessEntityCode(), register.getName(), "Fonds recu par passation " + handoverNumber, incoming, null);
        }
        return mapper.toCashSessionResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CashSessionResponse> listSessions(String registerCode, CashSessionStatus status,
                                                               String openedBy, String closedBy, String searchText, Pageable pageable) {
        return new PaginatedResponse<>(sessionRepository.search(
                trim(registerCode),
                status == null ? null : status.name(),
                trim(openedBy),
                trim(closedBy),
                trim(searchText),
                unsorted(pageable)
        ).map(mapper::toCashSessionResponse));
    }

    @Override
    public CashSessionResponse closeSession(String sessionNumber, CloseCashSessionRequest request) {
        CashSession session = session(sessionNumber);
        // Une caisse automatique se clot sur son seuil, pas sur un comptage · il n'y a pas d'especes
        // a compter, et un ecart declare a la main y serait un ecart invente.
        assertManualEntryAllowed(session.getCashRegister());
        BigDecimal countedAmount = request.countedClosingAmount() != null ? request.countedClosingAmount() : request.closingAmount();
        if (countedAmount == null) {
            throw new BadRequestException("Counted closing amount is required");
        }
        // Forme historique de la cloture : un total compte et un motif libre. Le cycle de vie
        // (lot C2) applique tolerance, double comptage, revue et dernier comptage.
        return lifecycle.submitClosingCount(sessionNumber,
                com.sni.bokaticowork.features.payment.dto.request.CashCountRequest.ofTotal(money(countedAmount), request.varianceReason()))
                .session();
    }

    @Override
    public CashSessionResponse approveVariance(String sessionNumber, ApproveCashVarianceRequest request) {
        // Forme historique : la decision par defaut (charge pour un manquant, produit pour un excedent).
        return lifecycle.decideSessionVariance(sessionNumber,
                new com.sni.bokaticowork.features.payment.dto.request.CashVarianceDecisionRequest(null, request.note(), null))
                .session();
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionSummaryResponse sessionSummary(String sessionNumber) {
        return summarySupport.summarize(session(sessionNumber));
    }

    @Override
    public CashMovementResponse addAttachment(String movementNumber, com.sni.bokaticowork.features.payment.dto.request.AddCashMovementAttachmentRequest request) {
        CashMovement movement = movementByNumber(movementNumber);
        com.sni.bokaticowork.features.payment.model.CashMovementAttachment attachment =
                com.sni.bokaticowork.features.payment.model.CashMovementAttachment.builder()
                        .cashMovement(movement)
                        .fileName(request.fileName().trim())
                        .contentType(trim(request.contentType()))
                        .storagePath(request.storagePath().trim())
                        .label(trim(request.label()))
                        .uploadedBy(actors.resolve(request.uploadedBy()))
                        .build();
        attachmentRepository.save(attachment);
        return mapper.toCashMovementResponse(movement);
    }

    @Override
    public CashMovementResponse signMovement(String movementNumber, String signedBy) {
        String signer = actors.resolve(signedBy);
        CashMovement movement = movementByNumber(movementNumber);
        movement.setSignedBy(signer);
        movement.setSignedAt(Instant.now());
        return mapper.toCashMovementResponse(movementRepository.save(movement));
    }

    @Override
    public CashMovementResponse markMovementPrinted(String movementNumber) {
        CashMovement movement = movementByNumber(movementNumber);
        movement.setPrintedAt(Instant.now());
        return mapper.toCashMovementResponse(movementRepository.save(movement));
    }

    @Override
    @Transactional(readOnly = true)
    public CashMovementResponse getMovement(String movementNumber) {
        return mapper.toCashMovementResponse(movementByNumber(movementNumber));
    }

    private CashMovement movementByNumber(String movementNumber) {
        if (!StringUtils.hasText(movementNumber)) {
            throw new BadRequestException("Cash movement number is required");
        }
        return movementRepository.findByMovementNumber(movementNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash movement not found: " + movementNumber));
    }

    @Override
    public CashMovementResponse createMovement(String sessionNumber, CreateCashMovementRequest request) {
        CashSession session = requireOpenSession(sessionNumber);
        CashMovementType type = request.movementType();
        if (type == CashMovementType.PAYMENT || type == CashMovementType.OPENING_FLOAT || type == CashMovementType.CLOSING_COUNT) {
            throw new BadRequestException("Movement type is system managed");
        }
        if (type == CashMovementType.ADJUSTMENT
                && request.direction() != com.sni.bokaticowork.features.payment.enums.CashFlowDirection.IN
                && request.direction() != com.sni.bokaticowork.features.payment.enums.CashFlowDirection.OUT) {
            throw new BadRequestException("Un ajustement de caisse doit preciser son sens : IN (entree) ou OUT (sortie)");
        }
        com.sni.bokaticowork.features.payment.model.CashOperationNature nature = nature(request.natureCode(), type, request.amount(), request.referenceType());
        CashMovement movement = saveManualMovement(session, type, request.amount(), request.documentType(), request.documentNumber(),
                nature == null ? request.flowCategory() : nature.getCode(), request.referenceType(), request.referenceCode(), request.counterpartyType(),
                request.counterpartyCode(), request.counterpartyName(),
                nature != null && !StringUtils.hasText(request.reason()) ? nature.getLabel() : request.reason(), request.createdBy(), request.metadataJson(),
                type == CashMovementType.ADJUSTMENT ? request.direction() : null);
        movement = applyOperationRules(movement, nature);
        movement = applyEnrichments(movement, request);
        return mapper.toCashMovementResponse(movement);
    }

    private CashMovement applyEnrichments(CashMovement movement, CreateCashMovementRequest request) {
        boolean changed = false;
        if (StringUtils.hasText(request.relatedMovementNumber())) {
            movement.setRelatedMovementId(movementRepository.findByMovementNumber(request.relatedMovementNumber().trim())
                    .map(CashMovement::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Related cash movement not found: " + request.relatedMovementNumber())));
            changed = true;
        }
        if (StringUtils.hasText(request.batchId())) {
            movement.setBatchId(request.batchId().trim());
            changed = true;
        }
        if (request.exchangeRate() != null) {
            movement.setExchangeRate(request.exchangeRate());
            changed = true;
        }
        if (request.channel() != null) {
            movement.setChannel(request.channel());
            changed = true;
        }
        if (StringUtils.hasText(request.deviceCode())) {
            movement.setDeviceCode(request.deviceCode().trim());
            changed = true;
        }
        if (StringUtils.hasText(request.deviceIp())) {
            movement.setDeviceIp(request.deviceIp().trim());
            changed = true;
        }
        if (StringUtils.hasText(request.subCategory())) {
            movement.setSubCategory(request.subCategory().trim());
            changed = true;
        }
        if (StringUtils.hasText(request.tags())) {
            movement.setTags(request.tags().trim());
            changed = true;
        }
        BigDecimal runningBalance = computeRunningBalance(movement);
        movement.setRunningBalance(runningBalance);
        return changed || runningBalance != null ? movementRepository.save(movement) : movement;
    }

    /**
     * Solde de la session juste apres ce mouvement.
     *
     * <p>Il part de zero : le fonds d'ouverture est deja un mouvement {@code OPENING_FLOAT}. Partir
     * du fonds d'ouverture, comme avant, le comptait deux fois. Le sens vient de la regle unique
     * ({@link com.sni.bokaticowork.features.payment.service.support.CashFlowRules}).</p>
     */
    private BigDecimal computeRunningBalance(CashMovement movement) {
        CashSession session = movement.getCashSession();
        if (session == null || session.getId() == null) {
            return null;
        }
        BigDecimal balance = BigDecimal.ZERO;
        for (CashMovement entry : movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId())) {
            if (entry.getId() != null && entry.getId().equals(movement.getId())) {
                break;
            }
            balance = balance.add(com.sni.bokaticowork.features.payment.service.support.CashFlowRules.signedAmount(entry));
        }
        return balance.add(com.sni.bokaticowork.features.payment.service.support.CashFlowRules.signedAmount(movement)).setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public CashMovementResponse createEntryVoucher(String sessionNumber, CreateCashVoucherRequest request) {
        return voucher(sessionNumber, CashMovementType.CASH_IN, CashDocumentType.ENTRY_VOUCHER, request);
    }

    @Override
    public CashMovementResponse createExitVoucher(String sessionNumber, CreateCashVoucherRequest request) {
        return voucher(sessionNumber, CashMovementType.CASH_OUT, CashDocumentType.EXIT_VOUCHER, request);
    }

    private CashMovementResponse voucher(String sessionNumber, CashMovementType type, CashDocumentType defaultDocument,
                                         CreateCashVoucherRequest request) {
        CashSession session = requireOpenSession(sessionNumber);
        com.sni.bokaticowork.features.payment.model.CashOperationNature nature = nature(request.natureCode(), type, request.amount(), request.referenceType());
        CashMovement movement = saveManualMovement(session, type, request.amount(),
                request.documentType() == null ? defaultDocument : request.documentType(),
                request.documentNumber(), nature == null ? request.flowCategory() : nature.getCode(), request.referenceType(), request.referenceCode(),
                request.counterpartyType(), request.counterpartyCode(), request.counterpartyName(),
                nature != null && !StringUtils.hasText(request.reason()) ? nature.getLabel() : request.reason(),
                request.createdBy(), request.metadataJson());
        movement = applyOperationRules(movement, nature);
        return mapper.toCashMovementResponse(movement);
    }

    /**
     * Nature d'operation choisie (lot C3) : active, de meme sens que l'operation, dans son plafond,
     * et passee par une demande de fonds quand la nature l'exige.
     */
    private com.sni.bokaticowork.features.payment.model.CashOperationNature nature(String natureCode, CashMovementType type,
                                                                                    BigDecimal amount, String referenceType) {
        if (!StringUtils.hasText(natureCode)) {
            return null;
        }
        com.sni.bokaticowork.features.payment.model.CashOperationNature nature = natureRepository.findByCode(natureCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Nature d'operation inconnue : " + natureCode));
        if (!Boolean.TRUE.equals(nature.getActive())) {
            throw new BadRequestException("La nature « " + nature.getLabel() + " » n'est plus proposee");
        }
        if (type != CashMovementType.ADJUSTMENT && nature.getMovementType() != type) {
            throw new BadRequestException("La nature « " + nature.getLabel() + " » est une "
                    + (nature.getDirection() == com.sni.bokaticowork.features.payment.enums.CashFlowDirection.IN ? "entree" : "sortie") + " : elle ne convient pas a cette operation");
        }
        boolean fromRequest = "CASH_REQUEST".equalsIgnoreCase(referenceType) || "FUND_REQUEST".equalsIgnoreCase(referenceType);
        if (Boolean.TRUE.equals(nature.getPriorRequestRequired()) && !fromRequest) {
            throw new BadRequestException("« " + nature.getLabel() + " » passe par une demande de fonds approuvee");
        }
        if (nature.getMaxPerOperation() != null && amount != null && amount.compareTo(nature.getMaxPerOperation()) > 0 && !fromRequest) {
            throw new BadRequestException("Au-dela de " + nature.getMaxPerOperation().stripTrailingZeros().toPlainString()
                    + " XAF, « " + nature.getLabel() + " » passe par une demande de fonds");
        }
        nature.setUsageCount((nature.getUsageCount() == null ? 0L : nature.getUsageCount()) + 1);
        natureRepository.save(nature);
        return nature;
    }

    /**
     * Mouvements en attente (lot C3) : un ajustement attend toujours le visa d'un superviseur ; une
     * sortie au-dela du seuil de double visa (de la nature, sinon de la caisse) attend un second
     * visa. En attente, l'operation ne touche pas au solde et expire sans visa.
     */
    private CashMovement applyOperationRules(CashMovement movement, com.sni.bokaticowork.features.payment.model.CashOperationNature nature) {
        boolean changed = false;
        if (nature != null) {
            movement.setNatureCode(nature.getCode());
            changed = true;
        }
        BigDecimal threshold = nature != null && nature.getDoubleVisaThreshold() != null ? nature.getDoubleVisaThreshold()
                : movement.getCashSession().getCashRegister().getDoubleVisaThreshold();
        boolean outflow = com.sni.bokaticowork.features.payment.service.support.CashFlowRules
                .direction(movement.getMovementType(), movement.getDirection()) == com.sni.bokaticowork.features.payment.enums.CashFlowDirection.OUT;
        boolean needsVisa = movement.getMovementType() == CashMovementType.ADJUSTMENT
                || (threshold != null && movement.getAmount().compareTo(threshold) > 0 && (outflow || threshold.signum() == 0));
        if (needsVisa) {
            movement.setStatus(com.sni.bokaticowork.features.payment.enums.CashMovementStatus.PENDING);
            movement.setExpiresAt(Instant.now().plus(java.time.Duration.ofHours(visaExpiryHours)));
            movement.setRunningBalance(computeRunningBalance(movement));
            changed = true;
            emailNotifier.notifySupervisor("Operation de caisse a viser · " + movement.getMovementNumber(),
                    movement.getCreatedBy() + " a saisi " + movement.getMovementType().name() + " de "
                            + movement.getAmount().stripTrailingZeros().toPlainString() + " XAF sur la session "
                            + movement.getCashSession().getSessionNumber() + " : un second visa est attendu.",
                    movement.getMovementNumber(), CASH_SESSIONS_PATH + movement.getCashSession().getSessionNumber());
        }
        return changed ? movementRepository.save(movement) : movement;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CashMovementResponse> listMovements(String registerCode, String sessionNumber, CashMovementType movementType,
                                                                 CashDocumentType documentType, String documentNumber, String flowCategory,
                                                                 String referenceType, String referenceCode, String counterpartyCode, String counterpartyName, String createdBy,
                                                                 Instant fromDate, Instant toDate, String searchText, Pageable pageable) {
        return new PaginatedResponse<>(movementRepository.search(
                trim(registerCode),
                trim(sessionNumber),
                movementType == null ? null : movementType.name(),
                documentType == null ? null : documentType.name(),
                trim(documentNumber),
                trim(flowCategory),
                trim(referenceType),
                trim(referenceCode),
                trim(counterpartyCode),
                trim(counterpartyName),
                trim(createdBy),
                fromDate,
                toDate,
                trim(searchText),
                unsorted(pageable)
        ).map(mapper::toCashMovementResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CashMetricsOverviewResponse overviewMetrics(String registerCode, String businessEntityCode, Instant fromDate, Instant toDate) {
        Object[] countRow = unwrap(sessionRepository.overviewCounts(trim(registerCode), trim(businessEntityCode), fromDate, toDate));
        Object[] amountRow = unwrap(movementRepository.overviewAmounts(trim(registerCode), trim(businessEntityCode), fromDate, toDate));

        BigDecimal openingFloat = decimalAt(amountRow, 1);
        BigDecimal totalPayments = decimalAt(amountRow, 2);
        BigDecimal totalRefunds = decimalAt(amountRow, 3);
        BigDecimal totalCashIn = decimalAt(amountRow, 4);
        BigDecimal totalCashOut = decimalAt(amountRow, 5);
        BigDecimal totalAdjustments = decimalAt(amountRow, 6);

        return new CashMetricsOverviewResponse(
                CASH_CURRENCY,
                longAt(countRow, 0),
                longAt(countRow, 1),
                longAt(countRow, 2),
                longAt(countRow, 3),
                longAt(countRow, 4),
                longAt(amountRow, 0),
                openingFloat,
                totalPayments,
                totalRefunds,
                totalCashIn,
                totalCashOut,
                totalAdjustments,
                openingFloat.add(totalPayments).add(totalCashIn).add(totalAdjustments).subtract(totalRefunds).subtract(totalCashOut),
                decimalAt(countRow, 5)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterMetricsResponse> registerMetrics(String businessEntityCode, Instant fromDate, Instant toDate) {
        return movementRepository.registerMetrics(trim(businessEntityCode), fromDate, toDate).stream()
                .map(this::toRegisterMetrics)
                .toList();
    }

    @Override
    public void recordPayment(String sessionNumber, BigDecimal amount, String referenceCode, String createdBy) {
        CashSession session = requireOpenSession(sessionNumber);
        assertManualEntryAllowed(session.getCashRegister());
        validatePositive(amount);
        saveMovement(session, CashMovementType.PAYMENT, amount, CashDocumentType.RECEIPT, trim(referenceCode), "PAYMENT",
                "PAYMENT_TRANSACTION", trim(referenceCode), "CUSTOMER", null, null, null, actors.resolve(createdBy), null);
    }

    /**
     * Range un paiement de la plateforme (mobile money, portefeuille) dans sa caisse automatique, en
     * ouvrant une session s'il n'y en a pas.
     *
     * <p>Dans sa propre transaction : l'appelant (le traitement d'un paiement confirme) attrape les
     * erreurs d'ici pour ne pas bloquer la confirmation. Dans la meme transaction, une erreur l'aurait
     * marquee pour annulation et toute la confirmation (facture, reservation) aurait ete perdue.</p>
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordAutomaticPayment(PaymentTransaction transaction) {
        if (transaction == null || transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
            return;
        }
        if (transaction.getPaymentMethod() != PaymentMethod.WALLET
                && transaction.getPaymentMethod() != PaymentMethod.MOBILE_MONEY) {
            return;
        }
        String transactionNumber = trim(transaction.getTransactionNumber());
        if (!StringUtils.hasText(transactionNumber)
                || movementRepository.existsByReferenceTypeAndReferenceCode(PAYMENT_TRANSACTION_REFERENCE, transactionNumber)) {
            return;
        }

        BigDecimal amount = money(transaction.getAmount());
        CashRegister register = automaticRegister(transaction.getPaymentMethod());
        assertMethodAllowed(register, transaction.getPaymentMethod());
        CashSession session = automaticSession(register, transaction.getPaymentMethod(), amount);
        PaymentIntent intent = transaction.getPaymentIntent();
        String methodLabel = paymentMethodLabel(transaction.getPaymentMethod());
        String currency = StringUtils.hasText(transaction.getCurrency()) ? transaction.getCurrency().trim().toUpperCase(Locale.ROOT) : CASH_CURRENCY;
        String actor = StringUtils.hasText(transaction.getReceivedBy()) ? transaction.getReceivedBy().trim() : automaticActor(transaction.getPaymentMethod());

        saveMovement(session, CashMovementType.PAYMENT, amount, currency, CashDocumentType.RECEIPT,
                transaction.getReceiptNumber(), methodLabel, PAYMENT_TRANSACTION_REFERENCE, transactionNumber,
                intent == null ? "CUSTOMER" : intent.getCustomerType(), intent == null ? null : intent.getCustomerCode(),
                null, "Enregistrement automatique du paiement " + methodLabel, actor, automaticMetadata(transaction));

        BigDecimal expectedAfter = money(summarySupport.expectedClosingAmount(session));
        if (expectedAfter.compareTo(autoLimit()) >= 0) {
            closeAutomaticSession(session, expectedAfter, "Cloture automatique: seuil de session atteint");
            CashSession next = openAutomaticSession(register, transaction.getPaymentMethod());
            finalized(session.getSessionNumber(), true, "seuil de la session atteint", next.getSessionNumber());
        }
    }

    private CashRegister automaticRegister(PaymentMethod method) {
        String registerCode = automaticRegisterCode(method);
        return registerRepository.findByRegisterCode(registerCode)
                .map(register -> {
                    // Une caisse automatique creee avant cette regle n'en porte pas encore la
                    // marque · on la lui pose au premier passage plutot que par une migration,
                    // pour que la verite soit celle du code qui la cree.
                    boolean changed = false;
                    if (!Boolean.TRUE.equals(register.getActive())) {
                        register.setActive(true);
                        changed = true;
                    }
                    if (!Boolean.TRUE.equals(register.getSystemManaged())) {
                        register.setSystemManaged(true);
                        changed = true;
                    }
                    if (register.getRegisterType() != com.sni.bokaticowork.features.payment.enums.CashRegisterType.AUTOMATIC) {
                        register.setRegisterType(com.sni.bokaticowork.features.payment.enums.CashRegisterType.AUTOMATIC);
                        changed = true;
                    }
                    if (register.getStatus() != com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE) {
                        register.setStatus(com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE);
                        changed = true;
                    }
                    // La restriction n'est posee que si elle manque. La recrire quand elle differe
                    // effacerait le garde au moment meme ou il aurait quelque chose a dire : une
                    // caisse restreinte au portefeuille que l'on retrouve sous le code du mobile
                    // money est une configuration cassee, pas une configuration a rattraper en
                    // silence · assertMethodAllowed doit pouvoir le refuser.
                    if (register.getRestrictedToMethod() == null && exclusiveMethod(method) != null) {
                        register.setRestrictedToMethod(exclusiveMethod(method));
                        changed = true;
                    }
                    if (changed) {
                        registerRepository.save(register);
                    }
                    return register;
                })
                .orElseGet(() -> registerRepository.save(CashRegister.builder()
                        .registerCode(registerCode)
                        .name("Caisse automatique - " + paymentMethodLabel(method))
                        .locationCode(AUTO_LOCATION_CODE)
                        .businessEntityCode(AUTO_BUSINESS_CODE)
                        .deviceCode(automaticDeviceCode(method))
                        .active(true)
                        .cashControlEnabled(true)
                        .systemManaged(true)
                        .registerType(com.sni.bokaticowork.features.payment.enums.CashRegisterType.AUTOMATIC)
                        .status(com.sni.bokaticowork.features.payment.enums.CashRegisterStatus.ACTIVE)
                        .restrictedToMethod(exclusiveMethod(method))
                        .maxCashAmount(autoLimit())
                        .build()));
    }

    private CashSession automaticSession(CashRegister register, PaymentMethod method, BigDecimal nextAmount) {
        registerRepository.lockById(register.getId());
        CashSession session = sessionRepository.findFirstByCashRegisterIdAndStatusForUpdate(register.getId(), CashSessionStatus.OPEN.name())
                .orElseGet(() -> openAutomaticSession(register, method));
        BigDecimal currentTotal = money(summarySupport.expectedClosingAmount(session));
        if (currentTotal.signum() > 0 && currentTotal.add(nextAmount).compareTo(autoLimit()) > 0) {
            closeAutomaticSession(session, currentTotal, "Cloture automatique: seuil de session atteint");
            CashSession next = openAutomaticSession(register, method);
            finalized(session.getSessionNumber(), true, "seuil de la session atteint", next.getSessionNumber());
            return next;
        }
        return session;
    }

    private CashSession openAutomaticSession(CashRegister register, PaymentMethod method) {
        return sessionRepository.save(CashSession.builder()
                .sessionNumber(sequenceGenerator.next("cash_session"))
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy(automaticActor(method))
                .openingAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .build());
    }

    private void closeAutomaticSession(CashSession session, BigDecimal expectedAmount, String reason) {
        Instant now = Instant.now();
        BigDecimal counted = money(expectedAmount);
        session.setClosedBy(SYSTEM_ACTOR);
        session.setClosingAmount(counted);
        session.setExpectedClosingAmount(counted);
        session.setCountedClosingAmount(counted);
        session.setVarianceAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        session.setVarianceReason(reason);
        session.setClosingRequestedAt(now);
        session.setClosedAt(now);
        session.setStatus(CashSessionStatus.CLOSED);
        sessionRepository.save(session);
    }

    /**
     * Fin de mois · une session laissee ouverte est close par le systeme et remplacee.
     *
     * <p>Personne n'a compte les especes : la session est close au montant attendu, sans ecart
     * declare, et ce montant devient le fonds de caisse de la session suivante, ouverte au nom du
     * meme caissier. Le rapport de cloture part a l'administration, a la gerance et au responsable
     * de la caisse, qui peuvent faire compter. Une caisse automatique est close et rouverte de la
     * meme facon, sans report : ses fonds ne sont pas des especes.</p>
     */
    @Override
    public CashSessionResponse closeForMonthEnd(String sessionNumber) {
        CashSession session = requireOpenSession(sessionNumber);
        CashRegister register = session.getCashRegister();
        BigDecimal expected = money(summarySupport.expectedClosingAmount(session));
        String cause = "fin de mois, session laissée ouverte";
        if (!Boolean.TRUE.equals(register.getSystemManaged())
                && register.getMonthEndPolicy() == com.sni.bokaticowork.features.payment.enums.CashMonthEndPolicy.REQUIRE_COUNT) {
            // Plus de report silencieux sans comptage (lot C2) : la session est suspendue et un
            // superviseur la compte et la clot d'office.
            lifecycle.suspendBySystem(session.getSessionNumber(), "Fin de mois : la caisse doit etre comptee et close");
            return mapper.toCashSessionResponse(session);
        }
        if (Boolean.TRUE.equals(register.getSystemManaged())) {
            closeAutomaticSession(session, expected, "Cloture automatique: fin de mois");
            CashSession next = openAutomaticSession(register, register.getRestrictedToMethod());
            finalized(session.getSessionNumber(), true, cause, next.getSessionNumber());
            return mapper.toCashSessionResponse(next);
        }
        Instant now = Instant.now();
        session.setClosedBy(SYSTEM_ACTOR);
        session.setClosingAmount(expected);
        session.setExpectedClosingAmount(expected);
        // Rien n'a ete compte · ni montant compte, ni ecart, plutot qu'un ecart nul invente.
        session.setCountedClosingAmount(null);
        session.setVarianceAmount(null);
        session.setVarianceReason("Clôture automatique de fin de mois : session laissée ouverte, espèces non comptées,"
                + " montant attendu reporté sur la session suivante");
        session.setClosingRequestedAt(now);
        session.setClosedAt(now);
        session.setStatus(CashSessionStatus.CLOSED);
        sessionRepository.saveAndFlush(session);

        CashSession next = sessionRepository.save(CashSession.builder()
                .sessionNumber(sequenceGenerator.next("cash_session"))
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy(session.getOpenedBy())
                .openingAmount(expected)
                .build());
        if (expected.signum() > 0) {
            saveMovement(next, CashMovementType.OPENING_FLOAT, expected, CashDocumentType.CASH_VOUCHER,
                    next.getSessionNumber(), "OPENING_FLOAT", "CASH_SESSION", session.getSessionNumber(),
                    "INTERNAL", register.getBusinessEntityCode(), register.getName(),
                    "Report de la session " + session.getSessionNumber() + " (clôture de fin de mois)", SYSTEM_ACTOR, null);
        }
        finalized(session.getSessionNumber(), true, cause, next.getSessionNumber());
        eventPublisher.publishEvent(new com.sni.bokaticowork.features.payment.service.support.CashSessionClosedEvent(session.getSessionNumber()));
        return mapper.toCashSessionResponse(next);
    }

    /** La session est close pour de bon · le rapport part apres validation en base. */
    private void finalized(String sessionNumber, boolean bySystem, String cause, String renewedAs) {
        eventPublisher.publishEvent(new com.sni.bokaticowork.features.payment.service.support.CashSessionFinalizedEvent(
                sessionNumber, bySystem, cause, renewedAs));
    }

    private BigDecimal autoLimit() {
        return autoSessionLimit == null || autoSessionLimit.signum() <= 0
                ? DEFAULT_AUTO_SESSION_LIMIT.setScale(4, RoundingMode.HALF_UP)
                : money(autoSessionLimit);
    }

    /**
     * Moyen de paiement exclusif d'une caisse automatique.
     *
     * <p>La caisse generique n'en a pas : elle recueille ce qui ne releve d'aucune caisse dediee, et
     * lui imposer un moyen unique la rendrait inutilisable des qu'un troisieme moyen apparaitrait.</p>
     */
    private PaymentMethod exclusiveMethod(PaymentMethod method) {
        return switch (method) {
            case WALLET, MOBILE_MONEY -> method;
            default -> null;
        };
    }

    /**
     * Refuse toute saisie humaine sur une caisse tenue par le systeme.
     *
     * <p>Le message nomme la caisse et dit ou aller : un caissier qui tombe la-dessus s'est trompe
     * de caisse, il n'a pas besoin d'un refus, il a besoin de savoir laquelle prendre.</p>
     */
    private void assertManualEntryAllowed(CashRegister register) {
        if (Boolean.TRUE.equals(register.getSystemManaged())) {
            throw new BadRequestException("La caisse " + register.getRegisterCode()
                    + " est tenue automatiquement par le systeme · aucune saisie manuelle n'y est acceptee,"
                    + " utilisez une caisse ordinaire");
        }
    }

    /**
     * Refuse un moyen de paiement etranger a la caisse.
     *
     * <p>Sans cette regle, un paiement en especes atterrirait sur la caisse des portefeuilles et son
     * total cesserait d'etre celui des portefeuilles · le rapprochement avec le grand livre du
     * portefeuille, qui est la seule verification serieuse de cette caisse, ne voudrait plus rien
     * dire.</p>
     */
    private void assertMethodAllowed(CashRegister register, PaymentMethod method) {
        PaymentMethod restricted = register.getRestrictedToMethod();
        if (restricted != null && restricted != method) {
            throw new BadRequestException("La caisse " + register.getRegisterCode()
                    + " n'accepte que les paiements " + paymentMethodLabel(restricted)
                    + " · un paiement " + paymentMethodLabel(method) + " n'y a pas sa place");
        }
    }

    private String automaticRegisterCode(PaymentMethod method) {
        return switch (method) {
            case MOBILE_MONEY -> "CSR-AUTO-MOBILE-MONEY";
            case WALLET -> "CSR-AUTO-WALLET";
            default -> "CSR-AUTO-PAYMENT";
        };
    }

    private String automaticDeviceCode(PaymentMethod method) {
        return switch (method) {
            case MOBILE_MONEY -> "DEV-AUTO-MOBILE-MONEY";
            case WALLET -> "DEV-AUTO-WALLET";
            default -> "DEV-AUTO-PAYMENT";
        };
    }

    private String automaticActor(PaymentMethod method) {
        return switch (method) {
            case MOBILE_MONEY -> "SYSTEM_MOBILE_MONEY";
            case WALLET -> "SYSTEM_WALLET";
            default -> SYSTEM_ACTOR;
        };
    }

    private String paymentMethodLabel(PaymentMethod method) {
        return switch (method) {
            case MOBILE_MONEY -> "Mobile money";
            case WALLET -> "Portefeuille client";
            default -> method.name();
        };
    }

    private String automaticMetadata(PaymentTransaction transaction) {
        String provider = trim(transaction.getProvider());
        String providerReference = trim(transaction.getProviderReference());
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("automaticCashSession", true);
        metadata.put("paymentMethod", transaction.getPaymentMethod().name());
        if (provider != null) {
            metadata.put("provider", provider);
        }
        if (providerReference != null) {
            metadata.put("providerReference", providerReference);
        }
        return com.sni.bokaticowork.core.utils.json.JsonPayload.of(metadata);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCounterSessionOpen(String sessionNumber) {
        CashSession session = session(sessionNumber);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new BadRequestException("La session de caisse " + session.getSessionNumber() + " n'est pas ouverte");
        }
        assertManualEntryAllowed(session.getCashRegister());
    }

    private CashSession requireOpenSession(String sessionNumber) {
        CashSession session = session(sessionNumber);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new BadRequestException("Cash session is not open");
        }
        return session;
    }

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Cash session number is required");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash session not found"));
    }

    private CashRegister register(String registerCode) {
        if (!StringUtils.hasText(registerCode)) {
            throw new BadRequestException("Cash register code is required");
        }
        return registerRepository.findByRegisterCode(registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash register not found"));
    }

    private CashMovement saveManualMovement(CashSession session,
                                            CashMovementType movementType,
                                            BigDecimal amount,
                                            CashDocumentType documentType,
                                            String documentNumber,
                                            String flowCategory,
                                            String referenceType,
                                            String referenceCode,
                                            String counterpartyType,
                                            String counterpartyCode,
                                            String counterpartyName,
                                            String reason,
                                            String createdBy,
                                            String metadataJson) {
        return saveManualMovement(session, movementType, amount, documentType, documentNumber, flowCategory, referenceType,
                referenceCode, counterpartyType, counterpartyCode, counterpartyName, reason, createdBy, metadataJson, null);
    }

    private CashMovement saveManualMovement(CashSession session,
                                            CashMovementType movementType,
                                            BigDecimal amount,
                                            CashDocumentType documentType,
                                            String documentNumber,
                                            String flowCategory,
                                            String referenceType,
                                            String referenceCode,
                                            String counterpartyType,
                                            String counterpartyCode,
                                            String counterpartyName,
                                            String reason,
                                            String createdBy,
                                            String metadataJson,
                                            com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction) {
        validatePositive(amount);
        // Toute saisie humaine passe ici · le controle y tient donc en un seul point, plutot que
        // repete a chaque point d'entree ou l'un d'eux finirait par l'oublier.
        assertManualEntryAllowed(session.getCashRegister());
        String author = actors.resolve(createdBy);
        if (requiresReason(movementType) && !StringUtils.hasText(reason)) {
            throw new BadRequestException("Reason is required for this cash movement type");
        }
        if (requiresReason(documentType) && !StringUtils.hasText(reason)) {
            throw new BadRequestException("Reason is required for this cash document type");
        }
        if (requiresDocumentNumber(documentType) && !StringUtils.hasText(documentNumber)) {
            throw new BadRequestException("Document number is required for this cash document type");
        }
        return saveMovement(session, movementType, amount, CASH_CURRENCY, documentType, documentNumber, flowCategory,
                referenceType, referenceCode, counterpartyType, counterpartyCode, counterpartyName, reason, author, metadataJson, direction);
    }

    private CashMovement saveMovement(CashSession session, CashMovementType movementType, BigDecimal amount,
                                      CashDocumentType documentType, String documentNumber, String flowCategory,
                                      String referenceType, String referenceCode, String counterpartyType, String counterpartyCode,
                                      String counterpartyName, String reason, String createdBy, String metadataJson) {
        return saveMovement(session, movementType, amount, CASH_CURRENCY, documentType, documentNumber, flowCategory,
                referenceType, referenceCode, counterpartyType, counterpartyCode, counterpartyName, reason, createdBy, metadataJson);
    }

    private CashMovement saveMovement(CashSession session, CashMovementType movementType, BigDecimal amount, String currency,
                                      CashDocumentType documentType, String documentNumber, String flowCategory,
                                      String referenceType, String referenceCode, String counterpartyType, String counterpartyCode,
                                      String counterpartyName, String reason, String createdBy, String metadataJson) {
        return saveMovement(session, movementType, amount, currency, documentType, documentNumber, flowCategory,
                referenceType, referenceCode, counterpartyType, counterpartyCode, counterpartyName, reason, createdBy, metadataJson, null);
    }

    /**
     * Enregistre un mouvement et son solde courant.
     *
     * <p>Le solde courant n'etait pose que sur les mouvements libres ; les bons, les paiements et le
     * fonds d'ouverture n'en avaient pas. Il est desormais calcule pour tous.</p>
     */
    private CashMovement saveMovement(CashSession session, CashMovementType movementType, BigDecimal amount, String currency,
                                      CashDocumentType documentType, String documentNumber, String flowCategory,
                                      String referenceType, String referenceCode, String counterpartyType, String counterpartyCode,
                                      String counterpartyName, String reason, String createdBy, String metadataJson,
                                      com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction) {
        CashMovement movement = CashMovement.builder()
                .movementNumber(sequenceGenerator.next("cash_movement"))
                .cashSession(session)
                .movementType(movementType)
                .amount(money(amount))
                .currency(StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : CASH_CURRENCY)
                .documentType(documentType)
                .documentNumber(trim(documentNumber))
                .flowCategory(trim(flowCategory))
                .referenceType(trim(referenceType))
                .referenceCode(trim(referenceCode))
                .counterpartyType(trim(counterpartyType))
                .counterpartyCode(trim(counterpartyCode))
                .counterpartyName(trim(counterpartyName))
                .reason(trim(reason))
                .createdBy(trim(createdBy))
                .metadataJson(trim(metadataJson))
                .direction(direction)
                .build();
        // Calcule avant l'enregistrement : le mouvement n'est pas encore en base, il s'ajoute au
        // solde des mouvements deja enregistres de la session. Un seul enregistrement.
        movement.setRunningBalance(computeRunningBalance(movement));
        return movementRepository.save(movement);
    }

    private boolean requiresDocumentNumber(CashDocumentType documentType) {
        return documentType == CashDocumentType.ENTRY_VOUCHER
                || documentType == CashDocumentType.EXIT_VOUCHER
                || documentType == CashDocumentType.CASH_VOUCHER
                || documentType == CashDocumentType.BANK_SLIP
                || documentType == CashDocumentType.EXPENSE_NOTE;
    }

    private boolean requiresReason(CashDocumentType documentType) {
        return documentType == CashDocumentType.ENTRY_VOUCHER
                || documentType == CashDocumentType.EXIT_VOUCHER
                || documentType == CashDocumentType.EXPENSE_NOTE;
    }

    private boolean requiresReason(CashMovementType movementType) {
        return movementType == CashMovementType.REFUND
                || movementType == CashMovementType.CASH_OUT
                || movementType == CashMovementType.SAFE_DEPOSIT
                || movementType == CashMovementType.ADJUSTMENT
                || movementType == CashMovementType.TRANSFER_OUT;
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Cash amount must be positive");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new BadRequestException("Cash amount cannot be negative");
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private CashRegisterMetricsResponse toRegisterMetrics(Object[] row) {
        Object[] values = unwrap(row);
        BigDecimal totalPayments = decimalAt(values, 7);
        BigDecimal totalRefunds = decimalAt(values, 8);
        BigDecimal totalCashIn = decimalAt(values, 9);
        BigDecimal totalCashOut = decimalAt(values, 10);
        BigDecimal totalAdjustments = decimalAt(values, 11);
        return new CashRegisterMetricsResponse(
                textAt(values, 0),
                textAt(values, 1),
                textAt(values, 2),
                textAt(values, 3),
                longAt(values, 4),
                longAt(values, 5),
                longAt(values, 6),
                totalPayments,
                totalRefunds,
                totalCashIn,
                totalCashOut,
                totalAdjustments,
                totalPayments.add(totalCashIn).add(totalAdjustments).subtract(totalRefunds).subtract(totalCashOut)
        );
    }

    private String generateBusinessCode(String requestedCode, String name) {
        String base = compact(StringUtils.hasText(requestedCode) ? requestedCode : name, 16, "BIZ");
        String timestamp = CASH_CODE_FORMATTER.format(Instant.now());
        String token = compact(sequenceGenerator.next("cash_register"), 12, "000000");
        return "BIZ-" + base + "-" + timestamp + "-" + token;
    }

    private String generateDeviceCode(String requestedCode, String name, String businessCode) {
        String hint = StringUtils.hasText(requestedCode) ? requestedCode : businessCode + "-" + name;
        String base = compact(hint, 18, "DEVICE");
        String timestamp = CASH_CODE_FORMATTER.format(Instant.now());
        String token = compact(sequenceGenerator.next("cash_register"), 10, "000000");
        return "DEV-" + base + "-" + timestamp + "-" + token;
    }

    private String generateRegisterCode(String name, String businessCode, String deviceCode) {
        String base = compact(name, 12, "REGISTER");
        String businessPart = compact(businessCode, 8, "BIZ");
        String devicePart = compact(deviceCode, 8, "DEV");
        String timestamp = CASH_CODE_FORMATTER.format(Instant.now());
        String token = compact(sequenceGenerator.next("cash_register"), 10, "000000");
        return "CSR-" + businessPart + "-" + devicePart + "-" + base + "-" + timestamp + "-" + token;
    }

    private String compact(String raw, int maxLength, String fallback) {
        String normalized = StringUtils.hasText(raw)
                ? raw.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT)
                : fallback;
        if (!StringUtils.hasText(normalized)) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private Object[] unwrap(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        return row;
    }

    private BigDecimal decimalAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return BigDecimal.ZERO;
        }
        if (row[index] instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(row[index].toString());
    }

    private long longAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0L;
        }
        if (row[index] instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(row[index].toString());
    }

    private String textAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
