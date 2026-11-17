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
import org.springframework.data.domain.PageRequest;
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
    @org.springframework.context.annotation.Lazy
    private final com.sni.bokaticowork.features.payment.service.interfaces.CashOperationsService operations;
    private final CashEmailNotifier emailNotifier;
    private final ObjectMapper objectMapper;
    private final com.sni.bokaticowork.features.payment.service.support.CashActorResolver actors;

    @Override
    @Transactional
    public CashRequestResponse submit(String sessionNumber, CreateCashRequestRequest request) {
        CashSession session = session(sessionNumber);
        // Une demande s'execute sur sa session : elle n'est recevable que session ouverte.
        if (session.getStatus() != com.sni.bokaticowork.features.payment.enums.CashSessionStatus.OPEN) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " n'est pas ouverte ("
                    + session.getStatus() + ") : la demande ne peut pas être soumise");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BadRequestException("Le montant de la demande doit être positif");
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Le motif de la demande est requis");
        }
        String requestedBy = actors.current() != null ? actors.current()
                : (StringUtils.hasText(request.requestedBy()) ? request.requestedBy().trim() : session.getOpenedBy());

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
                "Nouvelle demande de caisse · " + cashRequest.getRequestNumber(),
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
        String reviewer = reviewer(cashRequest, request);
        CashSession session = cashRequest.getCashSession();
        if (session != null && !session.getStatus().isLive()) {
            // Session close ou annulee : la demande ne s'executera plus, elle est annulee au lieu de
            // rester en attente indefiniment.
            cancelInternally(cashRequest, reviewer, "Session " + session.getSessionNumber() + " " + session.getStatus()
                    + " : demande annulée");
            return toResponse(cashRequest);
        }
        if (session != null && session.getStatus() != com.sni.bokaticowork.features.payment.enums.CashSessionStatus.OPEN) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " est " + session.getStatus()
                    + " : la demande s'approuvera quand elle sera rouverte");
        }
        cashRequest.setStatus(CashRequestStatus.APPROVED);
        cashRequest.setReviewedBy(reviewer);
        cashRequest.setReviewedAt(Instant.now());
        cashRequest.setReviewNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        cashRequest = requestRepository.save(cashRequest);

        executeApprovedRequest(cashRequest);

        emailNotifier.notifyUser(
                cashRequest.getRequestedBy(),
                "Demande de caisse approuvée · " + cashRequest.getRequestNumber(),
                "Votre demande " + cashRequest.getRequestType().name() + " de " + cashRequest.getAmount() + " "
                        + cashRequest.getCurrency() + " a été approuvée par " + cashRequest.getReviewedBy()
                        + (StringUtils.hasText(cashRequest.getReviewNote()) ? (" · Note : " + cashRequest.getReviewNote()) : "")
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
        String reviewer = reviewer(cashRequest, request);
        cashRequest.setStatus(CashRequestStatus.REJECTED);
        cashRequest.setReviewedBy(reviewer);
        cashRequest.setReviewedAt(Instant.now());
        cashRequest.setReviewNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        cashRequest = requestRepository.save(cashRequest);

        emailNotifier.notifyUser(
                cashRequest.getRequestedBy(),
                "Demande de caisse rejetée · " + cashRequest.getRequestNumber(),
                "Votre demande " + cashRequest.getRequestType().name() + " de " + cashRequest.getAmount() + " "
                        + cashRequest.getCurrency() + " a été rejetée par " + cashRequest.getReviewedBy()
                        + (StringUtils.hasText(cashRequest.getReviewNote()) ? (" · Motif : " + cashRequest.getReviewNote()) : "") + ".",
                cashRequest.getRequestNumber(),
                CASH_REQUESTS_PATH + cashRequest.getRequestNumber()
        );

        return toResponse(cashRequest);
    }

    /** Annulation par le demandeur, ou par un approbateur, tant que la demande est en attente. */
    @Override
    @Transactional
    public CashRequestResponse cancel(String requestNumber, ReviewCashRequestRequest request) {
        CashRequest cashRequest = requireStatus(requestNumber, CashRequestStatus.PENDING);
        String actor = actors.resolve(request == null ? null : request.reviewedBy());
        boolean requester = com.sni.bokaticowork.features.payment.service.support.CashActorResolver.samePerson(actor, cashRequest.getRequestedBy());
        if (!requester && !actors.hasAuthority("CASH_MOVEMENT:APPROVE")) {
            throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException(
                    "Seul le demandeur ou un approbateur peut annuler la demande");
        }
        cancelInternally(cashRequest, actor, request == null || !StringUtils.hasText(request.note()) ? "Annulée" : request.note().trim());
        return toResponse(cashRequest);
    }

    /** Demandes encore en attente d'une session close : annulees (appele apres la cloture). */
    @Override
    @Transactional
    public int cancelPendingOfSession(String sessionNumber) {
        List<CashRequest> pending = requestRepository.findAllByCashSession_SessionNumberAndStatus(sessionNumber, CashRequestStatus.PENDING);
        pending.forEach(r -> cancelInternally(r, "SYSTEM", "Session " + sessionNumber + " clôturée : demande annulée"));
        return pending.size();
    }

    private void cancelInternally(CashRequest cashRequest, String actor, String note) {
        cashRequest.setStatus(CashRequestStatus.CANCELLED);
        cashRequest.setReviewedBy(actor);
        cashRequest.setReviewedAt(Instant.now());
        cashRequest.setReviewNote(note);
        requestRepository.save(cashRequest);
        emailNotifier.notifyUser(
                cashRequest.getRequestedBy(),
                "Demande de caisse annulée · " + cashRequest.getRequestNumber(),
                "Votre demande " + cashRequest.getRequestType().name() + " de " + cashRequest.getAmount() + " "
                        + cashRequest.getCurrency() + " est annulée. " + note,
                cashRequest.getRequestNumber(),
                CASH_REQUESTS_PATH + cashRequest.getRequestNumber());
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
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(requestRepository.search(
                blankToNull(registerCode),
                blankToNull(sessionNumber),
                requestType == null ? null : requestType.name(),
                status == null ? null : status.name(),
                blankToNull(requestedBy),
                blankToNull(searchText),
                unsortedPageable
        ).map(this::toResponse));
    }

    /**
     * Celui qui decide de la demande : l'utilisateur connecte, jamais le demandeur lui-meme.
     */
    private String reviewer(CashRequest cashRequest, ReviewCashRequestRequest request) {
        String reviewer = actors.resolve(request.reviewedBy());
        if (com.sni.bokaticowork.features.payment.service.support.CashActorResolver.samePerson(reviewer, cashRequest.getRequestedBy())) {
            throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException(
                    "Le demandeur ne peut pas decider de sa propre demande de caisse");
        }
        return reviewer;
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
                "cash-request",
                // Un justificatif approuve justifie de l'argent sorti du tiroir : il diminue l'attendu
                // (avant : sens IN, qui augmentait encore l'argent attendu en caisse).
                movementType == CashMovementType.ADJUSTMENT
                        ? com.sni.bokaticowork.features.payment.enums.CashFlowDirection.OUT
                        : null,
                null
        );
        var movementResponse = cashRegisterService.createMovement(cashRequest.getCashSession().getSessionNumber(), movementRequest);
        // La demande approuvee par un autre que le demandeur vaut second visa.
        operations.approveInternally(movementResponse.movementNumber(), cashRequest.getReviewedBy());
        CashMovement persisted = movementRepository.findByMovementNumber(movementResponse.movementNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement de caisse introuvable : " + movementResponse.movementNumber()));
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
            throw new BadRequestException("La demande " + requestNumber + " est " + cashRequest.getStatus()
                    + " : action possible seulement en " + expected.name());
        }
        return cashRequest;
    }

    private CashRequest find(String requestNumber) {
        if (!StringUtils.hasText(requestNumber)) {
            throw new BadRequestException("Le numéro de la demande est requis");
        }
        return requestRepository.findByRequestNumber(requestNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Demande de caisse introuvable : " + requestNumber));
    }

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Le numéro de la session est requis");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable : " + sessionNumber));
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
