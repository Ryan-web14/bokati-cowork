package com.sni.bokaticowork.features.payment.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashExchangeRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashNatureRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferReceiveRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashNatureResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPettyRefillResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashTransferResponse;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashNatureFamily;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashTransferKind;
import com.sni.bokaticowork.features.payment.enums.CashTransferStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceKind;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashTransferOrder;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashTransferOrderRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashOperationsService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashCountRecorder;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashMovementWriter;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Operations de caisse et transferts internes (lot C3).
 *
 * <ul>
 *   <li>L'argent n'est jamais nulle part : un transfert envoye est en transit jusqu'a sa reception
 *       comptee par une autre personne ; un ecart de reception part en revue.</li>
 *   <li>Rien ne s'efface : un mouvement confirme se contre-passe, seul un mouvement en attente
 *       s'annule.</li>
 *   <li>Le caissier contre-passe seul dans les minutes qui suivent et sous un plafond ; au-dela, il
 *       faut le droit de contre-passer.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CashOperationsServiceImpl implements CashOperationsService {

    static final String REVERSAL_REASON = "REVERSAL_REASON";
    private static final String REVERSE = "CASH_MOVEMENT:REVERSE";
    private static final String APPROVE = "CASH_MOVEMENT:APPROVE";
    private static final Set<CashMovementType> NOT_REVERSIBLE_HERE = EnumSet.of(CashMovementType.PAYMENT, CashMovementType.REFUND,
            CashMovementType.OPENING_FLOAT, CashMovementType.CLOSING_COUNT, CashMovementType.TRANSFER_IN, CashMovementType.TRANSFER_OUT,
            CashMovementType.EXCHANGE, CashMovementType.VARIANCE);

    private final CashOperationNatureRepository natureRepository;
    private final CashMovementRepository movementRepository;
    private final CashSessionRepository sessionRepository;
    private final CashRegisterRepository registerRepository;
    private final CashTransferOrderRepository transferRepository;
    private final CashVarianceReviewRepository reviewRepository;
    private final CashMovementWriter writer;
    private final CashCountRecorder countRecorder;
    private final CashReferenceLists referenceLists;
    private final CashActorResolver actors;
    private final CashEmailNotifier emailNotifier;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashRegisterMapper mapper;
    private final ObjectMapper objectMapper;

    @Value("${bokati.cash.reversal.cashier-minutes:15}")
    private int cashierReversalMinutes = 15;

    @Value("${bokati.cash.reversal.cashier-max-amount:50000}")
    private BigDecimal cashierReversalMaxAmount = new BigDecimal("50000");

    @Value("${bokati.cash.transfer.max-transit-hours:2}")
    private int maxTransitHours = 2;

    private static final String REVERSAL_CASHIER_MINUTES_KEY = "bokati.cash.reversal.cashier-minutes";
    private static final String REVERSAL_CASHIER_MAX_AMOUNT_KEY = "bokati.cash.reversal.cashier-max-amount";
    private static final String TRANSFER_MAX_TRANSIT_HOURS_KEY = "bokati.cash.transfer.max-transit-hours";

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private int cashierReversalMinutes() {
        return platformSettings == null ? cashierReversalMinutes : platformSettings.integer(REVERSAL_CASHIER_MINUTES_KEY, cashierReversalMinutes);
    }

    private BigDecimal cashierReversalMaxAmount() {
        return platformSettings == null ? cashierReversalMaxAmount : platformSettings.decimal(REVERSAL_CASHIER_MAX_AMOUNT_KEY, cashierReversalMaxAmount);
    }

    private int maxTransitHours() {
        return platformSettings == null ? maxTransitHours : platformSettings.integer(TRANSFER_MAX_TRANSIT_HOURS_KEY, maxTransitHours);
    }

    // ------------------------------------------------------------------ natures

    @Override
    @Transactional(readOnly = true)
    public List<CashNatureResponse> natures(CashFlowDirection direction, CashNatureFamily family, String search, boolean includeInactive) {
        String term = StringUtils.hasText(search) ? normalize(search) : null;
        return natureRepository.findAllByOrderByFamilyAscSortOrderAscLabelAsc().stream()
                .filter(nature -> includeInactive || Boolean.TRUE.equals(nature.getActive()))
                .filter(nature -> direction == null || nature.getDirection() == direction)
                .filter(nature -> family == null || nature.getFamily() == family)
                .filter(nature -> term == null || normalize(nature.getLabel() + " " + nature.getCode() + " "
                        + (nature.getKeywords() == null ? "" : nature.getKeywords())).contains(term))
                // Les plus utilisees d'abord, dans chaque famille.
                .sorted(Comparator.comparing((CashOperationNature nature) -> nature.getFamily().ordinal())
                        .thenComparing(nature -> -(nature.getUsageCount() == null ? 0L : nature.getUsageCount()))
                        .thenComparing(CashOperationNature::getSortOrder))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CashNatureResponse> replaceNatures(List<CashNatureRequest> requests) {
        String actor = actors.resolve(null);
        Map<String, CashOperationNature> existing = new LinkedHashMap<>();
        natureRepository.findAllByOrderByFamilyAscSortOrderAscLabelAsc().forEach(nature -> existing.put(nature.getCode(), nature));
        List<CashOperationNature> saved = new ArrayList<>();
        for (CashNatureRequest request : requests == null ? List.<CashNatureRequest>of() : requests) {
            String code = request.code().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
            CashMovementType type = request.movementType() != null ? request.movementType()
                    : request.direction() == CashFlowDirection.IN ? CashMovementType.CASH_IN : CashMovementType.CASH_OUT;
            if (type != CashMovementType.CASH_IN && type != CashMovementType.CASH_OUT) {
                throw new BadRequestException("Une nature de caisse est une entree (CASH_IN) ou une sortie (CASH_OUT) : " + code);
            }
            if ((type == CashMovementType.CASH_IN) != (request.direction() == CashFlowDirection.IN)) {
                throw new BadRequestException("Le sens et le type de la nature " + code + " ne concordent pas");
            }
            CashOperationNature nature = existing.remove(code);
            if (nature == null) {
                nature = CashOperationNature.builder().code(code).systemValue(false).build();
            }
            nature.setLabel(request.label().trim());
            nature.setFamily(request.family());
            nature.setDirection(request.direction());
            nature.setMovementType(type);
            nature.setAccountNumber(trim(request.accountNumber()));
            nature.setVatCode(trim(request.vatCode()));
            nature.setDocumentRequired(Boolean.TRUE.equals(request.documentRequired()));
            nature.setJustificationDays(request.justificationDays());
            nature.setDefaultSection(trim(request.defaultSection()));
            nature.setMaxPerOperation(request.maxPerOperation());
            nature.setPriorRequestRequired(Boolean.TRUE.equals(request.priorRequestRequired()));
            nature.setDoubleVisaThreshold(request.doubleVisaThreshold());
            nature.setKeywords(trim(request.keywords()));
            nature.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
            nature.setActive(request.active() == null || request.active());
            nature.setUpdatedBy(actor);
            saved.add(nature);
        }
        // Une nature absente est desactivee : les mouvements passes gardent leur nature.
        for (CashOperationNature removed : existing.values()) {
            removed.setActive(false);
            removed.setUpdatedBy(actor);
            saved.add(removed);
        }
        natureRepository.saveAll(saved);
        return natures(null, null, null, true);
    }

    // ------------------------------------------------------- visas et corrections

    @Override
    public CashMovementResponse approveMovement(String movementNumber) {
        CashMovement movement = movement(movementNumber);
        if (movement.getStatus() != CashMovementStatus.PENDING) {
            throw new BadRequestException("Ce mouvement n'attend pas de visa");
        }
        String actor = actors.resolve(null);
        if (CashActorResolver.samePerson(actor, movement.getCreatedBy())
                || CashActorResolver.samePerson(actor, movement.getCashSession().getOpenedBy())) {
            throw new ForbiddenException("Le second visa est donne par une autre personne que celle qui a saisi l'operation");
        }
        if (!actors.hasAuthority(APPROVE)) {
            throw new ForbiddenException("Le visa des operations en attente demande le droit CASH_MOVEMENT:APPROVE");
        }
        confirm(movement, actor);
        emailNotifier.notifyUser(movement.getCreatedBy(), "Operation de caisse visee · " + movement.getMovementNumber(),
                "Votre operation " + movement.getMovementNumber() + " de " + plain(movement.getAmount()) + " XAF a ete visee par " + actor + ".",
                movement.getMovementNumber(), "/payments/cash-registers/sessions/" + movement.getCashSession().getSessionNumber());
        return mapper.toCashMovementResponse(movement);
    }

    @Override
    public void approveInternally(String movementNumber, String approver) {
        CashMovement movement = movement(movementNumber);
        if (movement.getStatus() == CashMovementStatus.PENDING) {
            confirm(movement, approver);
        }
    }

    private void confirm(CashMovement movement, String approver) {
        movement.setStatus(CashMovementStatus.CONFIRMED);
        movement.setApprovedBy(approver);
        movement.setApprovedAt(Instant.now());
        movement.setExpiresAt(null);
        movementRepository.save(movement);
        writer.recompute(movement.getCashSession());
    }

    @Override
    public CashMovementResponse cancelMovement(String movementNumber, CashReasonRequest request) {
        CashMovement movement = movement(movementNumber);
        if (movement.getStatus() != CashMovementStatus.PENDING) {
            throw new BadRequestException("Seul un mouvement en attente s'annule ; un mouvement confirme se contre-passe");
        }
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, movement.getCreatedBy()) && !actors.hasAuthority(APPROVE)) {
            throw new ForbiddenException("Seuls l'auteur et un viseur annulent cette operation");
        }
        String reason = request == null ? null : (StringUtils.hasText(request.reasonText()) ? request.reasonText().trim() : trim(request.note()));
        if (reason == null) {
            throw new BadRequestException("Indiquez le motif de l'annulation");
        }
        movement.setStatus(CashMovementStatus.CANCELLED);
        movement.setCancelledBy(actor);
        movement.setCancelReason(reason);
        movement.setExpiresAt(null);
        movementRepository.save(movement);
        writer.recompute(movement.getCashSession());
        return mapper.toCashMovementResponse(movement);
    }

    @Override
    public CashMovementResponse reverseMovement(String movementNumber, CashReasonRequest request) {
        CashMovement original = movement(movementNumber);
        if (original.getStatus() != CashMovementStatus.CONFIRMED) {
            throw new BadRequestException("Seul un mouvement confirme se contre-passe"
                    + (original.getStatus() == CashMovementStatus.PENDING ? " : celui-ci est en attente, annulez-le" : ""));
        }
        if (NOT_REVERSIBLE_HERE.contains(original.getMovementType())) {
            throw new BadRequestException(switch (original.getMovementType()) {
                case PAYMENT, REFUND -> "Un paiement ou un remboursement s'annule par la transaction de paiement, qui produit le mouvement inverse";
                case TRANSFER_IN, TRANSFER_OUT -> "Un transfert s'annule par son ordre de transfert";
                default -> "Ce mouvement ne se contre-passe pas";
            });
        }
        String actor = actors.resolve(null);
        boolean cashierWindow = CashActorResolver.samePerson(actor, original.getCreatedBy())
                && original.getCreatedAt() != null
                && original.getCreatedAt().plus(Duration.ofMinutes(cashierReversalMinutes())).isAfter(Instant.now())
                && original.getAmount().compareTo(cashierReversalMaxAmount()) < 0;
        if (!cashierWindow && !actors.hasAuthority(REVERSE)) {
            throw new ForbiddenException("Au-dela de " + cashierReversalMinutes() + " minutes ou de " + plain(cashierReversalMaxAmount())
                    + " XAF, la contre-passation demande un superviseur (droit CASH_MOVEMENT:REVERSE)");
        }
        String reason = referenceLists.describe(REVERSAL_REASON, request == null ? null : request.reasonCode(),
                request == null ? null : request.reasonText());
        CashSession target = targetSession(original.getCashSession());
        CashFlowDirection originalDirection = com.sni.bokaticowork.features.payment.service.support.CashFlowRules
                .direction(original.getMovementType(), original.getDirection());
        CashMovement reversal = writer.write(CashMovementWriter.Spec.builder()
                .session(target)
                .type(CashMovementType.ADJUSTMENT)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .documentType(CashDocumentType.CASH_VOUCHER)
                .documentNumber(original.getDocumentNumber())
                .flowCategory("REVERSAL")
                .referenceType("CASH_MOVEMENT")
                .referenceCode(original.getMovementNumber())
                .counterpartyType(original.getCounterpartyType())
                .counterpartyCode(original.getCounterpartyCode())
                .counterpartyName(original.getCounterpartyName())
                .reason("Contre-passation de " + original.getMovementNumber() + " : " + reason)
                .createdBy(actor)
                .direction(originalDirection == CashFlowDirection.OUT ? CashFlowDirection.IN : CashFlowDirection.OUT)
                .natureCode(original.getNatureCode())
                .relatedMovementId(original.getId())
                .build());
        reversal.setReversalReason(reason);
        movementRepository.save(reversal);
        original.setStatus(CashMovementStatus.REVERSED);
        original.setRelatedMovementId(reversal.getId());
        original.setReversalReason(reason);
        movementRepository.save(original);
        writer.recompute(original.getCashSession());
        if (target != original.getCashSession()) {
            writer.recompute(target);
        }
        return mapper.toCashMovementResponse(reversal);
    }

    /**
     * Session ou s'inscrit une correction : celle de l'original si elle est ouverte, sinon la
     * session ouverte de la meme caisse. Une session close n'est jamais reecrite.
     */
    private CashSession targetSession(CashSession original) {
        if (original.getStatus() == CashSessionStatus.OPEN) {
            return original;
        }
        CashRegister register = original.getCashRegister();
        if (register.getRegisterType() == CashRegisterType.SAFE) {
            return writer.safeSession(register);
        }
        return sessionRepository.findBlockingSessionByCashRegisterId(register.getId())
                .filter(session -> session.getStatus() == CashSessionStatus.OPEN)
                .orElseThrow(() -> new BadRequestException("La session d'origine est close et la caisse " + register.getRegisterCode()
                        + " n'a pas de session ouverte : ouvrez-en une pour y inscrire la contre-passation"));
    }

    // ------------------------------------------------------------------ echange

    @Override
    public CashMovementResponse exchange(String sessionNumber, CashExchangeRequest request) {
        CashSession session = sessionRepository.findBySessionNumber(sessionNumber == null ? null : sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + sessionNumber));
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new BadRequestException("L'echange de monnaie se fait sur une session ouverte");
        }
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, session.getOpenedBy())) {
            throw new ForbiddenException("Seul le caissier de la session fait un echange de monnaie");
        }
        CashRegister register = session.getCashRegister();
        BigDecimal given = countRecorder.totals(register, linesOnly(request.given())).total();
        BigDecimal received = countRecorder.totals(register, linesOnly(request.received())).total();
        if (given.signum() <= 0 || given.compareTo(received) != 0) {
            throw new BadRequestException("Un echange donne et recoit le meme montant : " + plain(given) + " contre " + plain(received));
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("given", request.given());
        metadata.put("received", request.received());
        CashMovement movement = writer.write(CashMovementWriter.Spec.builder()
                .session(session).type(CashMovementType.EXCHANGE).amount(given).currency("XAF")
                .documentType(CashDocumentType.CASH_VOUCHER).flowCategory("EXCHANGE")
                .reason(StringUtils.hasText(request.note()) ? request.note().trim() : "Echange de monnaie")
                .createdBy(actor).metadataJson(json(metadata)).build());
        return mapper.toCashMovementResponse(movement);
    }

    private static CashCountRequest linesOnly(List<CashCountRequest.Line> lines) {
        return new CashCountRequest(null, lines, null, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------- transferts

    @Override
    public CashTransferResponse sendTransfer(CashTransferRequest request) {
        String actor = actors.resolve(null);
        CashRegister from = register(request.fromRegisterCode());
        CashRegister to = StringUtils.hasText(request.toRegisterCode()) ? register(request.toRegisterCode())
                : from.getParentRegisterCode() == null ? null : register(from.getParentRegisterCode());
        if (to == null) {
            throw new BadRequestException("Indiquez la caisse destinataire : " + from.getRegisterCode() + " n'a pas de caisse mere");
        }
        BigDecimal amount = request.count() != null ? countRecorder.totals(from, request.count()).total()
                : request.amount() == null ? null : request.amount().setScale(4, RoundingMode.HALF_UP);
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Indiquez un montant a transferer, ou son billetage");
        }
        return send(from, to, amount, kindOf(from, to), actor, request.carrierEmail(), request.note());
    }

    private CashTransferResponse send(CashRegister from, CashRegister to, BigDecimal amount, CashTransferKind kind, String actor,
                                      String carrier, String note) {
        assertCircuit(from, to);
        CashSession session = senderSession(from, actor);
        String number = sequenceGenerator.next("cash_transfer_order");
        CashMovement out = writer.write(CashMovementWriter.Spec.builder()
                .session(session).type(CashMovementType.TRANSFER_OUT).amount(amount).currency("XAF")
                .documentType(CashDocumentType.TRANSFER_NOTE).documentNumber(number).flowCategory(kind.name())
                .referenceType("CASH_TRANSFER_ORDER").referenceCode(number)
                .counterpartyType("CASH_REGISTER").counterpartyCode(to.getRegisterCode()).counterpartyName(to.getName())
                .reason("Transfert vers " + to.getName()).createdBy(actor).transferOrderNumber(number).build());
        CashTransferOrder order = transferRepository.save(CashTransferOrder.builder()
                .transferNumber(number).kind(kind).fromRegister(from.getRegisterCode()).toRegister(to.getRegisterCode())
                .currency("XAF").sentAmount(amount).status(CashTransferStatus.IN_TRANSIT)
                .carrier(StringUtils.hasText(carrier) ? carrier.trim().toLowerCase(Locale.ROOT) : null)
                .sentBy(actor).fromSession(session.getSessionNumber()).outMovement(out.getMovementNumber()).note(trim(note)).build());
        emailNotifier.notifyRegisterManager(to, "Transfert de caisse a recevoir · " + number,
                actor + " envoie " + plain(amount) + " XAF de " + from.getName() + " a " + to.getName()
                        + ". Comptez a la reception et confirmez.", number, "/payments/cash-registers/transfers");
        return toResponse(order);
    }

    @Override
    public CashTransferResponse receiveTransfer(String transferNumber, CashTransferReceiveRequest request) {
        CashTransferOrder order = order(transferNumber);
        if (order.getStatus() != CashTransferStatus.IN_TRANSIT) {
            throw new BadRequestException("Ce transfert n'est plus en transit");
        }
        String actor = actors.resolve(null);
        if (CashActorResolver.samePerson(actor, order.getSentBy())) {
            throw new ForbiddenException("Le receptionnaire d'un transfert ne peut pas en etre l'emetteur");
        }
        CashRegister to = register(order.getToRegister());
        CashSession session = receiverSession(to, actor);
        CashCount count = countRecorder.record(to, session.getSessionNumber(), CashCountMoment.TRANSFER_RECEIPT, request.count(),
                actor, order.getSentAmount(), order.getTransferNumber(), null, null);
        BigDecimal received = count.getTotal();
        Instant now = Instant.now();
        if (received.signum() > 0) {
            CashMovement in = writer.write(CashMovementWriter.Spec.builder()
                    .session(session).type(CashMovementType.TRANSFER_IN).amount(received).currency(order.getCurrency())
                    .documentType(CashDocumentType.TRANSFER_NOTE).documentNumber(order.getTransferNumber())
                    .flowCategory(order.getKind().name()).referenceType("CASH_TRANSFER_ORDER").referenceCode(order.getTransferNumber())
                    .counterpartyType("CASH_REGISTER").counterpartyCode(order.getFromRegister())
                    .reason("Reception du transfert " + order.getTransferNumber()).createdBy(actor)
                    .transferOrderNumber(order.getTransferNumber()).build());
            order.setInMovement(in.getMovementNumber());
        }
        order.setReceivedAmount(received);
        order.setReceivedBy(actor);
        order.setReceivedAt(now);
        order.setToSession(session.getSessionNumber());
        order.setReceiptCount(count.getCountNumber());
        BigDecimal difference = received.subtract(order.getSentAmount());
        if (difference.signum() == 0) {
            order.setStatus(CashTransferStatus.RECEIVED);
        } else {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new BadRequestException("Le montant recu differe du montant envoye (" + plain(order.getSentAmount())
                        + " XAF) : expliquez l'ecart");
            }
            order.setStatus(CashTransferStatus.DISPUTED);
            CashVarianceReview review = reviewRepository.save(CashVarianceReview.builder()
                    .reviewNumber(sequenceGenerator.next("cash_variance_review"))
                    .registerCode(to.getRegisterCode())
                    .sessionNumber(session.getSessionNumber())
                    .kind(CashVarianceKind.TRANSFER)
                    .amount(difference)
                    .countNumber(count.getCountNumber())
                    .cashier(order.getSentBy())
                    .reason("Transfert " + order.getTransferNumber() + " : envoye " + plain(order.getSentAmount()) + ", recu "
                            + plain(received) + ". " + request.reason().trim())
                    .status(CashVarianceStatus.PENDING)
                    .build());
            order.setReviewNumber(review.getReviewNumber());
            emailNotifier.notifySupervisor("Ecart de transfert de caisse · " + order.getTransferNumber(),
                    review.getReason(), order.getTransferNumber(), "/payments/cash-registers/transfers");
        }
        return toResponse(transferRepository.save(order));
    }

    @Override
    public CashTransferResponse cancelTransfer(String transferNumber, CashReasonRequest request) {
        CashTransferOrder order = order(transferNumber);
        if (order.getStatus() != CashTransferStatus.IN_TRANSIT) {
            throw new BadRequestException("Seul un transfert encore en transit s'annule");
        }
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, order.getSentBy()) && !actors.hasAuthority(REVERSE)) {
            throw new ForbiddenException("Seul l'emetteur annule son transfert");
        }
        String reason = request == null ? null : (StringUtils.hasText(request.reasonText()) ? request.reasonText().trim() : trim(request.note()));
        if (reason == null) {
            throw new BadRequestException("Indiquez le motif de l'annulation");
        }
        CashMovement out = movementRepository.findByMovementNumber(order.getOutMovement())
                .orElseThrow(() -> new IllegalStateException("Sortie du transfert introuvable : " + order.getOutMovement()));
        CashSession target = targetSession(out.getCashSession());
        CashMovement back = writer.write(CashMovementWriter.Spec.builder()
                .session(target).type(CashMovementType.ADJUSTMENT).amount(out.getAmount()).currency(out.getCurrency())
                .documentType(CashDocumentType.TRANSFER_NOTE).documentNumber(order.getTransferNumber()).flowCategory("REVERSAL")
                .referenceType("CASH_TRANSFER_ORDER").referenceCode(order.getTransferNumber())
                .reason("Annulation du transfert " + order.getTransferNumber() + " : " + reason).createdBy(actor)
                .direction(CashFlowDirection.IN).transferOrderNumber(order.getTransferNumber()).relatedMovementId(out.getId()).build());
        out.setStatus(CashMovementStatus.REVERSED);
        out.setRelatedMovementId(back.getId());
        out.setReversalReason(reason);
        movementRepository.save(out);
        writer.recompute(out.getCashSession());
        order.setStatus(CashTransferStatus.CANCELLED);
        order.setCancelledBy(actor);
        order.setCancelledAt(Instant.now());
        order.setCancelReason(reason);
        return toResponse(transferRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashTransferResponse> transfers(String registerCode, CashTransferStatus status) {
        List<CashTransferOrder> orders = StringUtils.hasText(registerCode) ? transferRepository.findByRegister(registerCode.trim())
                : transferRepository.findByStatusOrderBySentAtAsc(status == null ? CashTransferStatus.IN_TRANSIT : status);
        return orders.stream().filter(order -> status == null || order.getStatus() == status).map(this::toResponse).toList();
    }

    // ------------------------------------------------------------- petite caisse

    @Override
    @Transactional(readOnly = true)
    public CashPettyRefillResponse pettyRefillProposal(String registerCode) {
        CashRegister petty = register(registerCode);
        if (petty.getRegisterType() != CashRegisterType.PETTY_CASH) {
            throw new BadRequestException("La caisse " + petty.getRegisterCode() + " n'est pas une petite caisse a fonds fixe");
        }
        Instant since = transferRepository.findIncoming(petty.getRegisterCode(), CashTransferKind.PETTY_REFILL,
                        EnumSet.of(CashTransferStatus.IN_TRANSIT, CashTransferStatus.RECEIVED, CashTransferStatus.DISPUTED)).stream()
                .map(CashTransferOrder::getSentAt).findFirst()
                .orElse(petty.getCreatedAt() == null ? Instant.EPOCH : petty.getCreatedAt());
        List<CashMovement> expenses = movementRepository.findByRegisterTypeSince(petty.getRegisterCode(), CashMovementType.CASH_OUT,
                List.of(CashMovementStatus.CONFIRMED), since);
        BigDecimal total = expenses.stream().map(CashMovement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashPettyRefillResponse(petty.getRegisterCode(), petty.getParentRegisterCode(), total.setScale(4, RoundingMode.HALF_UP),
                since, expenses.size(), petty.getStandardFloat());
    }

    @Override
    public CashTransferResponse pettyRefill(String registerCode, String carrierEmail) {
        CashPettyRefillResponse proposal = pettyRefillProposal(registerCode);
        if (proposal.amount().signum() <= 0) {
            throw new BadRequestException("Aucune depense justifiee depuis la derniere realimentation");
        }
        if (proposal.parentRegisterCode() == null) {
            throw new BadRequestException("La petite caisse n'a pas de caisse mere pour la realimenter");
        }
        CashRegister parent = register(proposal.parentRegisterCode());
        // Le montant est calcule par le systeme : exactement le total des depenses depuis la derniere fois.
        return send(parent, register(registerCode), proposal.amount(), CashTransferKind.PETTY_REFILL, actors.resolve(null), carrierEmail,
                proposal.expenseCount() + " depense(s) depuis le " + proposal.since());
    }

    @Override
    public CashTransferResponse remitAtClosing(CashSession session, BigDecimal amount, String actor) {
        CashRegister from = session.getCashRegister();
        if (from.getParentRegisterCode() == null || amount == null || amount.signum() <= 0) {
            return null;
        }
        CashRegister parent = register(from.getParentRegisterCode());
        String number = sequenceGenerator.next("cash_transfer_order");
        CashMovement out = writer.write(CashMovementWriter.Spec.builder()
                .session(session).type(CashMovementType.TRANSFER_OUT).amount(amount).currency("XAF")
                .documentType(CashDocumentType.TRANSFER_NOTE).documentNumber(number).flowCategory(CashTransferKind.REMITTANCE.name())
                .referenceType("CASH_TRANSFER_ORDER").referenceCode(number)
                .counterpartyType("CASH_REGISTER").counterpartyCode(parent.getRegisterCode()).counterpartyName(parent.getName())
                .reason("Remise de fin de session a " + parent.getName()).createdBy(actor).transferOrderNumber(number).build());
        CashTransferOrder order = transferRepository.save(CashTransferOrder.builder()
                .transferNumber(number).kind(CashTransferKind.REMITTANCE).fromRegister(from.getRegisterCode())
                .toRegister(parent.getRegisterCode()).currency("XAF").sentAmount(amount.setScale(4, RoundingMode.HALF_UP))
                .status(CashTransferStatus.IN_TRANSIT).sentBy(actor).fromSession(session.getSessionNumber())
                .outMovement(out.getMovementNumber()).note("Remise a la cloture").build());
        emailNotifier.notifyRegisterManager(parent, "Remise de caisse a recevoir · " + number,
                actor + " remet " + plain(amount) + " XAF de " + from.getName() + " a la cloture de sa session. Comptez a la reception.",
                number, "/payments/cash-registers/transfers");
        return toResponse(order);
    }

    // -------------------------------------------------------- traitement periodique

    @Override
    public void runPeriodicChecks() {
        Instant now = Instant.now();
        for (CashMovement movement : movementRepository.findExpired(CashMovementStatus.PENDING, now)) {
            try {
                movement.setStatus(CashMovementStatus.CANCELLED);
                movement.setCancelledBy("SYSTEM");
                movement.setCancelReason("Visa non donne dans le delai");
                movementRepository.save(movement);
                writer.recompute(movement.getCashSession());
                emailNotifier.notifyUser(movement.getCreatedBy(), "Operation de caisse expiree · " + movement.getMovementNumber(),
                        "Votre operation " + movement.getMovementNumber() + " n'a pas ete visee dans le delai : elle est annulee.",
                        movement.getMovementNumber(), "/payments/cash-registers/sessions/" + movement.getCashSession().getSessionNumber());
            } catch (RuntimeException exception) {
                log.error("Caisse · expiration du mouvement {} en echec : {}", movement.getMovementNumber(), exception.getMessage(), exception);
            }
        }
        for (CashTransferOrder order : transferRepository.findByStatusOrderBySentAtAsc(CashTransferStatus.IN_TRANSIT)) {
            if (order.getTransitAlertedAt() == null && order.getSentAt().plus(Duration.ofHours(maxTransitHours())).isBefore(now)) {
                order.setTransitAlertedAt(now);
                transferRepository.save(order);
                emailNotifier.notifySupervisor("Transfert de caisse en transit depuis plus de " + maxTransitHours() + " h · " + order.getTransferNumber(),
                        "Le transfert " + order.getTransferNumber() + " de " + plain(order.getSentAmount()) + " XAF, envoye par "
                                + order.getSentBy() + " de " + order.getFromRegister() + " vers " + order.getToRegister()
                                + ", n'a pas ete recu.", order.getTransferNumber(), "/payments/cash-registers/transfers");
            }
        }
    }

    // ------------------------------------------------------------------ outils

    /** Circuits autorises : caisse mere et caisses filles, et le coffre du meme site. */
    private void assertCircuit(CashRegister from, CashRegister to) {
        if (from.getRegisterCode().equals(to.getRegisterCode())) {
            throw new BadRequestException("Une caisse ne se transfere pas a elle-meme");
        }
        for (CashRegister register : List.of(from, to)) {
            if (register.getStatus() == CashRegisterStatus.CLOSED || register.getRegisterType() == CashRegisterType.AUTOMATIC) {
                throw new BadRequestException("La caisse " + register.getRegisterCode() + " ne peut pas recevoir ni envoyer de transfert");
            }
        }
        boolean parentChild = to.getRegisterCode().equals(from.getParentRegisterCode()) || from.getRegisterCode().equals(to.getParentRegisterCode());
        boolean safeOfSite = (from.getRegisterType() == CashRegisterType.SAFE || to.getRegisterType() == CashRegisterType.SAFE)
                && from.getLocationCode() != null && from.getLocationCode().equals(to.getLocationCode());
        if (!parentChild && !safeOfSite) {
            throw new BadRequestException("Circuit non autorise : " + from.getRegisterCode() + " vers " + to.getRegisterCode()
                    + ". Les transferts suivent l'arbre des caisses (caisse mere et caisses filles) et le coffre du site.");
        }
    }

    private static CashTransferKind kindOf(CashRegister from, CashRegister to) {
        if (to.getRegisterType() == CashRegisterType.SAFE) {
            return CashTransferKind.SAFE_DEPOSIT;
        }
        if (from.getRegisterType() == CashRegisterType.SAFE) {
            return CashTransferKind.SAFE_WITHDRAWAL;
        }
        if (to.getRegisterCode().equals(from.getParentRegisterCode())) {
            return CashTransferKind.REMITTANCE;
        }
        if (from.getRegisterCode().equals(to.getParentRegisterCode())) {
            return CashTransferKind.FEED;
        }
        return CashTransferKind.OTHER;
    }

    private CashSession senderSession(CashRegister register, String actor) {
        if (register.getRegisterType() == CashRegisterType.SAFE) {
            return writer.safeSession(register);
        }
        return sessionRepository.findBlockingSessionByOpenedBy(actor)
                .filter(session -> session.getCashRegister().getRegisterCode().equals(register.getRegisterCode())
                        && session.getStatus() == CashSessionStatus.OPEN)
                .orElseThrow(() -> new BadRequestException("Ouvrez une session sur la caisse " + register.getRegisterCode()
                        + " pour envoyer un transfert"));
    }

    private CashSession receiverSession(CashRegister register, String actor) {
        if (register.getRegisterType() == CashRegisterType.SAFE) {
            return writer.safeSession(register);
        }
        return sessionRepository.findBlockingSessionByOpenedBy(actor)
                .filter(session -> session.getCashRegister().getRegisterCode().equals(register.getRegisterCode())
                        && session.getStatus() == CashSessionStatus.OPEN)
                .orElseThrow(() -> new BadRequestException("Ouvrez une session sur la caisse " + register.getRegisterCode()
                        + " pour recevoir ce transfert"));
    }

    private CashMovement movement(String movementNumber) {
        return movementRepository.findByMovementNumber(movementNumber == null ? null : movementNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement de caisse introuvable : " + movementNumber));
    }

    private CashTransferOrder order(String transferNumber) {
        return transferRepository.findByTransferNumber(transferNumber == null ? null : transferNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Transfert introuvable : " + transferNumber));
    }

    private CashRegister register(String registerCode) {
        return registerRepository.findByRegisterCode(registerCode == null ? null : registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
    }

    private CashNatureResponse toResponse(CashOperationNature nature) {
        return new CashNatureResponse(nature.getCode(), nature.getLabel(), nature.getFamily(), nature.getDirection(), nature.getMovementType(),
                nature.getAccountNumber(), nature.getVatCode(), nature.getDocumentRequired(), nature.getJustificationDays(),
                nature.getDefaultSection(), nature.getMaxPerOperation(), nature.getPriorRequestRequired(), nature.getDoubleVisaThreshold(),
                nature.getKeywords(), nature.getSortOrder(), nature.getUsageCount(), nature.getActive(), nature.getSystemValue());
    }

    private CashTransferResponse toResponse(CashTransferOrder order) {
        Instant end = order.getReceivedAt() != null ? order.getReceivedAt() : order.getCancelledAt() != null ? order.getCancelledAt() : Instant.now();
        return new CashTransferResponse(order.getTransferNumber(), order.getKind(), order.getFromRegister(), order.getToRegister(),
                order.getCurrency(), order.getSentAmount(), order.getReceivedAmount(),
                order.getReceivedAmount() == null ? null : order.getReceivedAmount().subtract(order.getSentAmount()),
                order.getStatus(), order.getCarrier(), order.getSentBy(), order.getSentAt(), order.getFromSession(), order.getOutMovement(),
                order.getReceivedBy(), order.getReceivedAt(), order.getToSession(), order.getInMovement(), order.getReviewNumber(),
                order.getCancelledBy(), order.getCancelledAt(), order.getCancelReason(),
                order.getSentAt() == null ? null : Duration.between(order.getSentAt(), end).toMinutes(), order.getNote());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
