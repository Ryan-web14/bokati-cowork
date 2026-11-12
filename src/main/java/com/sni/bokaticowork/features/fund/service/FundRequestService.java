package com.sni.bokaticowork.features.fund.service;

import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.support.FrenchAmountWords;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.*;
import com.sni.bokaticowork.features.fund.model.*;
import com.sni.bokaticowork.features.fund.repository.*;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.LinkedSupplier;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashFlowRules;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Demandes de fonds (lot F5) et avances a justifier (lot F6), plan
 * {@code demandes-et-justification.md}.
 *
 * <ul>
 *   <li>tout employe cree une demande, sans session de caisse ;</li>
 *   <li>validation a plusieurs niveaux selon le montant ; le demandeur ne valide jamais la sienne ;</li>
 *   <li>decaissement distinct par la caissiere, depuis sa session : un mouvement de sortie par ligne
 *   (depense directe) ou un seul sur le compte d'avance {@code 421} (avance) ;</li>
 *   <li>justification ligne par ligne d'une avance, validee par un comptable : chaque ligne
 *   validee produit son ecriture (D charge, D TVA, C 421) ; reliquat rendu ou complement verse.</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FundRequestService {

    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    static final Set<FundRequestStatus> OPEN_ADVANCE = EnumSet.of(FundRequestStatus.PARTIALLY_DISBURSED, FundRequestStatus.DISBURSED,
            FundRequestStatus.TO_JUSTIFY, FundRequestStatus.PARTIALLY_JUSTIFIED, FundRequestStatus.JUSTIFIED);
    static final Set<FundRequestType> ADVANCES = EnumSet.of(FundRequestType.ADVANCE);
    static final Set<FundRequestType> TRANSFERS = EnumSet.of(FundRequestType.CASH_SUPPLY, FundRequestType.FUND_REMITTANCE);
    static final String ADVANCE_NATURE = "AVANCE_A_JUSTIFIER";
    static final String RETURN_NATURE = "RETOUR_AVANCE";

    private final FundRequestRepository requestRepository;
    private final FundRequestLineRepository lineRepository;
    private final FundRequestApprovalRepository approvalRepository;
    private final FundApprovalDelegationRepository delegationRepository;
    private final FundDisbursementRepository disbursementRepository;
    private final ExpenseJustificationLineRepository expenseRepository;
    private final CashOperationNatureRepository natureRepository;
    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    @Lazy
    private final CashRegisterService cashRegisterService;
    private final SequenceGeneratorFacade sequences;
    private final CashActorResolver actors;
    private final AccountingEventPublisher accountingEvents;
    private final FundNotifier notifier;
    private final org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.core.accounting.BudgetControl> budgetControl;
    /** Fiches fournisseurs d'inventaire, pour relier une ligne a son fournisseur. */
    private final com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository supplierRepository;

    @Value("${fund.approval.level1-max:50000}")
    private long level1Max;
    @Value("${fund.approval.level2-max:500000}")
    private long level2Max;
    @Value("${fund.request.expiry-days:15}")
    private int expiryDays;
    @Value("${fund.justification.days:7}")
    private int justificationDays;
    @Value("${fund.advance.overdue-policy:BLOCK}")
    private String overduePolicy;
    @Value("${fund.justification.receipt-exempt-max:5000}")
    private long receiptExemptMax;
    @Value("${fund.vat-recoverable-account:4452}")
    private String vatAccount;
    @Value("${fund.budget.blocking-mode:EXTRA_APPROVAL}")
    private String budgetBlockingMode;
    @Value("${fund.budget.unplanned-approval:false}")
    private boolean unplannedApproval;
    @Value("${fund.budget.require-forecast-choice:true}")
    private boolean requireForecastChoice;

    // ------------------------------------------------------------------ saisie

    public FundDtos.Request create(FundDtos.RequestInput input) {
        String actor = actor();
        FundRequest request = requestRepository.save(FundRequest.builder()
                .number(sequences.next("fund_request"))
                .requestType(input.type())
                .status(FundRequestStatus.DRAFT)
                .requester(actor)
                .subject(input.subject().trim())
                .build());
        apply(request, input);
        if (Boolean.TRUE.equals(input.submit())) {
            return submit(request.getNumber());
        }
        return view(request);
    }

    public FundDtos.Request update(String number, FundDtos.RequestInput input) {
        FundRequest request = request(number);
        if (request.getStatus() != FundRequestStatus.DRAFT) {
            throw new BadRequestException("Seule une demande en brouillon (ou renvoyee pour correction) se modifie");
        }
        assertOwner(request);
        request.setRequestType(input.type());
        request.setSubject(input.subject().trim());
        apply(request, input);
        if (Boolean.TRUE.equals(input.submit())) {
            return submit(request.getNumber());
        }
        return view(request);
    }

    /** Copie d'une demande precedente, en brouillon. */
    public FundDtos.Request duplicate(String number) {
        FundRequest source = request(number);
        List<FundRequestLine> lines = lineRepository.findByRequestIdOrderByLineNoAsc(source.getId());
        FundDtos.RequestInput input = new FundDtos.RequestInput(source.getRequestType(), source.getSubject(), source.getRequesterDepartment(),
                source.getRequesterPhone(), source.getBeneficiaryType(), source.getBeneficiaryCode(), source.getBeneficiaryName(),
                source.getBeneficiaryIdNumber(), source.getBeneficiaryPhone(), source.getRegisterCode(), source.getDeliveryMode(), null,
                source.getMissionDestination(), null, null, source.getUnplanned(), source.getUnplannedReason(),
                lines.stream().map(line -> new FundDtos.RequestLine(line.getNatureCode(), line.getDesignation(), line.getAmount(), line.getSupplier(),
                        line.getSection(), line.getSupplierCode())).toList(), null, false);
        return create(input);
    }

    private void apply(FundRequest request, FundDtos.RequestInput input) {
        String actor = request.getRequester();
        if (input.lines().stream().anyMatch(line -> line.amount() == null || line.amount().signum() <= 0)) {
            throw new BadRequestException("Chaque ligne a un montant positif");
        }
        for (FundDtos.RequestLine line : input.lines()) {
            if (StringUtils.hasText(line.natureCode()) && natureRepository.findByCode(line.natureCode().trim().toUpperCase(Locale.ROOT)).isEmpty()) {
                throw new BadRequestException("Nature d'operation inconnue : " + line.natureCode());
            }
        }
        if (input.missionStart() != null && input.missionEnd() != null && input.missionEnd().isBefore(input.missionStart())) {
            throw new BadRequestException("La mission se termine avant de commencer");
        }
        if (Boolean.TRUE.equals(input.unplanned()) && !StringUtils.hasText(input.unplannedReason())) {
            throw new BadRequestException("Une depense non prevue indique son motif");
        }
        FundBeneficiaryType beneficiaryType = input.beneficiaryType() == null ? FundBeneficiaryType.SELF : input.beneficiaryType();
        request.setBeneficiaryType(beneficiaryType);
        if (beneficiaryType == FundBeneficiaryType.SELF) {
            request.setBeneficiaryCode(actor);
            request.setBeneficiaryName(StringUtils.hasText(input.beneficiaryName()) ? input.beneficiaryName().trim() : actor);
        } else {
            if (!StringUtils.hasText(input.beneficiaryCode()) && !StringUtils.hasText(input.beneficiaryName())) {
                throw new BadRequestException("Indiquez le beneficiaire");
            }
            request.setBeneficiaryCode(StringUtils.hasText(input.beneficiaryCode()) ? input.beneficiaryCode().trim() : input.beneficiaryName().trim());
            request.setBeneficiaryName(StringUtils.hasText(input.beneficiaryName()) ? input.beneficiaryName().trim() : input.beneficiaryCode().trim());
        }
        if (beneficiaryType == FundBeneficiaryType.THIRD_PARTY && !StringUtils.hasText(input.beneficiaryIdNumber())) {
            throw new BadRequestException("Un beneficiaire exterieur presente une piece d'identite : indiquez son numero");
        }
        request.setBeneficiaryIdNumber(trim(input.beneficiaryIdNumber()));
        request.setBeneficiaryPhone(trim(input.beneficiaryPhone()));
        request.setRequesterDepartment(trim(input.requesterDepartment()));
        request.setRequesterPhone(trim(input.requesterPhone()));
        request.setRegisterCode(trim(input.registerCode()));
        request.setDeliveryMode(input.deliveryMode() == null ? FundDeliveryMode.CASH : input.deliveryMode());
        request.setWantedDate(input.wantedDate());
        request.setMissionDestination(trim(input.missionDestination()));
        request.setMissionStart(input.missionStart());
        request.setMissionEnd(input.missionEnd());
        request.setUnplanned(Boolean.TRUE.equals(input.unplanned()));
        request.setUnplannedReason(trim(input.unplannedReason()));
        request.setForecastNumber(trim(input.forecastNumber()));
        request.setUnplannedCode(trim(input.unplannedCode()));
        if (request.getForecastNumber() != null && request.getUnplanned()) {
            throw new BadRequestException("Une depense est liee a une prevision ou declaree non prevue, pas les deux");
        }
        request.setAttachments(input.attachments());
        lineRepository.deleteByRequestId(request.getId());
        lineRepository.flush();
        int no = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (FundDtos.RequestLine line : input.lines()) {
            BigDecimal amount = money(line.amount());
            LinkedSupplier.Resolved supplier = LinkedSupplier.resolve(supplierRepository, line.supplierCode(), line.supplier(), no);
            lineRepository.save(FundRequestLine.builder().requestId(request.getId()).lineNo(no++)
                    .natureCode(StringUtils.hasText(line.natureCode()) ? line.natureCode().trim().toUpperCase(Locale.ROOT) : null)
                    .designation(line.designation().trim()).amount(amount).supplier(supplier.name()).supplierCode(supplier.code())
                    .section(trim(line.section())).build());
            total = total.add(amount);
        }
        request.setTotalAmount(total);
        requestRepository.save(request);
    }

    // ------------------------------------------------------------------ circuit

    public FundDtos.Request submit(String number) {
        FundRequest request = request(number);
        if (request.getStatus() != FundRequestStatus.DRAFT) {
            throw new BadRequestException("La demande " + number + " est deja " + request.getStatus());
        }
        assertOwner(request);
        if (lineRepository.findByRequestIdOrderByLineNoAsc(request.getId()).isEmpty()) {
            throw new BadRequestException("Une demande a au moins une ligne");
        }
        FundDtos.Insight insight = insight(request);
        if (Boolean.TRUE.equals(insight.blockedByOverdueAdvance())) {
            throw new BadRequestException(insight.warning());
        }
        // Lot F16 : controle budgetaire a la soumission.
        request.setBudgetApproval(null);
        com.sni.bokaticowork.core.accounting.BudgetControl.Impact budget = insight.budget();
        int levels = insight.requiredLevels();
        if (budget != null && budget.budgetActive()) {
            if (requireForecastChoice && request.getForecastNumber() == null && !request.getUnplanned()) {
                throw new BadRequestException("Choisissez la prevision couverte par cette depense, ou declarez-la non prevue avec son motif");
            }
            if (budget.blocked()) {
                if ("REFUSE".equalsIgnoreCase(budgetBlockingMode)) {
                    throw new BadRequestException("Budget insuffisant : " + String.join(" ; ", budget.messages())
                            + ". Demandez un virement de credits ou une revision.");
                }
                request.setBudgetApproval("OVERRIDE");
                levels++;
            } else if (request.getUnplanned() && unplannedApproval) {
                request.setBudgetApproval("UNPLANNED");
                levels++;
            }
        }
        Instant now = Instant.now();
        request.setRequiredLevels(levels);
        request.setCurrentLevel(1);
        request.setStatus(FundRequestStatus.IN_APPROVAL);
        request.setSubmittedAt(now);
        request.setLevelSince(now);
        request.setApproverRemindedAt(null);
        request.setLastComment(null);
        requestRepository.save(request);
        record(request, 0, FundApprovalDecision.SUBMITTED, null, null, null, null);
        notifier.toApprovers(request, 1);
        return view(request);
    }

    /** Niveau requis par le montant (grille parametrable : 50 000, puis 500 000 XAF). */
    int levelsFor(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(level1Max)) <= 0) {
            return 1;
        }
        return amount.compareTo(BigDecimal.valueOf(level2Max)) <= 0 ? 2 : 3;
    }

    public FundDtos.Request approve(String number, FundDtos.Decision decision) {
        FundRequest request = inApproval(number);
        String delegatedFrom = assertCanDecide(request);
        String comment = decision == null ? null : trim(decision.comment());
        BigDecimal before = amount(request);
        if (decision != null && decision.reducedAmount() != null) {
            BigDecimal reduced = money(decision.reducedAmount());
            if (reduced.signum() <= 0 || reduced.compareTo(before) >= 0) {
                throw new BadRequestException("Le montant reduit est positif et inferieur au montant demande (" + plain(before) + ")");
            }
            if (!StringUtils.hasText(comment)) {
                throw new BadRequestException("Indiquez le motif de la reduction");
            }
            request.setApprovedAmount(reduced);
            record(request, request.getCurrentLevel(), FundApprovalDecision.REDUCED, before, reduced, comment, delegatedFrom);
        } else {
            record(request, request.getCurrentLevel(), FundApprovalDecision.APPROVED, before, before, comment, delegatedFrom);
        }
        request.setLastComment(comment);
        Instant now = Instant.now();
        if (request.getCurrentLevel() >= request.getRequiredLevels()) {
            request.setStatus(FundRequestStatus.APPROVED);
            request.setApprovedAt(now);
            if (request.getApprovedAmount() == null) {
                request.setApprovedAmount(request.getTotalAmount());
            }
            request.setExpiresAt(now.plus(Duration.ofDays(expiryDays)));
            requestRepository.save(request);
            commitBudget(request);
            notifier.approved(request);
        } else {
            request.setCurrentLevel(request.getCurrentLevel() + 1);
            request.setLevelSince(now);
            request.setApproverRemindedAt(null);
            requestRepository.save(request);
            notifier.toApprovers(request, request.getCurrentLevel());
            notifier.toRequester(request, "Votre demande " + request.getNumber() + " a franchi le niveau " + (request.getCurrentLevel() - 1));
        }
        return view(request);
    }

    /** Lot F16 : engagement des montants approuves sur les lignes de budget. */
    private void commitBudget(FundRequest request) {
        com.sni.bokaticowork.core.accounting.BudgetControl control = budgetControl.getIfAvailable();
        if (control == null) {
            return;
        }
        List<com.sni.bokaticowork.core.accounting.BudgetControl.Spend> spends = spends(request);
        LocalDate today = LocalDate.now(ZONE);
        if ("OVERRIDE".equals(request.getBudgetApproval())) {
            control.recordOverride(request.getNumber(), control.check(spends, today),
                    request.getLastComment() == null ? "Derogation accordee a la validation budgetaire" : request.getLastComment(), actor());
        }
        control.commit("FUND_REQUEST", request.getNumber(), spends, today, request.getForecastNumber(), Boolean.TRUE.equals(request.getUnplanned()),
                request.getUnplannedCode(), request.getUnplannedReason(), actor());
    }

    /** Lignes de la demande, au prorata du montant approuve. */
    List<com.sni.bokaticowork.core.accounting.BudgetControl.Spend> spends(FundRequest request) {
        BigDecimal total = request.getTotalAmount();
        BigDecimal approved = amount(request);
        List<com.sni.bokaticowork.core.accounting.BudgetControl.Spend> spends = new ArrayList<>();
        for (FundRequestLine line : lineRepository.findByRequestIdOrderByLineNoAsc(request.getId())) {
            BigDecimal value = total.signum() == 0 || approved.compareTo(total) == 0 ? line.getAmount()
                    : line.getAmount().multiply(approved).divide(total, 4, RoundingMode.HALF_UP);
            String nature = line.getNatureCode();
            if (FundRequestService.ADVANCE_NATURE.equals(nature) && lineRepository.findByRequestIdOrderByLineNoAsc(request.getId()).size() == 1) {
                // Une avance porte son objet sur la nature de la mission : le compte d'avance n'est pas une charge.
                nature = null;
            }
            spends.add(new com.sni.bokaticowork.core.accounting.BudgetControl.Spend(nature, null, line.getSection(), value));
        }
        return spends;
    }

    /** Lot F16 : solde (realise) ou liberation de l'engagement. */
    void settleBudget(FundRequest request, BigDecimal realized) {
        com.sni.bokaticowork.core.accounting.BudgetControl control = budgetControl.getIfAvailable();
        if (control != null) {
            control.settle("FUND_REQUEST", request.getNumber(), realized);
        }
    }

    void releaseBudget(FundRequest request, boolean expired) {
        com.sni.bokaticowork.core.accounting.BudgetControl control = budgetControl.getIfAvailable();
        if (control != null) {
            control.release("FUND_REQUEST", request.getNumber(), expired);
        }
    }

    public FundDtos.Request reject(String number, FundDtos.Decision decision) {
        FundRequest request = inApproval(number);
        String delegatedFrom = assertCanDecide(request);
        String comment = decision == null ? null : trim(decision.comment());
        if (!StringUtils.hasText(comment)) {
            throw new BadRequestException("Indiquez le motif du rejet");
        }
        record(request, request.getCurrentLevel(), FundApprovalDecision.REJECTED, amount(request), null, comment, delegatedFrom);
        request.setStatus(FundRequestStatus.REJECTED);
        request.setLastComment(comment);
        requestRepository.save(request);
        releaseBudget(request, false);
        notifier.toRequester(request, "Votre demande " + request.getNumber() + " est rejetee : " + comment);
        return view(request);
    }

    /** Renvoi pour correction : la demande revient en brouillon avec le commentaire. */
    public FundDtos.Request sendBack(String number, FundDtos.Decision decision) {
        FundRequest request = inApproval(number);
        String delegatedFrom = assertCanDecide(request);
        String comment = decision == null ? null : trim(decision.comment());
        if (!StringUtils.hasText(comment)) {
            throw new BadRequestException("Dites ce qu'il faut corriger");
        }
        record(request, request.getCurrentLevel(), FundApprovalDecision.RETURNED, amount(request), null, comment, delegatedFrom);
        request.setStatus(FundRequestStatus.DRAFT);
        request.setCurrentLevel(0);
        request.setApprovedAmount(null);
        request.setLastComment(comment);
        requestRepository.save(request);
        notifier.toRequester(request, "Votre demande " + request.getNumber() + " vous est renvoyee pour correction : " + comment);
        return view(request);
    }

    public FundDtos.Request cancel(String number, FundDtos.Decision decision) {
        FundRequest request = request(number);
        if (!EnumSet.of(FundRequestStatus.DRAFT, FundRequestStatus.SUBMITTED, FundRequestStatus.IN_APPROVAL, FundRequestStatus.APPROVED)
                .contains(request.getStatus())) {
            throw new BadRequestException("Une demande decaissee ne s'annule plus");
        }
        assertOwner(request);
        record(request, request.getCurrentLevel(), FundApprovalDecision.CANCELLED, amount(request), null,
                decision == null ? null : trim(decision.comment()), null);
        request.setStatus(FundRequestStatus.CANCELLED);
        request.setCancelledAt(Instant.now());
        requestRepository.save(request);
        releaseBudget(request, false);
        return view(request);
    }

    // ------------------------------------------------------------------ decaissement

    /**
     * Decaissement par la caissiere, depuis sa session. Depense directe : un mouvement de sortie par
     * ligne, avec sa nature. Avance : un mouvement sur la nature d'avance ({@code 421}), total ou
     * partiel.
     */
    public FundDtos.Request disburse(String number, FundDtos.Disburse input) {
        FundRequest request = request(number);
        if (request.getStatus() != FundRequestStatus.APPROVED && request.getStatus() != FundRequestStatus.PARTIALLY_DISBURSED) {
            throw new BadRequestException("La demande " + number + " n'est pas a decaisser (" + request.getStatus() + ")");
        }
        if (TRANSFERS.contains(request.getRequestType())) {
            throw new BadRequestException("Un approvisionnement ou une remise de fonds passe par un ordre de transfert entre caisses : "
                    + "creez l'ordre en citant la demande, puis rattachez-le");
        }
        String actor = actor();
        if (CashActorResolver.samePerson(actor, request.getBeneficiaryCode()) || CashActorResolver.samePerson(actor, request.getRequester())) {
            throw new ForbiddenException("La caissiere ne se decaisse pas sa propre demande");
        }
        CashSession session = sessionRepository.findBySessionNumber(input.sessionNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + input.sessionNumber()));
        BigDecimal approved = amount(request);
        BigDecimal remaining = approved.subtract(request.getDisbursedAmount());
        BigDecimal value = input.amount() == null ? remaining : money(input.amount());
        if (value.signum() <= 0 || value.compareTo(remaining) > 0) {
            throw new BadRequestException("Montant a remettre entre 0 et " + plain(remaining) + " XAF");
        }
        BigDecimal available = sessionBalance(session);
        if (available.compareTo(value) < 0) {
            throw new BadRequestException("La caisse n'a que " + plain(available) + " XAF : demandez un approvisionnement avant de decaisser");
        }
        boolean advance = ADVANCES.contains(request.getRequestType()) || request.getRequestType() == FundRequestType.STAFF_ADVANCE;
        if (advance) {
            CashMovementResponse movement = cashOut(request, session, ADVANCE_NATURE, value, request.getSubject());
            disbursement(request, FundDisbursementKind.DISBURSEMENT, movement, session, value, input, actor);
        } else {
            if (value.compareTo(remaining) != 0) {
                throw new BadRequestException("Une depense directe se decaisse en une fois, ligne par ligne");
            }
            BigDecimal factor = approved.compareTo(request.getTotalAmount()) == 0 ? BigDecimal.ONE
                    : approved.divide(request.getTotalAmount(), 10, RoundingMode.HALF_UP);
            BigDecimal left = value;
            List<FundRequestLine> lines = lineRepository.findByRequestIdOrderByLineNoAsc(request.getId());
            for (int index = 0; index < lines.size(); index++) {
                FundRequestLine line = lines.get(index);
                BigDecimal part = index == lines.size() - 1 ? left : money(line.getAmount().multiply(factor));
                left = left.subtract(part);
                if (part.signum() <= 0) {
                    continue;
                }
                String nature = StringUtils.hasText(line.getNatureCode()) ? line.getNatureCode() : null;
                if (nature == null) {
                    throw new BadRequestException("Ligne " + line.getLineNo() + " sans nature d'operation : completez-la avant de decaisser");
                }
                CashMovementResponse movement = cashOut(request, session, nature, part, line.getDesignation());
                disbursement(request, FundDisbursementKind.DISBURSEMENT, movement, session, part, input, actor);
            }
        }
        request.setDisbursedAmount(request.getDisbursedAmount().add(value));
        Instant now = Instant.now();
        request.setDisbursedAt(now);
        if (request.getDisbursedAmount().compareTo(approved) < 0) {
            request.setStatus(FundRequestStatus.PARTIALLY_DISBURSED);
        } else if (advance && request.getRequestType() == FundRequestType.ADVANCE) {
            request.setStatus(FundRequestStatus.TO_JUSTIFY);
            LocalDate base = request.getMissionEnd() != null ? request.getMissionEnd() : LocalDate.now(ZONE);
            request.setJustificationDueDate(base.plusDays(justificationDays));
        } else {
            request.setStatus(FundRequestStatus.SETTLED);
            request.setSettledAt(now);
            settleBudget(request, request.getDisbursedAmount());
        }
        requestRepository.save(request);
        notifier.disbursed(request, value);
        return view(request);
    }

    private CashMovementResponse cashOut(FundRequest request, CashSession session, String nature, BigDecimal amount, String label) {
        return cashRegisterService.createMovement(session.getSessionNumber(), new CreateCashMovementRequest(CashMovementType.CASH_OUT, amount,
                CashDocumentType.EXIT_VOUCHER, request.getNumber(), null, "FUND_REQUEST", request.getNumber(),
                request.getBeneficiaryType() == FundBeneficiaryType.THIRD_PARTY ? "THIRD_PARTY" : "EMPLOYEE", request.getBeneficiaryCode(),
                request.getBeneficiaryName(), label, null, null, null, null, null, null, null, null, null, null, null, nature, null, null, null,
                supplierOf(request), request.getBeneficiaryName(), request.getBeneficiaryIdNumber(), request.getBeneficiaryPhone(), request.getNumber()));
    }

    private String supplierOf(FundRequest request) {
        return request.getRequestType() == FundRequestType.SUPPLIER_PAYMENT
                ? lineRepository.findByRequestIdOrderByLineNoAsc(request.getId()).stream().map(FundRequestLine::getSupplier)
                .filter(StringUtils::hasText).findFirst().orElse(null)
                : null;
    }

    private void disbursement(FundRequest request, FundDisbursementKind kind, CashMovementResponse movement, CashSession session, BigDecimal amount,
                              FundDtos.Disburse input, String actor) {
        disbursementRepository.save(FundDisbursement.builder().requestId(request.getId()).kind(kind).movementNumber(movement.movementNumber())
                .sessionNumber(session.getSessionNumber()).amount(amount).signedByName(input == null ? null : trim(input.signedByName()))
                .signature(input == null ? null : input.signature()).createdBy(actor).build());
    }

    /** Rattache l'ordre de transfert d'un approvisionnement ou d'une remise de fonds. */
    public FundDtos.Request linkTransfer(String number, FundDtos.LinkTransfer input) {
        FundRequest request = request(number);
        if (!TRANSFERS.contains(request.getRequestType()) || request.getStatus() != FundRequestStatus.APPROVED) {
            throw new BadRequestException("Seul un approvisionnement ou une remise de fonds approuve se rattache a un ordre de transfert");
        }
        request.setTransferOrderNumber(input.transferOrderNumber().trim());
        request.setDisbursedAmount(amount(request));
        request.setStatus(FundRequestStatus.SETTLED);
        request.setSettledAt(Instant.now());
        return view(requestRepository.save(request));
    }

    private BigDecimal sessionBalance(CashSession session) {
        return movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId()).stream()
                .map(CashFlowRules::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ justification (F6)

    public FundDtos.Request addExpense(String number, FundDtos.ExpenseInput input) {
        FundRequest request = advance(number);
        String actor = actor();
        if (!CashActorResolver.samePerson(actor, request.getBeneficiaryCode()) && !CashActorResolver.samePerson(actor, request.getRequester())
                && !actors.hasAuthority("FUND_REQUEST:DISBURSE") && !actors.hasAuthority("FUND_JUSTIFICATION:VALIDATE")) {
            throw new ForbiddenException("Seul le beneficiaire, la caisse ou un comptable saisit la justification");
        }
        CashOperationNature nature = natureRepository.findByCode(input.natureCode().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Nature d'operation inconnue : " + input.natureCode()));
        BigDecimal amount = money(input.amount());
        BigDecimal vat = input.vatAmount() == null ? BigDecimal.ZERO.setScale(4) : money(input.vatAmount());
        if (amount.signum() <= 0 || vat.signum() < 0 || vat.compareTo(amount) >= 0) {
            throw new BadRequestException("Montant positif ; TVA positive et inferieure au montant");
        }
        if (!StringUtils.hasText(input.documentReference())) {
            boolean exempt = Boolean.TRUE.equals(input.honourStatement()) && amount.compareTo(BigDecimal.valueOf(receiptExemptMax)) <= 0;
            if (!exempt) {
                throw new BadRequestException("Joignez la piece (ticket, facture) ; sans piece, une attestation sur l'honneur couvre au plus "
                        + receiptExemptMax + " XAF par ligne");
            }
        }
        LocalDate earliest = request.getMissionStart() != null ? request.getMissionStart()
                : request.getDisbursedAt() == null ? null : request.getDisbursedAt().atZone(ZONE).toLocalDate().minusDays(1);
        if (earliest != null && input.expenseDate().isBefore(earliest)) {
            throw new BadRequestException("La depense est datee avant l'avance ou la mission");
        }
        List<ExpenseJustificationLine> existing = expenseRepository.findByRequestIdOrderByLineNoAsc(request.getId());
        LinkedSupplier.Resolved supplier = LinkedSupplier.resolve(supplierRepository, input.supplierCode(), input.supplier(), existing.size() + 1);
        expenseRepository.save(ExpenseJustificationLine.builder().number(sequences.next("expense_justification")).requestId(request.getId())
                .lineNo(existing.size() + 1).expenseDate(input.expenseDate()).natureCode(nature.getCode()).supplier(supplier.name())
                .supplierCode(supplier.code())
                .designation(input.designation().trim()).amount(amount).vatAmount(vat).section(trim(input.section()))
                .documentReference(trim(input.documentReference())).honourStatement(Boolean.TRUE.equals(input.honourStatement()))
                .requestLineNo(input.requestLineNo()).status(ExpenseLineStatus.PENDING).enteredBy(actor).build());
        return view(request);
    }

    public FundDtos.Request deleteExpense(String number, String lineNumber) {
        FundRequest request = advance(number);
        ExpenseJustificationLine line = expense(request, lineNumber);
        if (line.getStatus() == ExpenseLineStatus.VALIDATED) {
            throw new BadRequestException("Une ligne validee est comptabilisee : elle ne se supprime plus");
        }
        expenseRepository.delete(line);
        return view(request);
    }

    /** Validation d'une ligne par un comptable : elle produit son ecriture (D charge, D TVA, C 421). */
    public FundDtos.Request validateExpense(String number, String lineNumber) {
        FundRequest request = advance(number);
        ExpenseJustificationLine line = expense(request, lineNumber);
        if (line.getStatus() != ExpenseLineStatus.PENDING) {
            throw new BadRequestException("La ligne " + lineNumber + " est deja " + line.getStatus());
        }
        String actor = actor();
        if (CashActorResolver.samePerson(actor, request.getBeneficiaryCode())) {
            throw new ForbiddenException("Le beneficiaire ne valide pas sa propre justification");
        }
        line.setStatus(ExpenseLineStatus.VALIDATED);
        line.setValidatedBy(actor);
        line.setValidatedAt(Instant.now());
        expenseRepository.save(line);
        CashOperationNature nature = natureRepository.findByCode(line.getNatureCode()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNumber", line.getNumber());
        data.put("requestNumber", request.getNumber());
        data.put("journalCode", "AV");
        data.put("natureAccount", nature == null || !StringUtils.hasText(nature.getAccountNumber()) ? "471" : nature.getAccountNumber());
        data.put("vatAccount", vatAccount);
        data.put("amount", line.getAmount());
        data.put("netAmount", line.getAmount().subtract(line.getVatAmount()));
        data.put("vatAmount", line.getVatAmount());
        data.put("designation", line.getDesignation());
        data.put("supplier", line.getSupplier());
        data.put("section", line.getSection());
        data.put("natureSection", nature == null ? null : nature.getDefaultSection());
        data.put("beneficiaryCode", request.getBeneficiaryCode());
        accountingEvents.publish("EXPENSE_JUSTIFIED", "FUNDING", line.getNumber(), line.getExpenseDate(), data);
        recompute(request);
        notifier.toBeneficiary(request, "La depense « " + line.getDesignation() + " » (" + plain(line.getAmount()) + " XAF) est validee");
        return view(request);
    }

    public FundDtos.Request rejectExpense(String number, String lineNumber, FundDtos.Reject input) {
        FundRequest request = advance(number);
        ExpenseJustificationLine line = expense(request, lineNumber);
        if (line.getStatus() != ExpenseLineStatus.PENDING) {
            throw new BadRequestException("La ligne " + lineNumber + " est deja " + line.getStatus());
        }
        line.setStatus(ExpenseLineStatus.REJECTED);
        line.setRejectReason(input.reason().trim());
        line.setValidatedBy(actor());
        line.setValidatedAt(Instant.now());
        expenseRepository.save(line);
        notifier.toBeneficiary(request, "La depense « " + line.getDesignation() + " » est rejetee : " + input.reason().trim()
                + ". Remplacez-la ou remboursez le montant.");
        return view(request);
    }

    /** Retour du reliquat au guichet (bon d'entree) : D caisse, C 421. */
    public FundDtos.Request returnRemainder(String number, FundDtos.CashBack input) {
        FundRequest request = advance(number);
        BigDecimal remaining = remainingToJustify(request);
        if (remaining.signum() <= 0) {
            throw new BadRequestException("Rien a rendre sur cette avance");
        }
        BigDecimal value = input.amount() == null ? remaining : money(input.amount());
        if (value.signum() <= 0 || value.compareTo(remaining) > 0) {
            throw new BadRequestException("Montant rendu entre 0 et " + plain(remaining) + " XAF");
        }
        CashSession session = sessionRepository.findBySessionNumber(input.sessionNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + input.sessionNumber()));
        String actor = actor();
        CashMovementResponse movement = cashRegisterService.createMovement(session.getSessionNumber(), new CreateCashMovementRequest(
                CashMovementType.CASH_IN, value, CashDocumentType.ENTRY_VOUCHER, request.getNumber(), null, "FUND_REQUEST", request.getNumber(),
                "EMPLOYEE", request.getBeneficiaryCode(), request.getBeneficiaryName(), "Retour du reliquat de l'avance " + request.getNumber(),
                null, null, null, null, null, null, null, null, null, null, null, RETURN_NATURE, null, null, null, null,
                request.getBeneficiaryName(), request.getBeneficiaryIdNumber(), request.getBeneficiaryPhone(), request.getNumber()));
        disbursement(request, FundDisbursementKind.RETURN, movement, session, value,
                new FundDtos.Disburse(input.sessionNumber(), value, input.signedByName(), input.signature()), actor);
        request.setReturnedAmount(request.getReturnedAmount().add(value));
        recompute(request);
        return view(request);
    }

    /** Complement verse quand les depenses validees depassent l'avance : D 421, C caisse. */
    public FundDtos.Request payComplement(String number, FundDtos.CashBack input) {
        FundRequest request = advance(number);
        if (expenseRepository.findByRequestIdOrderByLineNoAsc(request.getId()).stream().anyMatch(line -> line.getStatus() == ExpenseLineStatus.PENDING)) {
            throw new BadRequestException("Des lignes attendent encore leur validation");
        }
        BigDecimal due = request.getJustifiedAmount().subtract(request.getDisbursedAmount().add(request.getComplementAmount()))
                .add(request.getReturnedAmount());
        if (due.signum() <= 0) {
            throw new BadRequestException("Aucun complement du : les depenses validees ne depassent pas l'avance");
        }
        String actor = actor();
        if (CashActorResolver.samePerson(actor, request.getBeneficiaryCode())) {
            throw new ForbiddenException("La caissiere ne se verse pas son propre complement");
        }
        CashSession session = sessionRepository.findBySessionNumber(input.sessionNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + input.sessionNumber()));
        CashMovementResponse movement = cashOut(request, session, ADVANCE_NATURE, due, "Complement de l'avance " + request.getNumber());
        disbursement(request, FundDisbursementKind.COMPLEMENT, movement, session, due,
                new FundDtos.Disburse(input.sessionNumber(), due, input.signedByName(), input.signature()), actor);
        request.setComplementAmount(request.getComplementAmount().add(due));
        recompute(request);
        return view(request);
    }

    /** Recalcule le justifie et l'etat d'une avance ; soldee quand avance + complement = justifie + rendu. */
    void recompute(FundRequest request) {
        BigDecimal justified = expenseRepository.findByRequestIdOrderByLineNoAsc(request.getId()).stream()
                .filter(line -> line.getStatus() == ExpenseLineStatus.VALIDATED).map(ExpenseJustificationLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        request.setJustifiedAmount(money(justified));
        BigDecimal given = request.getDisbursedAmount().add(request.getComplementAmount());
        BigDecimal covered = justified.add(request.getReturnedAmount());
        if (covered.compareTo(given) == 0 && given.signum() > 0) {
            request.setStatus(FundRequestStatus.SETTLED);
            request.setSettledAt(Instant.now());
            settleBudget(request, justified);
            notifier.toBeneficiary(request, "Votre avance " + request.getNumber() + " est soldee");
        } else if (justified.compareTo(given) >= 0) {
            request.setStatus(FundRequestStatus.JUSTIFIED);
        } else if (justified.signum() > 0) {
            request.setStatus(FundRequestStatus.PARTIALLY_JUSTIFIED);
        } else {
            request.setStatus(FundRequestStatus.TO_JUSTIFY);
        }
        requestRepository.save(request);
    }

    BigDecimal remainingToJustify(FundRequest request) {
        return request.getDisbursedAmount().add(request.getComplementAmount()).subtract(request.getJustifiedAmount()).subtract(request.getReturnedAmount());
    }

    // ------------------------------------------------------------------ delegations

    public FundDtos.DelegationView delegate(FundDtos.Delegation input) {
        String actor = actor();
        if (!actors.hasAuthority(levelPermission(input.level()))) {
            throw new ForbiddenException("On ne delegue que le niveau de validation qu'on detient");
        }
        if (CashActorResolver.samePerson(actor, input.delegate())) {
            throw new BadRequestException("On ne se delegue pas a soi-meme");
        }
        FundApprovalDelegation delegation = delegationRepository.save(FundApprovalDelegation.builder().delegator(actor)
                .delegate(input.delegate().trim()).level(input.level()).startsOn(input.startsOn()).endsOn(input.endsOn()).createdBy(actor).build());
        return toView(delegation);
    }

    public void cancelDelegation(Long id) {
        FundApprovalDelegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation introuvable : " + id));
        delegation.setCancelledAt(Instant.now());
        delegationRepository.save(delegation);
    }

    @Transactional(readOnly = true)
    public List<FundDtos.DelegationView> delegations() {
        return delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc().stream().map(FundRequestService::toView).toList();
    }

    private static FundDtos.DelegationView toView(FundApprovalDelegation delegation) {
        return new FundDtos.DelegationView(delegation.getId(), delegation.getDelegator(), delegation.getDelegate(), delegation.getLevel(),
                delegation.getStartsOn(), delegation.getEndsOn());
    }

    /** Le valideur du niveau courant (ou son delegue) ; renvoie le delegant le cas echeant. */
    private String assertCanDecide(FundRequest request) {
        String actor = actor();
        if (CashActorResolver.samePerson(actor, request.getRequester()) || CashActorResolver.samePerson(actor, request.getBeneficiaryCode())) {
            throw new ForbiddenException("On ne valide jamais sa propre demande, meme avec le niveau requis");
        }
        int level = request.getCurrentLevel();
        if (request.getBudgetApproval() != null && level == request.getRequiredLevels()) {
            String permission = "OVERRIDE".equals(request.getBudgetApproval()) ? "BUDGET:OVERRIDE" : "BUDGET:APPROVE_UNPLANNED";
            if (!actors.hasAuthority(permission)) {
                throw new ForbiddenException("La validation budgetaire demande le droit " + permission);
            }
            return null;
        }
        if (actors.hasAuthority(levelPermission(level))) {
            return null;
        }
        LocalDate today = LocalDate.now(ZONE);
        return delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc().stream()
                .filter(delegation -> delegation.getLevel() == level && CashActorResolver.samePerson(delegation.getDelegate(), actor))
                .filter(delegation -> !delegation.getStartsOn().isAfter(today) && !delegation.getEndsOn().isBefore(today))
                .filter(delegation -> !CashActorResolver.samePerson(delegation.getDelegator(), request.getRequester()))
                .map(FundApprovalDelegation::getDelegator)
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("Ce niveau de validation demande le droit " + levelPermission(level)));
    }

    static String levelPermission(int level) {
        return "FUND_REQUEST:APPROVE_L" + Math.max(1, Math.min(3, level));
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public FundDtos.Request get(String number) {
        FundRequest request = request(number);
        String actor = actor();
        if (!CashActorResolver.samePerson(actor, request.getRequester()) && !CashActorResolver.samePerson(actor, request.getBeneficiaryCode())
                && !actors.hasAuthority("FUND_REQUEST:READ") && !actors.hasAuthority("FUND_REQUEST:DISBURSE") && !actors.hasAuthority("FUND_JUSTIFICATION:VALIDATE")
                && !actors.hasAuthority(levelPermission(Math.max(1, request.getCurrentLevel())))) {
            throw new ForbiddenException("Cette demande ne vous concerne pas");
        }
        return view(request);
    }

    @Transactional(readOnly = true)
    public List<FundDtos.Request> mine() {
        String actor = actor();
        return requestRepository.findByRequesterOrBeneficiaryCodeOrderByCreatedAtDesc(actor, actor).stream().map(this::summary).toList();
    }

    /** File de validation : demandes au niveau que l'utilisateur detient (ou par delegation). */
    @Transactional(readOnly = true)
    public List<FundDtos.Request> toApprove() {
        String actor = actor();
        LocalDate today = LocalDate.now(ZONE);
        Set<Integer> levels = new HashSet<>();
        for (int level = 1; level <= 3; level++) {
            if (actors.hasAuthority(levelPermission(level))) {
                levels.add(level);
            }
        }
        delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc().stream()
                .filter(delegation -> CashActorResolver.samePerson(delegation.getDelegate(), actor))
                .filter(delegation -> !delegation.getStartsOn().isAfter(today) && !delegation.getEndsOn().isBefore(today))
                .forEach(delegation -> levels.add(delegation.getLevel()));
        return requestRepository.findByStatusInOrderByCreatedAtAsc(EnumSet.of(FundRequestStatus.IN_APPROVAL)).stream()
                .filter(request -> request.getBudgetApproval() != null && request.getCurrentLevel().equals(request.getRequiredLevels())
                        ? actors.hasAuthority("OVERRIDE".equals(request.getBudgetApproval()) ? "BUDGET:OVERRIDE" : "BUDGET:APPROVE_UNPLANNED")
                        : levels.contains(request.getCurrentLevel()))
                .filter(request -> !CashActorResolver.samePerson(actor, request.getRequester()))
                .map(this::summary).toList();
    }

    /** File « A decaisser » d'une caisse (ou de toutes), triee par date souhaitee. */
    @Transactional(readOnly = true)
    public List<FundDtos.Request> toDisburse(String registerCode) {
        return requestRepository.findByStatusInOrderByCreatedAtAsc(EnumSet.of(FundRequestStatus.APPROVED, FundRequestStatus.PARTIALLY_DISBURSED,
                        FundRequestStatus.JUSTIFIED, FundRequestStatus.PARTIALLY_JUSTIFIED)).stream()
                .filter(request -> request.getStatus() == FundRequestStatus.APPROVED || request.getStatus() == FundRequestStatus.PARTIALLY_DISBURSED
                        || remainingToJustify(request).signum() < 0)
                .filter(request -> !StringUtils.hasText(registerCode) || registerCode.equalsIgnoreCase(request.getRegisterCode())
                        || !StringUtils.hasText(request.getRegisterCode()))
                .sorted(Comparator.comparing((FundRequest request) -> request.getWantedDate() == null ? LocalDate.MAX : request.getWantedDate()))
                .map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<FundDtos.Request> all(FundRequestStatus status) {
        return requestRepository.findByStatusInOrderByCreatedAtAsc(status == null ? EnumSet.allOf(FundRequestStatus.class) : EnumSet.of(status))
                .stream().map(this::summary).toList();
    }

    /** Etat des avances ouvertes par beneficiaire, anciennete et reste. */
    @Transactional(readOnly = true)
    public List<FundDtos.AdvanceRow> openAdvances() {
        LocalDate today = LocalDate.now(ZONE);
        return requestRepository.findByStatusInOrderByCreatedAtAsc(OPEN_ADVANCE).stream()
                .filter(request -> request.getRequestType() == FundRequestType.ADVANCE)
                .map(request -> new FundDtos.AdvanceRow(request.getNumber(), request.getBeneficiaryCode(), request.getBeneficiaryName(),
                        request.getDisbursedAmount().add(request.getComplementAmount()), request.getJustifiedAmount(), request.getReturnedAmount(),
                        remainingToJustify(request), request.getJustificationDueDate(),
                        request.getJustificationDueDate() == null || !today.isAfter(request.getJustificationDueDate()) ? 0L
                                : ChronoUnit.DAYS.between(request.getJustificationDueDate(), today), request.getStatus()))
                .toList();
    }

    /** Lignes de justification en attente de validation (file du comptable). */
    @Transactional(readOnly = true)
    public List<FundDtos.Request> justificationsToValidate() {
        Set<Long> ids = new LinkedHashSet<>();
        expenseRepository.findByStatusOrderByCreatedAtAsc(ExpenseLineStatus.PENDING).forEach(line -> ids.add(line.getRequestId()));
        return requestRepository.findAllById(ids).stream().map(this::view).toList();
    }

    /** Ce que la demande va devenir : niveaux, solde de la caisse, avances ouvertes du beneficiaire. */
    public FundDtos.Insight insight(FundRequest request) {
        BigDecimal amount = amount(request);
        int levels = levelsFor(amount);
        List<String> labels = new ArrayList<>(List.of("Chef de service", "Responsable financier", "Direction")).subList(0, levels);
        BigDecimal balance = null;
        if (StringUtils.hasText(request.getRegisterCode())) {
            Optional<CashRegister> register = registerRepository.findByRegisterCode(request.getRegisterCode());
            if (register.isPresent()) {
                balance = sessionRepository.findBlockingSessionByCashRegisterId(register.get().getId()).map(this::sessionBalance)
                        .orElse(register.get().getLastCountAmount());
            }
        }
        LocalDate today = LocalDate.now(ZONE);
        List<FundRequest> open = StringUtils.hasText(request.getBeneficiaryCode())
                ? requestRepository.findByBeneficiaryCodeAndStatusIn(request.getBeneficiaryCode(), OPEN_ADVANCE).stream()
                .filter(other -> !other.getId().equals(request.getId()) && other.getRequestType() == FundRequestType.ADVANCE).toList()
                : List.of();
        boolean overdue = open.stream().anyMatch(other -> other.getJustificationDueDate() != null && today.isAfter(other.getJustificationDueDate()));
        boolean blocked = overdue && "BLOCK".equalsIgnoreCase(overduePolicy) && request.getRequestType() == FundRequestType.ADVANCE;
        String warning = overdue ? "Le beneficiaire a une avance en retard de justification" + (blocked ? " : aucune nouvelle avance avant sa justification" : "")
                : balance != null && balance.compareTo(amount) < 0 ? "La caisse ne peut pas couvrir ce montant aujourd'hui" : null;
        com.sni.bokaticowork.core.accounting.BudgetControl control = budgetControl.getIfAvailable();
        com.sni.bokaticowork.core.accounting.BudgetControl.Impact budget = control == null || request.getId() == null ? null
                : control.check(spends(request), LocalDate.now(ZONE));
        List<String> allLabels = new ArrayList<>(labels);
        String budgetApproval = null;
        if (budget != null && budget.blocked() && !"REFUSE".equalsIgnoreCase(budgetBlockingMode)) {
            budgetApproval = "OVERRIDE";
        } else if (budget != null && budget.budgetActive() && Boolean.TRUE.equals(request.getUnplanned()) && unplannedApproval) {
            budgetApproval = "UNPLANNED";
        }
        if (budgetApproval != null) {
            allLabels.add("Validation budgetaire");
        }
        return new FundDtos.Insight(levels, allLabels, balance, balance == null ? null : balance.compareTo(amount) >= 0,
                open.stream().map(other -> other.getNumber() + " : reste " + plain(remainingToJustify(other)) + " XAF").toList(), blocked, warning,
                budget, budgetApproval);
    }

    @Transactional(readOnly = true)
    public FundDtos.Insight insight(String number) {
        return insight(request(number));
    }

    FundDtos.Request summary(FundRequest request) {
        return build(request, false);
    }

    FundDtos.Request view(FundRequest request) {
        return build(request, true);
    }

    private FundDtos.Request build(FundRequest request, boolean full) {
        List<FundDtos.RequestLine> lines = lineRepository.findByRequestIdOrderByLineNoAsc(request.getId()).stream()
                .map(line -> new FundDtos.RequestLine(line.getNatureCode(), line.getDesignation(), line.getAmount(), line.getSupplier(), line.getSection(),
                        line.getSupplierCode()))
                .toList();
        List<FundDtos.Approval> timeline = full ? approvalRepository.findByRequestIdOrderByDecidedAtAscIdAsc(request.getId()).stream()
                .map(approval -> new FundDtos.Approval(approval.getLevel(), approval.getApprover(), approval.getDecision(), approval.getAmountBefore(),
                        approval.getAmountAfter(), approval.getComment(), approval.getDelegatedFrom(), approval.getDecidedAt())).toList() : List.of();
        List<FundDtos.Disbursement> disbursements = full ? disbursementRepository.findByRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .map(item -> new FundDtos.Disbursement(item.getKind(), item.getMovementNumber(), item.getSessionNumber(), item.getAmount(),
                        item.getSignedByName(), item.getCreatedBy(), item.getCreatedAt())).toList() : List.of();
        List<FundDtos.ExpenseLine> justification = full ? expenseRepository.findByRequestIdOrderByLineNoAsc(request.getId()).stream()
                .map(line -> new FundDtos.ExpenseLine(line.getNumber(), line.getLineNo(), line.getExpenseDate(), line.getNatureCode(), line.getSupplier(),
                        line.getDesignation(), line.getAmount(), line.getVatAmount(), line.getSection(), line.getDocumentReference(),
                        line.getHonourStatement(), line.getRequestLineNo(), line.getStatus(), line.getRejectReason(), line.getEnteredBy(),
                        line.getValidatedBy(), line.getValidatedAt(), line.getSupplierCode())).toList() : List.of();
        BigDecimal approved = amount(request);
        return new FundDtos.Request(request.getNumber(), request.getRequestType(), request.getStatus(), request.getRequester(),
                request.getRequesterDepartment(), request.getRequesterPhone(), request.getBeneficiaryType(), request.getBeneficiaryCode(),
                request.getBeneficiaryName(), request.getBeneficiaryIdNumber(), request.getBeneficiaryPhone(), request.getRegisterCode(),
                request.getSubject(), request.getTotalAmount(), FrenchAmountWords.withCurrency(request.getTotalAmount(), "XAF"),
                request.getApprovedAmount(), request.getDisbursedAmount(), request.getJustifiedAmount(), request.getReturnedAmount(),
                request.getComplementAmount(), request.getRequestType() == FundRequestType.ADVANCE ? remainingToJustify(request) : BigDecimal.ZERO,
                EnumSet.of(FundRequestStatus.APPROVED, FundRequestStatus.PARTIALLY_DISBURSED).contains(request.getStatus())
                        ? approved.subtract(request.getDisbursedAmount()) : BigDecimal.ZERO,
                request.getDeliveryMode(), request.getWantedDate(), request.getMissionDestination(), request.getMissionStart(), request.getMissionEnd(),
                request.getUnplanned(), request.getUnplannedReason(), request.getNeedStatementNumber(), request.getTransferOrderNumber(),
                request.getRequiredLevels(), request.getCurrentLevel(), request.getLastComment(), request.getSubmittedAt(), request.getApprovedAt(),
                request.getExpiresAt(), request.getDisbursedAt(), request.getJustificationDueDate(), request.getSettledAt(),
                request.getLegacyRequestNumber(), request.getAttachments(), request.getCreatedAt(), lines, timeline, disbursements, justification,
                full && EnumSet.of(FundRequestStatus.DRAFT, FundRequestStatus.IN_APPROVAL, FundRequestStatus.APPROVED).contains(request.getStatus())
                        ? insight(request) : null);
    }

    // ------------------------------------------------------------------ outils

    FundRequest request(String number) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException("Numero de demande manquant");
        }
        return requestRepository.findByNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Demande de fonds introuvable : " + number));
    }

    private FundRequest inApproval(String number) {
        FundRequest request = request(number);
        if (request.getStatus() != FundRequestStatus.IN_APPROVAL) {
            throw new BadRequestException("La demande " + number + " n'est pas en validation (" + request.getStatus() + ")");
        }
        return request;
    }

    private FundRequest advance(String number) {
        FundRequest request = request(number);
        if (request.getRequestType() != FundRequestType.ADVANCE || !OPEN_ADVANCE.contains(request.getStatus())) {
            throw new BadRequestException("La demande " + number + " n'est pas une avance a justifier en cours");
        }
        return request;
    }

    private ExpenseJustificationLine expense(FundRequest request, String lineNumber) {
        ExpenseJustificationLine line = expenseRepository.findByNumber(lineNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de justification introuvable : " + lineNumber));
        if (!line.getRequestId().equals(request.getId())) {
            throw new BadRequestException("La ligne " + lineNumber + " n'appartient pas a la demande " + request.getNumber());
        }
        return line;
    }

    private void assertOwner(FundRequest request) {
        if (!CashActorResolver.samePerson(actor(), request.getRequester())) {
            throw new ForbiddenException("Seul le demandeur agit sur sa demande a ce stade");
        }
    }

    void record(FundRequest request, int level, FundApprovalDecision decision, BigDecimal before, BigDecimal after, String comment,
                String delegatedFrom) {
        approvalRepository.save(FundRequestApproval.builder().requestId(request.getId()).level(level).approver(actor()).decision(decision)
                .amountBefore(before).amountAfter(after).comment(comment).delegatedFrom(delegatedFrom).build());
    }

    BigDecimal amount(FundRequest request) {
        return request.getApprovedAmount() != null ? request.getApprovedAmount() : request.getTotalAmount();
    }

    String actor() {
        return actors.resolve("SYSTEM");
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
