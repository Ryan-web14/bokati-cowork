package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementChannel;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRequest;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashRequestRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CashRequestServiceImpl implements CashRequestService {

    private static final String CASH_CURRENCY = "XAF";
    private static final String CASH_REQUESTS_PATH = "/payments/cash-registers/requests/";

    private final CashRequestRepository requestRepository;
    private final CashSessionRepository sessionRepository;
    private final com.sni.bokaticowork.features.payment.repository.CashMovementRepository movementRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashRegisterService cashRegisterService;
    private final CashEmailNotifier emailNotifier;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CashRequestResponse submit(String sessionNumber, CreateCashRequestRequest request) {
        CashSession session = session(sessionNumber);
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BadRequestException("Cash request amount must be positive");
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Cash request reason is required");
        }
        String requestedBy = StringUtils.hasText(request.requestedBy()) ? request.requestedBy().trim() : session.getOpenedBy();

        CashRequest cashRequest = CashRequest.builder()
                .requestNumber(sequenceGenerator.next("cash_request"))
                .cashSession(session)
                .requestType(request.requestType())
                .status(CashRequestStatus.PENDING)
                .amount(request.amount().setScale(4, java.math.RoundingMode.HALF_UP))
                .currency(StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase(Locale.ROOT) : CASH_CURRENCY)
                .reason(request.reason().trim())
                .requestedBy(requestedBy)
                .attachmentsJson(serializeAttachments(request.attachments()))
                .build();
        cashRequest = requestRepository.save(cashRequest);

        emailNotifier.notifySupervisor(
                "Nouvelle demande de caisse — " + cashRequest.getRequestNumber(),
                requesterLabel(cashRequest) + " a soumis une demande de type " + cashRequest.getRequestType().name()
                        + " pour un montant de " + cashRequest.getAmount() + " " + cashRequest.getCurrency()
                        + " sur la session " + session.getSessionNumber() + ". Motif : " + cashRequest.getReason(),
                cashRequest.getRequestNumber(),
                CASH_REQUESTS_PATH + cashRequest.getRequestNumber()
        );

        return toResponse(cashRequest);
    }

    @Override
    @Transactional
    public CashRequestResponse approve(String requestNumber, ReviewCashRequestRequest request) {
        CashRequest cashRequest = requireStatus(requestNumber, CashRequestStatus.PENDING);
        cashRequest.setStatus(CashRequestStatus.APPROVED);
        cashRequest.setReviewedBy(request.reviewedBy().trim());
        cashRequest.setReviewedAt(Instant.now());
        cashRequest.setReviewNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        cashRequest = requestRepository.save(cashRequest);

        executeApprovedRequest(cashRequest);

        emailNotifier.notifyUser(
                cashRequest.getRequestedBy(),
                "Demande de caisse approuvée — " + cashRequest.getRequestNumber(),
                "Votre demande " + cashRequest.getRequestType().name() + " de " + cashRequest.getAmount() + " "
                        + cashRequest.getCurrency() + " a été approuvée par " + cashRequest.getReviewedBy()
                        + (StringUtils.hasText(cashRequest.getReviewNote()) ? (" — Note : " + cashRequest.getReviewNote()) : "")
                        + ". Un mouvement de caisse a été généré automatiquement.",
                cashRequest.getRequestNumber(),
                CASH_REQUESTS_PATH + cashRequest.getRequestNumber()
        );

        return toResponse(cashRequest);
    }

    @Override
    @Transactional
    public CashRequestResponse reject(String requestNumber, ReviewCashRequestRequest request) {
        CashRequest cashRequest = requireStatus(requestNumber, CashRequestStatus.PENDING);
        cashRequest.setStatus(CashRequestStatus.REJECTED);
        cashRequest.setReviewedBy(request.reviewedBy().trim());
        cashRequest.setReviewedAt(Instant.now());
        cashRequest.setReviewNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        cashRequest = requestRepository.save(cashRequest);

        emailNotifier.notifyUser(
                cashRequest.getRequestedBy(),
                "Demande de caisse rejetée — " + cashRequest.getRequestNumber(),
                "Votre demande " + cashRequest.getRequestType().name() + " de " + cashRequest.getAmount() + " "
                        + cashRequest.getCurrency() + " a été rejetée par " + cashRequest.getReviewedBy()
                        + (StringUtils.hasText(cashRequest.getReviewNote()) ? (" — Motif : " + cashRequest.getReviewNote()) : "") + ".",
                cashRequest.getRequestNumber(),
                CASH_REQUESTS_PATH + cashRequest.getRequestNumber()
        );

        return toResponse(cashRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CashRequestResponse get(String requestNumber) {
        return toResponse(find(requestNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CashRequestResponse> list(String registerCode, String sessionNumber, CashRequestType requestType,
                                                        CashRequestStatus status, String requestedBy, String searchText, Pageable pageable) {
        return new PaginatedResponse<>(requestRepository.search(
                blankToNull(registerCode),
                blankToNull(sessionNumber),
                requestType == null ? null : requestType.name(),
                status == null ? null : status.name(),
                blankToNull(requestedBy),
                blankToNull(searchText),
                pageable
        ).map(this::toResponse));
    }

    private void executeApprovedRequest(CashRequest cashRequest) {
        CashMovementType movementType = movementTypeFor(cashRequest.getRequestType());
        CashDocumentType documentType = documentTypeFor(cashRequest.getRequestType());
        CreateCashMovementRequest movementRequest = new CreateCashMovementRequest(
                movementType,
                cashRequest.getAmount(),
                documentType,
                cashRequest.getRequestNumber(),
                "CASH_REQUEST",
                "CASH_REQUEST",
                cashRequest.getRequestNumber(),
                "USER",
                cashRequest.getRequestedBy(),
                cashRequest.getRequestedBy(),
                cashRequest.getReason(),
                cashRequest.getReviewedBy(),
                null,
                null,
                cashRequest.getRequestNumber(),
                null,
                CashMovementChannel.MANUAL,
                null,
                null,
                cashRequest.getRequestType().name(),
                "cash-request"
        );
        var movementResponse = cashRegisterService.createMovement(cashRequest.getCashSession().getSessionNumber(), movementRequest);
        CashMovement persisted = movementRepository.findByMovementNumber(movementResponse.movementNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Cash movement not found: " + movementResponse.movementNumber()));
        cashRequest.setExecutedMovement(persisted);
        cashRequest.setExecutedAt(Instant.now());
        cashRequest.setStatus(CashRequestStatus.EXECUTED);
        requestRepository.save(cashRequest);
    }

    private CashMovementType movementTypeFor(CashRequestType requestType) {
        return switch (requestType) {
            case REMISE_DE_FONDS -> CashMovementType.SAFE_DEPOSIT;
            case JUSTIFICATIF -> CashMovementType.ADJUSTMENT;
            case CASH_ADVANCE -> CashMovementType.CASH_OUT;
            case EXPENSE_REIMBURSEMENT -> CashMovementType.CASH_OUT;
        };
    }

    private CashDocumentType documentTypeFor(CashRequestType requestType) {
        return switch (requestType) {
            case REMISE_DE_FONDS -> CashDocumentType.BANK_SLIP;
            case JUSTIFICATIF -> CashDocumentType.CASH_VOUCHER;
            case CASH_ADVANCE -> CashDocumentType.EXIT_VOUCHER;
            case EXPENSE_REIMBURSEMENT -> CashDocumentType.EXPENSE_NOTE;
        };
    }

    private CashRequest requireStatus(String requestNumber, CashRequestStatus expected) {
        CashRequest cashRequest = find(requestNumber);
        if (cashRequest.getStatus() != expected) {
            throw new BadRequestException("Cash request " + requestNumber + " is not " + expected.name().toLowerCase(Locale.ROOT));
        }
        return cashRequest;
    }

    private CashRequest find(String requestNumber) {
        if (!StringUtils.hasText(requestNumber)) {
            throw new BadRequestException("Cash request number is required");
        }
        return requestRepository.findByRequestNumber(requestNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash request not found: " + requestNumber));
    }

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Cash session number is required");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash session not found"));
    }

    private String requesterLabel(CashRequest cashRequest) {
        return StringUtils.hasText(cashRequest.getRequestedBy()) ? cashRequest.getRequestedBy() : "Un caissier";
    }

    private String serializeAttachments(List<CreateCashRequestRequest.CashRequestAttachmentInput> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (Exception ex) {
            log.warn("Unable to serialize cash request attachments", ex);
            return null;
        }
    }

    private List<CashRequestResponse.AttachmentRef> deserializeAttachments(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            CreateCashRequestRequest.CashRequestAttachmentInput[] parsed =
                    objectMapper.readValue(json, CreateCashRequestRequest.CashRequestAttachmentInput[].class);
            return List.of(parsed).stream()
                    .map(a -> new CashRequestResponse.AttachmentRef(a.fileName(), a.contentType(), a.storagePath(), a.label()))
                    .toList();
        } catch (Exception ex) {
            log.warn("Unable to parse cash request attachments", ex);
            return List.of();
        }
    }

    private CashRequestResponse toResponse(CashRequest cashRequest) {
        CashSession session = cashRequest.getCashSession();
        CashMovement executedMovement = cashRequest.getExecutedMovement();
        return new CashRequestResponse(
                cashRequest.getRequestNumber(),
                session == null ? null : session.getSessionNumber(),
                session == null ? null : session.getCashRegister().getRegisterCode(),
                cashRequest.getRequestType(),
                cashRequest.getStatus(),
                cashRequest.getAmount(),
                cashRequest.getCurrency(),
                cashRequest.getReason(),
                cashRequest.getRequestedBy(),
                cashRequest.getRequestedAt(),
                cashRequest.getReviewedBy(),
                cashRequest.getReviewedAt(),
                cashRequest.getReviewNote(),
                executedMovement == null ? null : executedMovement.getMovementNumber(),
                cashRequest.getExecutedAt(),
                deserializeAttachments(cashRequest.getAttachmentsJson())
        );
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
