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
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class CashRegisterServiceImpl implements CashRegisterService {

    private static final String CASH_CURRENCY = "XAF";

    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashSessionSummarySupport summarySupport;
    private final CashRegisterMapper mapper;

    @Override
    public CashRegisterResponse createRegister(CreateCashRegisterRequest request) {
        CashRegister register = registerRepository.save(CashRegister.builder()
                .registerCode(sequenceGenerator.next("cash_register"))
                .name(request.name().trim())
                .locationCode(trim(request.locationCode()))
                .businessEntityCode(trim(request.businessEntityCode()))
                .deviceCode(trim(request.deviceCode()))
                .active(true)
                .cashControlEnabled(request.cashControlEnabled() == null || request.cashControlEnabled())
                .maxCashAmount(request.maxCashAmount() == null ? null : money(request.maxCashAmount()))
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
        return mapper.toCashSessionResponse(sessionRepository.save(session));
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
        return mapper.toCashSessionResponse(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionSummaryResponse sessionSummary(String sessionNumber) {
        return summarySupport.summarize(session(sessionNumber));
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
        return mapper.toCashMovementResponse(movement);
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
    public void recordPayment(String sessionNumber, BigDecimal amount, String referenceCode, String createdBy) {
        CashSession session = requireOpenSession(sessionNumber);
        validatePositive(amount);
        saveMovement(session, CashMovementType.PAYMENT, amount, CashDocumentType.RECEIPT, trim(referenceCode), "PAYMENT",
                "PAYMENT_TRANSACTION", trim(referenceCode), "CUSTOMER", null, null, null, trim(createdBy), null);
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
        return movementRepository.save(CashMovement.builder()
                .movementNumber(sequenceGenerator.next("cash_movement"))
                .cashSession(session)
                .movementType(movementType)
                .amount(money(amount))
                .currency(CASH_CURRENCY)
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

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
