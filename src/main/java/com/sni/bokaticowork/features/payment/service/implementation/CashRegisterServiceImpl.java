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

    private static final String CASH_SESSIONS_PATH = "/payments/cash-registers/sessions/";

    @Value("${bokati.payment.cash-session.auto-limit:10000000}")
    private BigDecimal autoSessionLimit;

    @Override
    public CashRegisterResponse createRegister(CreateCashRegisterRequest request) {
        String businessEntityCode = generateBusinessCode(request.businessEntityCode(), request.name());
        String deviceCode = generateDeviceCode(request.deviceCode(), request.name(), businessEntityCode);
        String registerCode = generateRegisterCode(request.name(), businessEntityCode, deviceCode);
        CashRegister register = registerRepository.save(CashRegister.builder()
                .registerCode(registerCode)
                .name(request.name().trim())
                .locationCode(trim(request.locationCode()))
                .businessEntityCode(businessEntityCode)
                .deviceCode(deviceCode)
                .active(true)
                .cashControlEnabled(request.cashControlEnabled() == null || request.cashControlEnabled())
                .maxCashAmount(request.maxCashAmount() == null ? null : money(request.maxCashAmount()))
                .managerEmail(trim(request.managerEmail()))
                .build());
        return mapper.toCashRegisterResponse(register);
    }

    @Override
    public CashRegisterResponse activateRegister(String registerCode) {
        CashRegister register = register(registerCode);
        register.setActive(true);
        return mapper.toCashRegisterResponse(registerRepository.save(register));
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
        register.setActive(false);
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
        if (!Boolean.TRUE.equals(register.getActive())) {
            throw new BadRequestException("Cash register is inactive");
        }
        assertManualEntryAllowed(register);
        sessionRepository.findBlockingSessionByCashRegisterId(register.getId())
                .ifPresent(session -> {
                    throw new BadRequestException("Cash register already has an active session");
                });
        sessionRepository.findBlockingSessionByOpenedBy(request.openedBy().trim())
                .ifPresent(session -> {
                    throw new BadRequestException("Cashier already has an active cash session");
                });
        CashSession session = sessionRepository.save(CashSession.builder()
                .sessionNumber(sequenceGenerator.next("cash_session"))
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy(request.openedBy().trim())
                .openingAmount(money(request.openingAmount() == null ? BigDecimal.ZERO : request.openingAmount()))
                .build());
        if (session.getOpeningAmount().signum() > 0) {
            saveMovement(session, CashMovementType.OPENING_FLOAT, session.getOpeningAmount(), CashDocumentType.CASH_VOUCHER,
                    session.getSessionNumber(), "OPENING_FLOAT", "CASH_SESSION", session.getSessionNumber(),
                    "INTERNAL", register.getBusinessEntityCode(), register.getName(), "Opening float", session.getOpenedBy(), null);
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
        CashSession session = requireOpenSession(sessionNumber);
        // Une caisse automatique se clot sur son seuil, pas sur un comptage · il n'y a pas d'especes
        // a compter, et un ecart declare a la main y serait un ecart invente.
        assertManualEntryAllowed(session.getCashRegister());
        BigDecimal countedAmount = request.countedClosingAmount() != null ? request.countedClosingAmount() : request.closingAmount();
        if (countedAmount == null) {
            throw new BadRequestException("Counted closing amount is required");
        }
        BigDecimal counted = money(countedAmount);
        BigDecimal expected = money(summarySupport.expectedClosingAmount(session));
        BigDecimal variance = counted.subtract(expected);
        if (variance.signum() != 0 && !StringUtils.hasText(request.varianceReason())) {
            throw new BadRequestException("Variance reason is required when counted amount differs from expected amount");
        }
        Instant now = Instant.now();
        session.setClosedBy(request.closedBy().trim());
        session.setClosingAmount(counted);
        session.setExpectedClosingAmount(expected);
        session.setCountedClosingAmount(counted);
        session.setVarianceAmount(variance);
        session.setVarianceReason(trim(request.varianceReason()));
        session.setClosingRequestedAt(now);
        if (variance.signum() == 0) {
            session.setStatus(CashSessionStatus.CLOSED);
            session.setClosedAt(now);
        } else {
            session.setStatus(CashSessionStatus.CLOSING_REVIEW);
        }
        CashSession saved = sessionRepository.save(session);
        if (variance.signum() != 0) {
            emailNotifier.notifySupervisor(
                    "Écart de caisse à valider · " + saved.getSessionNumber(),
                    "La session " + saved.getSessionNumber() + " ouverte par " + saved.getOpenedBy()
                            + " a été clôturée par " + saved.getClosedBy() + " avec un écart de " + variance
                            + " " + CASH_CURRENCY + " (montant attendu : " + expected + ", montant compté : " + counted
                            + "). Motif déclaré : " + trim(request.varianceReason())
                            + ". Une validation par un superviseur est requise.",
                    saved.getSessionNumber(),
                    CASH_SESSIONS_PATH + saved.getSessionNumber()
            );
        }
        eventPublisher.publishEvent(new com.sni.bokaticowork.features.payment.service.support.CashSessionClosedEvent(saved.getSessionNumber()));
        return mapper.toCashSessionResponse(saved);
    }

    @Override
    public CashSessionResponse approveVariance(String sessionNumber, ApproveCashVarianceRequest request) {
        CashSession session = session(sessionNumber);
        if (session.getStatus() != CashSessionStatus.CLOSING_REVIEW) {
            throw new BadRequestException("Cash session is not waiting for variance review");
        }
        session.setReviewedBy(request.reviewedBy().trim());
        session.setReviewedAt(Instant.now());
        if (StringUtils.hasText(request.note())) {
            String currentReason = trim(session.getVarianceReason());
            session.setVarianceReason((currentReason == null ? "" : currentReason + " | ") + "Review: " + request.note().trim());
        }
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        CashSession saved = sessionRepository.save(session);
        emailNotifier.notifyUser(
                saved.getOpenedBy(),
                "Écart de caisse validé · " + saved.getSessionNumber(),
                "L'écart de " + trim(saved.getVarianceAmount() == null ? null : saved.getVarianceAmount().toPlainString())
                        + " " + CASH_CURRENCY + " constaté sur votre session " + saved.getSessionNumber()
                        + " a été validé par " + saved.getReviewedBy()
                        + (StringUtils.hasText(request.note()) ? (" · Note : " + request.note().trim()) : "")
                        + ". La session est désormais clôturée.",
                saved.getSessionNumber(),
                CASH_SESSIONS_PATH + saved.getSessionNumber()
        );
        return mapper.toCashSessionResponse(saved);
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
                        .uploadedBy(trim(request.uploadedBy()))
                        .build();
        attachmentRepository.save(attachment);
        return mapper.toCashMovementResponse(movement);
    }

    @Override
    public CashMovementResponse signMovement(String movementNumber, String signedBy) {
        if (!StringUtils.hasText(signedBy)) {
            throw new BadRequestException("Signer identifier is required");
        }
        CashMovement movement = movementByNumber(movementNumber);
        movement.setSignedBy(signedBy.trim());
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
        CashMovement movement = saveManualMovement(session, type, request.amount(), request.documentType(), request.documentNumber(),
                request.flowCategory(), request.referenceType(), request.referenceCode(), request.counterpartyType(),
                request.counterpartyCode(), request.counterpartyName(), request.reason(), request.createdBy(), request.metadataJson());
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

    private BigDecimal computeRunningBalance(CashMovement movement) {
        CashSession session = movement.getCashSession();
        if (session == null) {
            return null;
        }
        BigDecimal balance = safeAmount(session.getOpeningAmount());
        for (CashMovement entry : movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId())) {
            balance = applyDirection(balance, entry);
            if (entry.getId().equals(movement.getId())) {
                return balance.setScale(4, RoundingMode.HALF_UP);
            }
        }
        return balance.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal applyDirection(BigDecimal balance, CashMovement entry) {
        return switch (entry.getMovementType()) {
            case PAYMENT, CASH_IN, TRANSFER_IN, OPENING_FLOAT -> balance.add(entry.getAmount());
            case REFUND, CASH_OUT, SAFE_DEPOSIT, TRANSFER_OUT -> balance.subtract(entry.getAmount());
            case ADJUSTMENT, CLOSING_COUNT -> balance;
        };
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    @Override
    public CashMovementResponse createEntryVoucher(String sessionNumber, CreateCashVoucherRequest request) {
        CashSession session = requireOpenSession(sessionNumber);
        CashMovement movement = saveManualMovement(session, CashMovementType.CASH_IN, request.amount(),
                request.documentType() == null ? CashDocumentType.ENTRY_VOUCHER : request.documentType(),
                request.documentNumber(), request.flowCategory(), request.referenceType(), request.referenceCode(),
                request.counterpartyType(), request.counterpartyCode(), request.counterpartyName(), request.reason(),
                request.createdBy(), request.metadataJson());
        return mapper.toCashMovementResponse(movement);
    }

    @Override
    public CashMovementResponse createExitVoucher(String sessionNumber, CreateCashVoucherRequest request) {
        CashSession session = requireOpenSession(sessionNumber);
        CashMovement movement = saveManualMovement(session, CashMovementType.CASH_OUT, request.amount(),
                request.documentType() == null ? CashDocumentType.EXIT_VOUCHER : request.documentType(),
                request.documentNumber(), request.flowCategory(), request.referenceType(), request.referenceCode(),
                request.counterpartyType(), request.counterpartyCode(), request.counterpartyName(), request.reason(),
                request.createdBy(), request.metadataJson());
        return mapper.toCashMovementResponse(movement);
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
                "PAYMENT_TRANSACTION", trim(referenceCode), "CUSTOMER", null, null, null, trim(createdBy), null);
    }

    @Override
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
            closeAutomaticSession(session, expectedAfter);
            openAutomaticSession(register, transaction.getPaymentMethod());
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
                    // La restriction n'est posee que si elle manque. La recrire quand elle differe
                    // effacerait le garde au moment meme ou il aurait quelque chose a dire : une
                    // caisse restreinte au portefeuille que l'on retrouve sous le code du mobile
                    // money est une configuration cassee, pas une configuration a rattraper en
                    // silence · assertMethodAllowed doit pouvoir le refuser.
                    if (register.getRestrictedToMethod() == null && exclusiveMethod(method) != null) {
                        register.setRestrictedToMethod(exclusiveMethod(method));
                        changed = true;
                    }
                    return changed ? registerRepository.save(register) : register;
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
                        .restrictedToMethod(exclusiveMethod(method))
                        .maxCashAmount(autoLimit())
                        .build()));
    }

    private CashSession automaticSession(CashRegister register, PaymentMethod method, BigDecimal nextAmount) {
        CashSession session = sessionRepository.findFirstByCashRegisterIdAndStatusForUpdate(register.getId(), CashSessionStatus.OPEN.name())
                .orElseGet(() -> openAutomaticSession(register, method));
        BigDecimal currentTotal = money(summarySupport.expectedClosingAmount(session));
        if (currentTotal.signum() > 0 && currentTotal.add(nextAmount).compareTo(autoLimit()) > 0) {
            closeAutomaticSession(session, currentTotal);
            return openAutomaticSession(register, method);
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

    private void closeAutomaticSession(CashSession session, BigDecimal expectedAmount) {
        Instant now = Instant.now();
        BigDecimal counted = money(expectedAmount);
        session.setClosedBy(SYSTEM_ACTOR);
        session.setClosingAmount(counted);
        session.setExpectedClosingAmount(counted);
        session.setCountedClosingAmount(counted);
        session.setVarianceAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        session.setVarianceReason("Cloture automatique: seuil de session atteint");
        session.setClosingRequestedAt(now);
        session.setClosedAt(now);
        session.setStatus(CashSessionStatus.CLOSED);
        sessionRepository.save(session);
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
        return "{\"automaticCashSession\":true"
                + ",\"paymentMethod\":\"" + transaction.getPaymentMethod().name() + "\""
                + (provider == null ? "" : ",\"provider\":\"" + provider + "\"")
                + (providerReference == null ? "" : ",\"providerReference\":\"" + providerReference + "\"")
                + "}";
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
        validatePositive(amount);
        // Toute saisie humaine passe ici · le controle y tient donc en un seul point, plutot que
        // repete a chaque point d'entree ou l'un d'eux finirait par l'oublier.
        assertManualEntryAllowed(session.getCashRegister());
        if (!StringUtils.hasText(createdBy)) {
            throw new BadRequestException("Created by is required for manual cash movement");
        }
        if (requiresReason(movementType) && !StringUtils.hasText(reason)) {
            throw new BadRequestException("Reason is required for this cash movement type");
        }
        if (requiresReason(documentType) && !StringUtils.hasText(reason)) {
            throw new BadRequestException("Reason is required for this cash document type");
        }
        if (requiresDocumentNumber(documentType) && !StringUtils.hasText(documentNumber)) {
            throw new BadRequestException("Document number is required for this cash document type");
        }
        return saveMovement(session, movementType, amount, documentType, documentNumber, flowCategory, referenceType, referenceCode,
                counterpartyType, counterpartyCode, counterpartyName, reason, createdBy, metadataJson);
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
        return movementRepository.save(CashMovement.builder()
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
                .build());
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
