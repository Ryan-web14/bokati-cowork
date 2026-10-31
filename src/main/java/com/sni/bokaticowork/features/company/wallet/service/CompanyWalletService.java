package com.sni.bokaticowork.features.company.wallet.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.wallet.dto.CompanyWalletDtos;
import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.model.SpendOperation;
import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.company.wallet.repository.SpendingAuthorizationRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.service.WalletMerchantPaymentGuard;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTopUpService;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Le portefeuille de l'organisation et les depenses que ses collaborateurs y font.
 *
 * <p>Le portefeuille est un {@code wallet_account} au nom de l'organisation ({@code CUSTOMER}).
 * Payer avec lui reste interdit a quiconque n'en est pas titulaire, sauf ici : un collaborateur
 * rattache, sous une autorisation en vigueur qui couvre l'operation et le montant. L'intention de
 * paiement est alors creee au nom de l'organisation, la facture en source, et le collaborateur
 * note comme celui qui a depense. La verification « le titulaire du portefeuille est le client de
 * l'intention » du paiement par portefeuille reste donc entiere.</p>
 *
 * <p>Au-dela du seuil de validation, la depense attend le gerant : ses fonds sont retenus sur le
 * portefeuille, et sans reponse sous {@code bokati.company.wallet.approval-hours} (48 par defaut)
 * elle est annulee et les fonds rendus.</p>
 */
@Slf4j
@Service
@Transactional
public class CompanyWalletService {

    static final String OWNER_TYPE = "CUSTOMER";
    static final String HOLD_SOURCE = "COMPANY_SPEND";
    static final String INTENT_PURPOSE = "COMPANY_WALLET_SPEND";
    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";

    private final WalletService walletService;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerService ledgerService;
    private final WalletHoldRepository holdRepository;
    private final WalletHoldService holdService;
    private final WalletAdminActionService adminActionService;
    private final WalletTopUpService topUpService;
    private final WalletMerchantPaymentGuard paymentGuard;
    private final PaymentService paymentService;
    private final BillingDocumentService billingDocumentService;
    private final SpendingAuthorizationRepository authorizationRepository;
    private final CompanyWalletSpendRepository spendRepository;
    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final CustomerMemberLinkRepository linkRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final MemberInAppNotifier inAppNotifier;
    private final Clock clock;
    private final String defaultCurrency;
    private final Duration approvalWindow;

    @org.springframework.beans.factory.annotation.Autowired
    public CompanyWalletService(WalletService walletService, WalletAccountRepository walletRepository,
                                WalletLedgerService ledgerService, WalletHoldRepository holdRepository,
                                WalletHoldService holdService, WalletAdminActionService adminActionService,
                                WalletTopUpService topUpService, WalletMerchantPaymentGuard paymentGuard,
                                @Lazy PaymentService paymentService, BillingDocumentService billingDocumentService,
                                SpendingAuthorizationRepository authorizationRepository,
                                CompanyWalletSpendRepository spendRepository, CustomerRepository customerRepository,
                                MemberRepository memberRepository, CustomerMemberLinkRepository linkRepository,
                                SequenceGeneratorFacade sequenceGenerator, MemberInAppNotifier inAppNotifier,
                                @Value("${bokati.company.wallet.currency:XAF}") String defaultCurrency,
                                @Value("${bokati.company.wallet.approval-hours:48}") long approvalHours) {
        this(walletService, walletRepository, ledgerService, holdRepository, holdService, adminActionService, topUpService,
                paymentGuard, paymentService, billingDocumentService, authorizationRepository, spendRepository,
                customerRepository, memberRepository, linkRepository, sequenceGenerator, inAppNotifier,
                Clock.systemDefaultZone(), defaultCurrency, Duration.ofHours(approvalHours));
    }

    CompanyWalletService(WalletService walletService, WalletAccountRepository walletRepository,
                         WalletLedgerService ledgerService, WalletHoldRepository holdRepository,
                         WalletHoldService holdService, WalletAdminActionService adminActionService,
                         WalletTopUpService topUpService, WalletMerchantPaymentGuard paymentGuard,
                         PaymentService paymentService, BillingDocumentService billingDocumentService,
                         SpendingAuthorizationRepository authorizationRepository,
                         CompanyWalletSpendRepository spendRepository, CustomerRepository customerRepository,
                         MemberRepository memberRepository, CustomerMemberLinkRepository linkRepository,
                         SequenceGeneratorFacade sequenceGenerator, MemberInAppNotifier inAppNotifier,
                         Clock clock, String defaultCurrency, Duration approvalWindow) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.ledgerService = ledgerService;
        this.holdRepository = holdRepository;
        this.holdService = holdService;
        this.adminActionService = adminActionService;
        this.topUpService = topUpService;
        this.paymentGuard = paymentGuard;
        this.paymentService = paymentService;
        this.billingDocumentService = billingDocumentService;
        this.authorizationRepository = authorizationRepository;
        this.spendRepository = spendRepository;
        this.customerRepository = customerRepository;
        this.memberRepository = memberRepository;
        this.linkRepository = linkRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.inAppNotifier = inAppNotifier;
        this.clock = clock;
        this.defaultCurrency = defaultCurrency;
        this.approvalWindow = approvalWindow;
    }

    // ── Portefeuille ─────────────────────────────────────────────────────────

    /** Le portefeuille de l'organisation dans la devise, ouvert au besoin. */
    public WalletAccount wallet(Customer company, String currency) {
        String code = walletService.getOrCreate(OWNER_TYPE, company.getCustomerId(), currency(currency)).walletNumber();
        return walletService.serviceWallet(code);
    }

    /** Solde, derniers mouvements avec le collaborateur de chaque depense, et validations en attente. */
    public CompanyWalletDtos.WalletView view(Customer company, String currency) {
        WalletAccount wallet = wallet(company, currency);
        List<WalletLedgerEntryResponse> entries = walletService.ledger(wallet.getWalletNumber(), PageRequest.of(0, 100)).getData();
        List<String> intents = entries.stream().map(WalletLedgerEntryResponse::sourceCode).filter(StringUtils::hasText).distinct().toList();
        Map<String, CompanyWalletSpend> byIntent = new HashMap<>();
        if (!intents.isEmpty()) {
            spendRepository.findByIntentNumbers(intents).forEach(spend -> byIntent.put(spend.getIntentNumber(), spend));
        }
        Map<String, String> names = new HashMap<>();
        List<CompanyWalletDtos.EntryView> lines = new ArrayList<>();
        for (WalletLedgerEntryResponse entry : entries) {
            CompanyWalletSpend spend = entry.sourceCode() == null ? null : byIntent.get(entry.sourceCode());
            String member = spend != null ? spend.getSpentByMember() : null;
            lines.add(new CompanyWalletDtos.EntryView(entry.transactionNumber(), entry.direction(), entry.entryType(),
                    entry.amount(), entry.balanceAfter(), entry.reference(), member,
                    member == null ? null : name(member, names), spend == null ? null : spend.getDocumentNumber(),
                    entry.createdAt()));
        }
        List<CompanyWalletDtos.SpendView> pending = spendRepository
                .findOfWallet(wallet.getId(), CompanyWalletSpend.Status.PENDING_APPROVAL, PageRequest.of(0, 100))
                .stream().map(spend -> spendView(spend, names)).toList();
        return new CompanyWalletDtos.WalletView(company.getCustomerId(), walletService.get(wallet.getWalletNumber()),
                lines, pending);
    }

    @Transactional(readOnly = true)
    public List<CompanyWalletDtos.SpendView> spends(Customer company, String currency, CompanyWalletSpend.Status status) {
        Optional<WalletAccount> wallet = existingWallet(company.getCustomerId(), currency(currency));
        if (wallet.isEmpty()) {
            return List.of();
        }
        Map<String, String> names = new HashMap<>();
        return spendRepository.findOfWallet(wallet.get().getId(), status, PageRequest.of(0, 200)).stream()
                .map(spend -> spendView(spend, names)).toList();
    }

    /**
     * Recharge saisie par l'administration · especes recues a l'accueil ou virement constate. Passe
     * par les actions d'administration du portefeuille : au-dela du seuil, une seconde personne valide.
     */
    public CompanyWalletDtos.AdminTopUpResult adminTopUp(Customer company, CompanyWalletDtos.AdminTopUpRequest request,
                                                         String actor) {
        WalletAccount wallet = wallet(company, request.currency());
        String reference = request.method().name() + (StringUtils.hasText(request.reference()) ? " " + request.reference().trim() : "");
        WalletAdminAction action = adminActionService.request(wallet.getWalletNumber(), new WalletAdminActionService.ActionRequest(
                WalletAdminAction.Type.TOPUP, request.amount(), reference, request.reason().trim(), null), actor);
        String message = action.getStatus() == WalletAdminAction.Status.PENDING_APPROVAL
                ? "Recharge enregistrée, en attente de la seconde validation de l'administration."
                : "Recharge créditée sur le portefeuille.";
        return new CompanyWalletDtos.AdminTopUpResult(action.getActionNumber(), action.getStatus().name(),
                wallet.getWalletNumber(), message);
    }

    /** Recharge par mobile money depuis le portail du gerant · un recu, pas une facture. */
    public MobileMoneyDepositResponse ownerTopUp(Customer company, Member owner, WalletUsageDtos.TopUpRequest request) {
        WalletAccount wallet = wallet(company, null);
        return topUpService.initiate(wallet, new WalletTopUpService.TopUpOrder(request.amount(), request.phoneNumber(),
                request.correspondent()), owner.getMemberId());
    }

    // ── Autorisations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CompanyWalletDtos.AuthorizationView> authorizations(Customer company) {
        Map<String, String> names = new HashMap<>();
        return authorizationRepository.findAllOfCompany(company.getCustomerId()).stream()
                .map(authorization -> authorizationView(authorization, names)).toList();
    }

    public CompanyWalletDtos.AuthorizationView authorize(Customer company, CompanyWalletDtos.AuthorizationRequest request,
                                                         String actor) {
        Member member = memberRepository.findByMemberIdAndDeletedFalse(request.memberId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : " + request.memberId()));
        if (linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).isEmpty()) {
            throw new BadRequestException(member.getDisplayName() + " n'est pas rattaché à " + company.getCompanyName()
                    + " : rattachez-le avant de l'autoriser à dépenser.");
        }
        periodMustHold(request.validFrom(), request.validUntil());
        WalletAccount wallet = wallet(company, request.currency());
        if (authorizationRepository.findOpen(wallet.getId(), member.getMemberId()).isPresent()) {
            throw new BadRequestException(member.getDisplayName() + " a déjà une autorisation sur ce portefeuille : modifiez-la.");
        }
        SpendingAuthorization authorization = SpendingAuthorization.builder()
                .walletId(wallet.getId())
                .companyCode(company.getCustomerId())
                .memberCode(member.getMemberId())
                .maxPerTransaction(request.maxPerTransaction())
                .maxPerDay(request.maxPerDay())
                .maxPerMonth(request.maxPerMonth())
                .approvalAbove(request.approvalAbove())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .status(SpendingAuthorization.Status.ACTIVE)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        authorization.setOperations(request.operations());
        return authorizationView(authorizationRepository.saveAndFlush(authorization), new HashMap<>());
    }

    public CompanyWalletDtos.AuthorizationView updateAuthorization(Customer company, Long authorizationId,
                                                                   CompanyWalletDtos.AuthorizationUpdateRequest request,
                                                                   String actor) {
        SpendingAuthorization authorization = authorizationRepository.findByShortId(authorizationId)
                .filter(a -> a.getCompanyCode().equalsIgnoreCase(company.getCustomerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Autorisation introuvable : " + authorizationId));
        if (authorization.getStatus() == SpendingAuthorization.Status.REVOKED) {
            throw new BadRequestException("Cette autorisation est révoquée : créez-en une nouvelle.");
        }
        periodMustHold(request.validFrom(), request.validUntil());
        if (request.status() == SpendingAuthorization.Status.ACTIVE
                && authorization.getStatus() != SpendingAuthorization.Status.ACTIVE) {
            Member member = memberRepository.findByMemberIdAndDeletedFalse(authorization.getMemberCode()).orElse(null);
            if (member == null || linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).isEmpty()) {
                throw new BadRequestException("Le collaborateur n'est plus rattaché à l'organisation : l'autorisation ne peut pas reprendre.");
            }
        }
        authorization.setOperations(request.operations());
        authorization.setMaxPerTransaction(request.maxPerTransaction());
        authorization.setMaxPerDay(request.maxPerDay());
        authorization.setMaxPerMonth(request.maxPerMonth());
        authorization.setApprovalAbove(request.approvalAbove());
        authorization.setValidFrom(request.validFrom());
        authorization.setValidUntil(request.validUntil());
        authorization.setUpdatedBy(actor);
        if (request.status() != null && request.status() != authorization.getStatus()) {
            authorization.setStatus(request.status());
            authorization.setStatusReason(StringUtils.hasText(request.reason()) ? request.reason().trim() : null);
        }
        authorizationRepository.save(authorization);
        if (authorization.getStatus() != SpendingAuthorization.Status.ACTIVE) {
            cancelPendingOf(company.getCustomerId(), authorization.getMemberCode(), "Autorisation "
                    + authorization.getStatus().name().toLowerCase(Locale.ROOT), actor);
        }
        return authorizationView(authorization, new HashMap<>());
    }

    /** A la fin d'un rattachement · ses autorisations sont suspendues, ses demandes en attente annulees. */
    public void suspendOnLinkEnd(Customer company, Member member, String reason, String actor) {
        for (SpendingAuthorization authorization : authorizationRepository.findActiveOfMemberInCompany(member.getMemberId(),
                company.getCustomerId())) {
            authorization.setStatus(SpendingAuthorization.Status.SUSPENDED);
            authorization.setStatusReason(reason);
            authorization.setUpdatedBy(actor);
            authorizationRepository.save(authorization);
        }
        cancelPendingOf(company.getCustomerId(), member.getMemberId(), reason, actor);
    }

    // ── Depenses ─────────────────────────────────────────────────────────────

    /**
     * Un collaborateur regle une facture de l'organisation, ou reliee a elle, avec le portefeuille de
     * l'organisation.
     *
     * <p>Refuse sans autorisation en vigueur couvrant l'operation, ou au-dela d'un plafond · le
     * refus dit lequel. Au-dela du seuil de validation, la depense attend le gerant.</p>
     */
    public CompanyWalletDtos.SpendResult payAsCollaborator(Member member, String customerId, String documentNumber) {
        Customer company = linkedCompany(member, customerId);
        BillingDocumentResponse invoice = companyInvoice(company, documentNumber);
        BigDecimal amount = invoice.balanceDue();
        SpendOperation operation = operationOf(invoice);
        WalletAccount wallet = existingWallet(company.getCustomerId(), invoice.currency())
                .orElseThrow(() -> new BadRequestException(company.getCompanyName() + " n'a pas de portefeuille en "
                        + invoice.currency() + "."));
        SpendingAuthorization authorization = authorizationRepository.findOpen(wallet.getId(), member.getMemberId())
                .filter(a -> a.inForceAt(clock.instant()))
                .orElseThrow(() -> new BadRequestException("Vous n'êtes pas autorisé à dépenser sur le portefeuille de "
                        + company.getCompanyName() + "."));
        if (!authorization.operations().contains(operation)) {
            throw new BadRequestException("Votre autorisation ne couvre pas ce type de dépense (" + operation + ").");
        }
        ensureWithinLimits(authorization, wallet, member.getMemberId(), amount);
        ensureSpendable(wallet, amount);
        ensureNoPending(documentNumber);

        CompanyWalletSpend spend = CompanyWalletSpend.builder()
                .walletId(wallet.getId())
                .authorizationId(authorization.getId())
                .companyCode(company.getCustomerId())
                .spentByMember(member.getMemberId())
                .operation(operation)
                .amount(amount)
                .currency(wallet.getCurrency())
                .documentNumber(invoice.documentNumber())
                .createdAt(clock.instant())
                .build();
        if (authorization.getApprovalAbove() != null && amount.compareTo(authorization.getApprovalAbove()) > 0) {
            spend.setStatus(CompanyWalletSpend.Status.PENDING_APPROVAL);
            spend.setExpiresAt(clock.instant().plus(approvalWindow));
            spend.setHoldNumber(hold(wallet, spend, member.getMemberId()));
            spendRepository.saveAndFlush(spend);
            notifyOwners(company, spend, member);
            return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()),
                    "Au-delà de " + plain(authorization.getApprovalAbove()) + " " + spend.getCurrency()
                            + ", la dépense attend la validation du gérant. Les fonds sont réservés ; sans réponse sous "
                            + approvalWindow.toHours() + " heures, elle est annulée.");
        }
        spend.setStatus(CompanyWalletSpend.Status.EXECUTED);
        spendRepository.saveAndFlush(spend);
        execute(spend, wallet, amount, member.getMemberId());
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Facture réglée par le portefeuille de "
                + company.getCompanyName() + ".");
    }

    /** Le gerant regle lui-meme une facture de l'organisation avec son portefeuille · pas d'autorisation, son code PIN. */
    public CompanyWalletDtos.SpendResult payAsOwner(Customer company, Member owner, String documentNumber, String pin) {
        BillingDocumentResponse invoice = companyInvoice(company, documentNumber);
        BigDecimal amount = invoice.balanceDue();
        WalletAccount wallet = existingWallet(company.getCustomerId(), invoice.currency())
                .orElseThrow(() -> new BadRequestException(company.getCompanyName() + " n'a pas de portefeuille en "
                        + invoice.currency() + "."));
        paymentGuard.assertAllowed(wallet, WalletOperationType.BILL_PAYMENT, amount, pin);
        ensureSpendable(wallet, amount);
        ensureNoPending(documentNumber);
        CompanyWalletSpend spend = CompanyWalletSpend.builder()
                .walletId(wallet.getId())
                .companyCode(company.getCustomerId())
                .spentByMember(owner.getMemberId())
                .operation(operationOf(invoice))
                .amount(amount)
                .currency(wallet.getCurrency())
                .documentNumber(invoice.documentNumber())
                .status(CompanyWalletSpend.Status.EXECUTED)
                .createdAt(clock.instant())
                .build();
        spendRepository.saveAndFlush(spend);
        execute(spend, wallet, amount, owner.getMemberId());
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Facture réglée par le portefeuille de "
                + company.getCompanyName() + ".");
    }

    @Transactional(readOnly = true)
    public List<CompanyWalletDtos.SpendView> pendingApprovals(Customer company) {
        return spends(company, null, CompanyWalletSpend.Status.PENDING_APPROVAL);
    }

    /** Le gerant accepte · la retenue est levee et la facture reglee, pour ce qu'elle doit encore. */
    public CompanyWalletDtos.SpendResult approve(Customer company, Member owner, Long spendId) {
        CompanyWalletSpend spend = pendingSpend(company, spendId);
        WalletAccount wallet = walletService.serviceWallet(walletNumberOf(spend));
        releaseHold(spend, owner.getMemberId());
        decide(spend, CompanyWalletSpend.Status.EXECUTED, owner.getMemberId(), null);
        BigDecimal due = billingDocumentService.get(spend.getDocumentNumber()).balanceDue();
        if (due == null || due.signum() <= 0) {
            spend.setStatus(CompanyWalletSpend.Status.CANCELLED);
            spend.setDecisionReason("Facture déjà réglée entre-temps");
            spendRepository.save(spend);
            notifySpender(spend, "La facture " + spend.getDocumentNumber() + " a été réglée entre-temps : rien n'a été prélevé.");
            return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()),
                    "La facture a été réglée entre-temps : rien n'a été prélevé.");
        }
        BigDecimal amount = due.min(spend.getAmount());
        ensureSpendable(wallet, amount);
        spend.setAmount(amount);
        execute(spend, wallet, amount, spend.getSpentByMember());
        notifySpender(spend, "Votre dépense sur la facture " + spend.getDocumentNumber() + " a été acceptée et réglée.");
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Dépense acceptée, facture réglée.");
    }

    public CompanyWalletDtos.SpendResult reject(Customer company, Member owner, Long spendId, String reason) {
        CompanyWalletSpend spend = pendingSpend(company, spendId);
        releaseHold(spend, owner.getMemberId());
        decide(spend, CompanyWalletSpend.Status.REJECTED, owner.getMemberId(), StringUtils.hasText(reason) ? reason.trim() : null);
        notifySpender(spend, "Votre dépense sur la facture " + spend.getDocumentNumber() + " a été refusée"
                + (StringUtils.hasText(reason) ? " : " + reason.trim() : "."));
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Dépense refusée, fonds rendus au portefeuille.");
    }

    /** Le collaborateur retire sa demande tant qu'elle attend. */
    public CompanyWalletDtos.SpendResult cancelOwn(Member member, String customerId, Long spendId) {
        Customer company = linkedCompany(member, customerId);
        CompanyWalletSpend spend = pendingSpend(company, spendId);
        if (!spend.getSpentByMember().equalsIgnoreCase(member.getMemberId())) {
            throw new ResourceNotFoundException("Dépense introuvable : " + spendId);
        }
        releaseHold(spend, member.getMemberId());
        decide(spend, CompanyWalletSpend.Status.CANCELLED, member.getMemberId(), "Retirée par le collaborateur");
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Demande retirée.");
    }

    /** Ce que le collaborateur peut depenser sur les portefeuilles de ses organisations, et ce qu'il y a depense. */
    @Transactional(readOnly = true)
    public List<CompanyWalletDtos.MyCompanyWallet> myWallets(Member member) {
        java.util.Set<String> linked = new java.util.HashSet<>();
        linkRepository.findActiveCompanyCodes(member.getMemberId()).forEach(code -> linked.add(code.toUpperCase()));
        Map<String, String> names = new HashMap<>();
        List<CompanyWalletDtos.MyCompanyWallet> wallets = new ArrayList<>();
        for (SpendingAuthorization authorization : authorizationRepository.findOpenOfMember(member.getMemberId())) {
            if (!linked.contains(authorization.getCompanyCode().toUpperCase())) {
                continue;
            }
            String companyName = customerRepository.findByCustomerId(authorization.getCompanyCode())
                    .map(Customer::getCompanyName).orElse(authorization.getCompanyCode());
            List<CompanyWalletDtos.SpendView> mine = spendRepository.findOfMember(authorization.getWalletId(),
                    member.getMemberId(), PageRequest.of(0, 100)).stream().map(spend -> spendView(spend, names)).toList();
            wallets.add(new CompanyWalletDtos.MyCompanyWallet(authorization.getCompanyCode(), companyName,
                    authorizationView(authorization, names), mine));
        }
        return wallets;
    }

    // ── Expiration ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Long> expiredPendingIds(int limit) {
        return spendRepository.findExpiredPending(clock.instant(), PageRequest.of(0, limit)).stream()
                .map(CompanyWalletSpend::getShortId).toList();
    }

    /** Sans reponse du gerant dans le delai · la demande est annulee et les fonds rendus. */
    public boolean expire(Long spendId) {
        CompanyWalletSpend spend = spendRepository.lockByShortId(spendId).orElse(null);
        if (spend == null || spend.getStatus() != CompanyWalletSpend.Status.PENDING_APPROVAL
                || spend.getExpiresAt() == null || spend.getExpiresAt().isAfter(clock.instant())) {
            return false;
        }
        releaseHold(spend, "SYSTEM");
        decide(spend, CompanyWalletSpend.Status.EXPIRED, "SYSTEM", "Sans réponse du gérant sous "
                + approvalWindow.toHours() + " heures");
        notifySpender(spend, "Votre dépense sur la facture " + spend.getDocumentNumber()
                + " n'a pas été validée à temps : elle est annulée.");
        return true;
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    /**
     * Le reglement · intention au nom de l'organisation, la facture en source, puis paiement par son
     * portefeuille. Le collaborateur est celui qui a depense, sur l'ecriture et dans l'intention.
     */
    private void execute(CompanyWalletSpend spend, WalletAccount wallet, BigDecimal amount, String spentBy) {
        String metadata = "{\"companySpend\":" + spend.getShortId() + ",\"spentByMember\":\"" + spentBy.replace("\"", "") + "\"}";
        PaymentIntentResponse intent = paymentService.createIntent(new CreatePaymentIntentRequest(OWNER_TYPE,
                spend.getCompanyCode(), amount, wallet.getCurrency(), INTENT_PURPOSE, BILLING_DOCUMENT_SOURCE,
                spend.getDocumentNumber(), "COMPANY_SPEND:" + spend.getId(), null, metadata));
        spend.setIntentNumber(intent.intentNumber());
        PaymentTransactionResponse transaction = paymentService.payWithWallet(intent.intentNumber(),
                new com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest(wallet.getWalletNumber(), amount,
                        spentBy, metadata));
        spend.setTransactionNumber(transaction.transactionNumber());
        spend.setStatus(CompanyWalletSpend.Status.EXECUTED);
        spendRepository.save(spend);
        log.info("Depense {} · {} {} sur le portefeuille de {} par {} (facture {})", spend.getShortId(), plain(amount),
                spend.getCurrency(), spend.getCompanyCode(), spentBy, spend.getDocumentNumber());
    }

    /** Retient les fonds dans la transaction de la demande · une demande annulee n'en laisse pas d'orpheline. */
    private String hold(WalletAccount wallet, CompanyWalletSpend spend, String createdBy) {
        String holdNumber = sequenceGenerator.next("wallet_hold");
        ledgerService.placeHold(wallet, spend.getAmount(), HOLD_SOURCE, spend.getDocumentNumber(), holdNumber, createdBy,
                "HOLD:" + holdNumber);
        holdRepository.save(WalletHold.builder()
                .holdNumber(holdNumber)
                .wallet(wallet)
                .amount(spend.getAmount())
                .currency(wallet.getCurrency())
                .status(WalletHoldStatus.ACTIVE)
                .sourceType(HOLD_SOURCE)
                .sourceCode(spend.getDocumentNumber())
                // Une heure apres l'echeance de la demande · l'expiration de la demande la leve avant.
                .expiresAt(spend.getExpiresAt().plus(Duration.ofHours(1)))
                .createdBy(createdBy)
                .build());
        return holdNumber;
    }

    private void releaseHold(CompanyWalletSpend spend, String actor) {
        if (spend.getHoldNumber() == null) {
            return;
        }
        holdRepository.findByHoldNumber(spend.getHoldNumber())
                .filter(hold -> hold.getStatus() == WalletHoldStatus.ACTIVE)
                .ifPresent(hold -> holdService.release(hold.getHoldNumber(), actor));
    }

    private void cancelPendingOf(String companyCode, String memberCode, String reason, String actor) {
        for (CompanyWalletSpend spend : spendRepository.findPendingOfMemberInCompany(companyCode, memberCode)) {
            releaseHold(spend, actor);
            decide(spend, CompanyWalletSpend.Status.CANCELLED, actor, reason);
        }
    }

    private void decide(CompanyWalletSpend spend, CompanyWalletSpend.Status status, String actor, String reason) {
        spend.setStatus(status);
        spend.setDecidedBy(actor);
        spend.setDecidedAt(clock.instant());
        spend.setDecisionReason(reason);
        spendRepository.save(spend);
    }

    /** Plafond par paiement, puis par jour et par mois · le refus dit lequel bloque. */
    void ensureWithinLimits(SpendingAuthorization authorization, WalletAccount wallet, String memberCode, BigDecimal amount) {
        String currency = wallet.getCurrency();
        if (authorization.getMaxPerTransaction() != null && amount.compareTo(authorization.getMaxPerTransaction()) > 0) {
            throw new BadRequestException("Plafond par paiement dépassé : " + plain(amount) + " " + currency
                    + " pour " + plain(authorization.getMaxPerTransaction()) + " " + currency + " au plus.");
        }
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        if (authorization.getMaxPerDay() != null) {
            BigDecimal engaged = spendRepository.sumEngagedSince(wallet.getId(), memberCode, today.atStartOfDay(zone).toInstant());
            if (engaged.add(amount).compareTo(authorization.getMaxPerDay()) > 0) {
                throw new BadRequestException("Plafond du jour dépassé : " + plain(engaged) + " " + currency
                        + " déjà engagés aujourd'hui, " + plain(authorization.getMaxPerDay()) + " " + currency + " au plus.");
            }
        }
        if (authorization.getMaxPerMonth() != null) {
            BigDecimal engaged = spendRepository.sumEngagedSince(wallet.getId(), memberCode,
                    today.withDayOfMonth(1).atStartOfDay(zone).toInstant());
            if (engaged.add(amount).compareTo(authorization.getMaxPerMonth()) > 0) {
                throw new BadRequestException("Plafond du mois dépassé : " + plain(engaged) + " " + currency
                        + " déjà engagés ce mois-ci, " + plain(authorization.getMaxPerMonth()) + " " + currency + " au plus.");
            }
        }
    }

    private void ensureSpendable(WalletAccount wallet, BigDecimal amount) {
        if (!wallet.spendable()) {
            throw new BadRequestException("Le portefeuille de l'organisation est bloqué : aucune dépense n'y passe.");
        }
        if (wallet.getAvailableBalance() == null || wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Solde insuffisant sur le portefeuille de l'organisation : "
                    + plain(wallet.getAvailableBalance()) + " " + wallet.getCurrency() + " disponibles.");
        }
    }

    private void ensureNoPending(String documentNumber) {
        if (spendRepository.existsPendingForDocument(documentNumber)) {
            throw new BadRequestException("Une demande attend déjà la validation du gérant pour cette facture.");
        }
    }

    /** L'operation que la facture represente · lue sur ses lignes. */
    static SpendOperation operationOf(BillingDocumentResponse invoice) {
        if (invoice.lines() != null) {
            for (BillingDocumentLineResponse line : invoice.lines()) {
                String source = line.sourceType() == null ? "" : line.sourceType().toUpperCase(Locale.ROOT);
                if (source.startsWith("BOOKING")) {
                    return SpendOperation.BOOKING;
                }
                if (source.startsWith("PASS")) {
                    return SpendOperation.PASS;
                }
                if (source.startsWith("SUBSCRIPTION") || source.startsWith("USAGE_OVERAGE")) {
                    return SpendOperation.SUBSCRIPTION;
                }
            }
        }
        String documentSource = invoice.sourceType() == null ? "" : invoice.sourceType().toUpperCase(Locale.ROOT);
        if (documentSource.startsWith("BOOKING")) {
            return SpendOperation.BOOKING;
        }
        return SpendOperation.INVOICE_PAYMENT;
    }

    /** La facture, si elle est a l'organisation ou reliee a elle, emise et encore due · 404 sinon. */
    private BillingDocumentResponse companyInvoice(Customer company, String documentNumber) {
        BillingDocumentResponse invoice;
        try {
            invoice = billingDocumentService.get(documentNumber);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Facture introuvable");
        }
        if (!belongsTo(company, invoice) || invoice.status() == BillingDocumentStatus.DRAFT) {
            throw new ResourceNotFoundException("Facture introuvable");
        }
        if (invoice.balanceDue() == null || invoice.balanceDue().signum() <= 0) {
            throw new BadRequestException("Cette facture est déjà réglée.");
        }
        return invoice;
    }

    static boolean belongsTo(Customer company, BillingDocumentResponse document) {
        if (document == null || company.getCustomerId() == null) {
            return false;
        }
        boolean own = OWNER_TYPE.equalsIgnoreCase(document.customerType())
                && company.getCustomerId().equalsIgnoreCase(document.customerCode());
        return own || company.getCustomerId().equalsIgnoreCase(document.companyCustomerCode());
    }

    /** L'organisation a laquelle le membre est rattache · 404 sinon, sans dire qu'elle existe. */
    private Customer linkedCompany(Member member, String customerId) {
        Customer company = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted() && c.getType() == CustomerType.COMPANY)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));
        if (linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Organisation introuvable");
        }
        return company;
    }

    private CompanyWalletSpend pendingSpend(Customer company, Long spendId) {
        CompanyWalletSpend spend = spendRepository.lockByShortId(spendId)
                .filter(s -> s.getCompanyCode().equalsIgnoreCase(company.getCustomerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + spendId));
        if (spend.getStatus() != CompanyWalletSpend.Status.PENDING_APPROVAL) {
            throw new BadRequestException("Cette dépense n'attend plus de validation (" + spend.getStatus() + ").");
        }
        if (spend.getExpiresAt() != null && !spend.getExpiresAt().isAfter(clock.instant())) {
            throw new BadRequestException("Le délai de validation est passé : la dépense est annulée.");
        }
        return spend;
    }

    private Optional<WalletAccount> existingWallet(String companyCode, String currency) {
        return walletRepository.findByOwnerAndCurrency(OWNER_TYPE, companyCode, currency(currency));
    }

    private String walletNumberOf(CompanyWalletSpend spend) {
        return walletRepository.findById(spend.getWalletId()).map(WalletAccount::getWalletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
    }

    private void periodMustHold(Instant from, Instant until) {
        if (from != null && until != null && until.isBefore(from)) {
            throw new BadRequestException("La fin de l'autorisation précède son début.");
        }
    }

    private String currency(String requested) {
        return StringUtils.hasText(requested) ? requested.trim().toUpperCase(Locale.ROOT) : defaultCurrency;
    }

    private void notifyOwners(Customer company, CompanyWalletSpend spend, Member spender) {
        for (CustomerMemberLink link : linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(company, MemberLinkStatus.ACTIVE)) {
            if (link.getRole() != MemberLinkRole.OWNER || link.getMember() == null) {
                continue;
            }
            Member owner = link.getMember();
            inAppNotifier.notify("COMPANY_SPEND_APPROVAL_REQUESTED", "COMPANY_WALLET_SPEND", String.valueOf(spend.getShortId()),
                    owner.getEmail(), owner.getDisplayName(), owner.getMemberId(),
                    spender.getDisplayName() + " demande à régler " + plain(spend.getAmount()) + " " + spend.getCurrency()
                            + " avec le portefeuille de " + company.getCompanyName(),
                    payload(spend));
        }
    }

    private void notifySpender(CompanyWalletSpend spend, String subject) {
        memberRepository.findByMemberIdAndDeletedFalse(spend.getSpentByMember()).ifPresent(member ->
                inAppNotifier.notify("COMPANY_SPEND_DECIDED", "COMPANY_WALLET_SPEND", String.valueOf(spend.getShortId()),
                        member.getEmail(), member.getDisplayName(), member.getMemberId(), subject, payload(spend)));
    }

    private static Map<String, Object> payload(CompanyWalletSpend spend) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("spendId", spend.getShortId());
        payload.put("companyCustomerCode", spend.getCompanyCode());
        payload.put("documentNumber", spend.getDocumentNumber());
        payload.put("amount", plain(spend.getAmount()));
        payload.put("currency", spend.getCurrency());
        payload.put("status", spend.getStatus().name());
        if (spend.getExpiresAt() != null) {
            payload.put("expiresAt", spend.getExpiresAt().toString());
        }
        return payload;
    }

    private CompanyWalletDtos.AuthorizationView authorizationView(SpendingAuthorization authorization, Map<String, String> names) {
        LocalDate today = LocalDate.now(clock);
        ZoneId zone = clock.getZone();
        BigDecimal day = spendRepository.sumEngagedSince(authorization.getWalletId(), authorization.getMemberCode(),
                today.atStartOfDay(zone).toInstant());
        BigDecimal month = spendRepository.sumEngagedSince(authorization.getWalletId(), authorization.getMemberCode(),
                today.withDayOfMonth(1).atStartOfDay(zone).toInstant());
        String walletNumber = walletRepository.findById(authorization.getWalletId()).map(WalletAccount::getWalletNumber).orElse(null);
        return new CompanyWalletDtos.AuthorizationView(authorization.getShortId(), authorization.getCompanyCode(), walletNumber,
                authorization.getMemberCode(), name(authorization.getMemberCode(), names), authorization.operations(),
                authorization.getMaxPerTransaction(), authorization.getMaxPerDay(), authorization.getMaxPerMonth(),
                authorization.getApprovalAbove(), authorization.getValidFrom(), authorization.getValidUntil(),
                authorization.getStatus(), authorization.getStatusReason(), authorization.inForceAt(clock.instant()),
                day, month, authorization.getCreatedBy(), authorization.getCreatedAt());
    }

    private CompanyWalletDtos.SpendView spendView(CompanyWalletSpend spend, Map<String, String> names) {
        return new CompanyWalletDtos.SpendView(spend.getShortId(), spend.getSpentByMember(), name(spend.getSpentByMember(), names),
                spend.getOperation(), spend.getAmount(), spend.getCurrency(), spend.getDocumentNumber(), spend.getStatus(),
                spend.getExpiresAt(), spend.getTransactionNumber(), spend.getDecidedBy(), spend.getDecidedAt(),
                spend.getDecisionReason(), spend.getCreatedAt());
    }

    private String name(String memberCode, Map<String, String> names) {
        if (memberCode == null) {
            return null;
        }
        return names.computeIfAbsent(memberCode.toUpperCase(Locale.ROOT), k -> memberRepository
                .findByMemberIdAndDeletedFalse(memberCode).map(Member::getDisplayName).orElse(memberCode));
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
