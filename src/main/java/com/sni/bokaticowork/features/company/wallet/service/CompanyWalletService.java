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
    private static final String MEMBER_NONE = "NONE";
    static final Duration OTP_VALIDITY = Duration.ofMinutes(10);

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
    private final com.sni.bokaticowork.features.payment.security.service.WalletSecurityService securityService;
    private static final String APPROVAL_HOURS_KEY = "bokati.company.wallet.approval-hours";

    private final CompanySpendCodeGuard codeGuard;
    private final CompanyWalletMailer mailer;
    private final java.security.SecureRandom random = new java.security.SecureRandom();
    private final Clock clock;
    private final String defaultCurrency;
    private final Duration approvalWindow;
    private com.sni.bokaticowork.core.settings.platform.PlatformSettings settings;

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
                                com.sni.bokaticowork.features.payment.security.service.WalletSecurityService securityService,
                                CompanySpendCodeGuard codeGuard, CompanyWalletMailer mailer,
                                @Value("${bokati.company.wallet.currency:XAF}") String defaultCurrency,
                                @Value("${" + APPROVAL_HOURS_KEY + ":48}") long approvalHours) {
        this(walletService, walletRepository, ledgerService, holdRepository, holdService, adminActionService, topUpService,
                paymentGuard, paymentService, billingDocumentService, authorizationRepository, spendRepository,
                customerRepository, memberRepository, linkRepository, sequenceGenerator, inAppNotifier, securityService,
                codeGuard, mailer, Clock.systemDefaultZone(), defaultCurrency, Duration.ofHours(approvalHours));
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
                         com.sni.bokaticowork.features.payment.security.service.WalletSecurityService securityService,
                         CompanySpendCodeGuard codeGuard, CompanyWalletMailer mailer,
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
        this.securityService = securityService;
        this.codeGuard = codeGuard;
        this.mailer = mailer;
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
    public CompanyWalletDtos.SpendResult payAsCollaborator(Member member, String customerId, String documentNumber, String pin) {
        Customer company = linkedCompany(member, customerId);
        BillingDocumentResponse invoice = companyInvoice(company, documentNumber);
        BigDecimal amount = invoice.balanceDue();
        SpendOperation operation = operationOf(invoice);
        WalletAccount wallet = existingWallet(company.getCustomerId(), invoice.currency())
                .orElseThrow(() -> new BadRequestException(company.getCompanyName() + " n'a pas de portefeuille en "
                        + invoice.currency() + "."));
        SpendingAuthorization authorization = authorizationFor(wallet, member, company, operation);
        ensureWithinLimits(authorization, wallet, member.getMemberId(), amount);
        ensureSpendable(wallet, amount);
        ensureNoPending(documentNumber);
        checkCollaboratorPin(authorization, wallet, amount, pin);

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
        if (otpRequired(wallet, amount)) {
            return awaitOtp(spend, member, company);
        }
        return proceed(spend, authorization, wallet, member, company);
    }

    /**
     * Apres l'autorisation, le code et l'eventuel code a usage unique · la validation du gerant
     * au-dela du seuil, sinon le reglement.
     */
    private CompanyWalletDtos.SpendResult proceed(CompanyWalletSpend spend, SpendingAuthorization authorization,
                                                  WalletAccount wallet, Member member, Customer company) {
        if (authorization != null && authorization.getApprovalAbove() != null
                && spend.getAmount().compareTo(authorization.getApprovalAbove()) > 0) {
            spend.setStatus(CompanyWalletSpend.Status.PENDING_APPROVAL);
            spend.setExpiresAt(clock.instant().plus(approvalWindow()));
            spend.setHoldNumber(hold(wallet, spend, member.getMemberId()));
            spendRepository.saveAndFlush(spend);
            notifyOwners(company, spend, member);
            return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()),
                    "Au-delà de " + plain(authorization.getApprovalAbove()) + " " + spend.getCurrency()
                            + ", la dépense attend la validation du gérant. Les fonds sont réservés ; sans réponse sous "
                            + approvalWindow().toHours() + " heures, elle est annulée.");
        }
        spend.setStatus(CompanyWalletSpend.Status.EXECUTED);
        spendRepository.saveAndFlush(spend);
        execute(spend, wallet, spend.getAmount(), member.getMemberId());
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()), "Facture réglée par le portefeuille de "
                + company.getCompanyName() + ".");
    }

    /** Le gerant regle lui-meme une facture de l'organisation avec son portefeuille · pas d'autorisation, son code. */
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
                .createdAt(clock.instant())
                .build();
        if (otpRequired(wallet, amount)) {
            return awaitOtp(spend, owner, company);
        }
        return proceed(spend, null, wallet, owner, company);
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setSettings(com.sni.bokaticowork.core.settings.platform.PlatformSettings settings) {
        this.settings = settings;
    }

    /** Le delai en vigueur · un changement vaut pour les depenses suivantes, pas pour celles en attente. */
    private Duration approvalWindow() {
        return settings == null ? approvalWindow
                : Duration.ofHours(settings.number(APPROVAL_HOURS_KEY, approvalWindow.toHours()));
    }

    /** Le collaborateur saisit le code a usage unique recu par e-mail. */
    public CompanyWalletDtos.SpendResult confirmAsCollaborator(Member member, String customerId, Long spendId, String code) {
        Customer company = linkedCompany(member, customerId);
        CompanyWalletSpend spend = awaitingOtp(company, member, spendId, code);
        BillingDocumentResponse invoice = companyInvoice(company, spend.getDocumentNumber());
        WalletAccount wallet = walletService.serviceWallet(walletNumberOf(spend));
        SpendingAuthorization authorization = authorizationFor(wallet, member, company, spend.getOperation());
        spend.setAmount(invoice.balanceDue().min(spend.getAmount()));
        ensureWithinLimits(authorization, wallet, member.getMemberId(), spend.getAmount());
        ensureSpendable(wallet, spend.getAmount());
        ensureNoPending(spend.getDocumentNumber());
        return proceed(spend, authorization, wallet, member, company);
    }

    /** Le gerant saisit le code a usage unique recu par e-mail. */
    public CompanyWalletDtos.SpendResult confirmAsOwner(Customer company, Member owner, Long spendId, String code) {
        CompanyWalletSpend spend = awaitingOtp(company, owner, spendId, code);
        BillingDocumentResponse invoice = companyInvoice(company, spend.getDocumentNumber());
        WalletAccount wallet = walletService.serviceWallet(walletNumberOf(spend));
        spend.setAmount(invoice.balanceDue().min(spend.getAmount()));
        ensureSpendable(wallet, spend.getAmount());
        ensureNoPending(spend.getDocumentNumber());
        return proceed(spend, null, wallet, owner, company);
    }

    /** Le code du collaborateur, quand la regle du portefeuille en exige un pour regler une facture. */
    private void checkCollaboratorPin(SpendingAuthorization authorization, WalletAccount wallet, BigDecimal amount, String pin) {
        if (!securityService.evaluate(wallet, WalletOperationType.BILL_PAYMENT, amount).pinRequired()) {
            return;
        }
        var policy = securityService.policyFor(wallet);
        int maxAttempts = policy.getMaxFailedAttempts() == null ? 5 : policy.getMaxFailedAttempts();
        long lockout = policy.getLockoutMinutes() == null ? 15 : policy.getLockoutMinutes();
        CompanySpendCodeGuard.Check check = codeGuard.verifyPin(authorization.getId(), pin, maxAttempts, lockout, clock.instant());
        switch (check.verdict()) {
            case OK -> { }
            case NO_CODE -> throw new BadRequestException("Définissez d'abord votre code de dépense pour ce portefeuille.");
            case LOCKED -> throw new BadRequestException("Code de dépense bloqué après trop d'essais, jusqu'à "
                    + check.lockedUntil() + ".");
            default -> throw new BadRequestException("Code de dépense incorrect : " + check.remaining() + " essai(s) restant(s).");
        }
    }

    /** Au-dela du seuil de la regle du portefeuille, un code a usage unique confirme la depense. */
    private boolean otpRequired(WalletAccount wallet, BigDecimal amount) {
        return securityService.evaluate(wallet, WalletOperationType.BILL_PAYMENT, amount).otpRequired();
    }

    private CompanyWalletDtos.SpendResult awaitOtp(CompanyWalletSpend spend, Member payer, Customer company) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        spend.setStatus(CompanyWalletSpend.Status.AWAITING_OTP);
        spend.setOtpHash(codeGuard.hash(code));
        spend.setOtpExpiresAt(clock.instant().plus(OTP_VALIDITY));
        spend.setOtpAttempts(0);
        spendRepository.saveAndFlush(spend);
        mailer.oneTimeCode(payer, code, "le règlement de " + plain(spend.getAmount()) + " " + spend.getCurrency()
                + " avec le portefeuille de " + company.getCompanyName(), OTP_VALIDITY.toMinutes(),
                spend.getDocumentNumber());
        return new CompanyWalletDtos.SpendResult(spendView(spend, new HashMap<>()),
                "Un code de confirmation vient de vous être envoyé par e-mail. Saisissez-le sous "
                        + OTP_VALIDITY.toMinutes() + " minutes pour confirmer la dépense.");
    }

    private CompanyWalletSpend awaitingOtp(Customer company, Member payer, Long spendId, String code) {
        CompanyWalletSpend found = spendRepository.findByShortId(spendId)
                .filter(s -> s.getCompanyCode().equalsIgnoreCase(company.getCustomerId())
                        && s.getSpentByMember().equalsIgnoreCase(payer.getMemberId()))
                .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + spendId));
        CompanySpendCodeGuard.Check check = codeGuard.verifyOtp(found.getShortId(), code, clock.instant());
        if (check.verdict() == CompanySpendCodeGuard.Verdict.EXPIRED) {
            throw new BadRequestException("Ce code n'est plus valable : recommencez le règlement.");
        }
        if (check.verdict() != CompanySpendCodeGuard.Verdict.OK) {
            throw new BadRequestException("Code de confirmation incorrect : " + check.remaining() + " essai(s) restant(s).");
        }
        return spendRepository.lockByShortId(spendId).orElseThrow();
    }

    /** L'autorisation en vigueur du collaborateur, couvrant l'operation · sinon le refus dit pourquoi. */
    private SpendingAuthorization authorizationFor(WalletAccount wallet, Member member, Customer company, SpendOperation operation) {
        SpendingAuthorization authorization = authorizationRepository.findOpen(wallet.getId(), member.getMemberId())
                .filter(a -> a.inForceAt(clock.instant()))
                .orElseThrow(() -> new BadRequestException("Vous n'êtes pas autorisé à dépenser sur le portefeuille de "
                        + company.getCompanyName() + "."));
        if (!authorization.operations().contains(operation)) {
            throw new BadRequestException("Votre autorisation ne couvre pas ce type de dépense (" + operation + ").");
        }
        return authorization;
    }

    // ── Code de depense du collaborateur ─────────────────────────────────────

    /** Le collaborateur pose ou change son code de depense · l'ancien est exige s'il en a un. */
    public void setCollaboratorPin(Member member, String customerId, String currentPin, String newPin) {
        Customer company = linkedCompany(member, customerId);
        WalletAccount wallet = existingWallet(company.getCustomerId(), null)
                .orElseThrow(() -> new BadRequestException(company.getCompanyName() + " n'a pas de portefeuille."));
        SpendingAuthorization authorization = authorizationRepository.findOpen(wallet.getId(), member.getMemberId())
                .orElseThrow(() -> new BadRequestException("Vous n'avez pas d'autorisation sur ce portefeuille."));
        var policy = securityService.policyFor(wallet);
        if (StringUtils.hasText(authorization.getPinHash())) {
            CompanySpendCodeGuard.Check check = codeGuard.verifyPin(authorization.getId(), currentPin,
                    policy.getMaxFailedAttempts() == null ? 5 : policy.getMaxFailedAttempts(),
                    policy.getLockoutMinutes() == null ? 15 : policy.getLockoutMinutes(), clock.instant());
            if (check.verdict() != CompanySpendCodeGuard.Verdict.OK) {
                throw new BadRequestException(check.verdict() == CompanySpendCodeGuard.Verdict.LOCKED
                        ? "Code de dépense bloqué après trop d'essais." : "Code actuel incorrect.");
            }
            authorization = authorizationRepository.findById(authorization.getId()).orElseThrow();
        }
        assertAcceptablePin(newPin, policy.getPinLength() == null ? 4 : policy.getPinLength());
        authorization.setPinHash(codeGuard.hash(newPin));
        authorization.setPinSetAt(clock.instant());
        authorization.setPinFailedAttempts(0);
        authorization.setPinLockedUntil(null);
        authorizationRepository.save(authorization);
    }

    /** Le gerant efface le code d'un collaborateur · celui-ci en pose un nouveau sans l'ancien. */
    public CompanyWalletDtos.AuthorizationView resetCollaboratorPin(Customer company, Long authorizationId, String actor) {
        SpendingAuthorization authorization = authorizationRepository.findByShortId(authorizationId)
                .filter(a -> a.getCompanyCode().equalsIgnoreCase(company.getCustomerId()))
                .orElseThrow(() -> new ResourceNotFoundException("Autorisation introuvable : " + authorizationId));
        authorization.setPinHash(null);
        authorization.setPinSetAt(null);
        authorization.setPinFailedAttempts(0);
        authorization.setPinLockedUntil(null);
        authorization.setUpdatedBy(actor);
        authorizationRepository.save(authorization);
        return authorizationView(authorization, new HashMap<>());
    }

    /** Ni une repetition, ni une suite · ce sont les premiers codes essayes. */
    static void assertAcceptablePin(String pin, int length) {
        if (!StringUtils.hasText(pin) || !pin.matches("\\d{" + length + "}")) {
            throw new BadRequestException("Le code de dépense doit comporter " + length + " chiffres.");
        }
        if (pin.chars().distinct().count() == 1) {
            throw new BadRequestException("Un code composé du même chiffre est trop facile à deviner.");
        }
        boolean ascending = true;
        boolean descending = true;
        for (int index = 1; index < pin.length(); index++) {
            ascending &= pin.charAt(index) - pin.charAt(index - 1) == 1;
            descending &= pin.charAt(index - 1) - pin.charAt(index) == 1;
        }
        if (ascending || descending) {
            throw new BadRequestException("Un code formé d'une suite est trop facile à deviner.");
        }
    }

    // ── Reservations prepayees ───────────────────────────────────────────────

    /** Ce qui permet de retenir une reservation sur le portefeuille de l'organisation. */
    public record BookingFunding(WalletAccount wallet, Long authorizationId, String companyCode, String memberCode) {
    }

    /**
     * Le portefeuille de l'organisation paie-t-il cette reservation prepayee · oui si le membre y a
     * une autorisation en vigueur couvrant les reservations. Vide sinon : son portefeuille personnel
     * paie, comme avant.
     *
     * <p>Les plafonds s'appliquent. Au-dela du seuil de validation, le prepaiement est refuse : une
     * reservation ne peut pas attendre la reponse du gerant pour etre confirmee.</p>
     */
    @Transactional(readOnly = true)
    public Optional<BookingFunding> bookingFunding(String memberCode, String companyCode, String currency, BigDecimal amount) {
        if (!StringUtils.hasText(memberCode) || !StringUtils.hasText(companyCode)
                || MEMBER_NONE.equalsIgnoreCase(companyCode.trim())) {
            return Optional.empty();
        }
        Optional<WalletAccount> found = existingWallet(companyCode.trim(), currency);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        WalletAccount wallet = found.get();
        Optional<SpendingAuthorization> authorization = authorizationRepository.findOpen(wallet.getId(), memberCode)
                .filter(a -> a.inForceAt(clock.instant()) && a.operations().contains(SpendOperation.BOOKING));
        if (authorization.isEmpty()) {
            return Optional.empty();
        }
        ensureWithinLimits(authorization.get(), wallet, memberCode, amount);
        if (authorization.get().getApprovalAbove() != null && amount.compareTo(authorization.get().getApprovalAbove()) > 0) {
            throw new BadRequestException("Au-delà de " + plain(authorization.get().getApprovalAbove()) + " "
                    + wallet.getCurrency() + ", une dépense sur le portefeuille de l'organisation attend la validation du"
                    + " gérant : réservez en paiement direct, puis réglez la facture avec le portefeuille de l'organisation.");
        }
        ensureSpendable(wallet, amount);
        return Optional.of(new BookingFunding(wallet, authorization.get().getId(), wallet.getOwnerCode(), memberCode));
    }

    /** Note la retenue posee pour la reservation · elle compte dans les plafonds jusqu'au reglement. */
    public void recordBookingHold(BookingFunding funding, String bookingNumber, BigDecimal amount, String holdNumber) {
        spendRepository.save(CompanyWalletSpend.builder()
                .walletId(funding.wallet().getId())
                .authorizationId(funding.authorizationId())
                .companyCode(funding.companyCode())
                .spentByMember(funding.memberCode())
                .operation(SpendOperation.BOOKING)
                .amount(amount)
                .currency(funding.wallet().getCurrency())
                .bookingNumber(bookingNumber)
                .holdNumber(holdNumber)
                .status(CompanyWalletSpend.Status.HELD)
                .createdAt(clock.instant())
                .build());
    }

    /** La retenue est-elle celle d'une reservation prepayee sur le portefeuille d'une organisation. */
    @Transactional(readOnly = true)
    public boolean isCompanyBookingHold(String holdNumber) {
        return spendRepository.findHeldByHold(holdNumber).isPresent();
    }

    /**
     * Regle la facture d'une reservation prepayee avec le portefeuille de l'organisation · la retenue
     * a deja ete levee par l'appelant, dans la meme transaction.
     */
    public void settleBookingHold(String holdNumber, String documentNumber, BigDecimal due) {
        CompanyWalletSpend spend = spendRepository.findHeldByHold(holdNumber).orElseThrow();
        WalletAccount wallet = walletService.serviceWallet(walletNumberOf(spend));
        spend.setDocumentNumber(documentNumber);
        spend.setAmount(due.min(spend.getAmount()));
        spend.setStatus(CompanyWalletSpend.Status.EXECUTED);
        spendRepository.saveAndFlush(spend);
        execute(spend, wallet, spend.getAmount(), spend.getSpentByMember());
    }

    /** La reservation annulee, ou sans rien a payer · la depense retenue est annulee. */
    public void cancelBookingHold(String bookingNumber, String reason) {
        for (CompanyWalletSpend spend : spendRepository.findHeldByBooking(bookingNumber)) {
            decide(spend, CompanyWalletSpend.Status.CANCELLED, "SYSTEM", reason);
        }
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
        List<Long> ids = new ArrayList<>(spendRepository.findExpiredPending(clock.instant(), PageRequest.of(0, limit)).stream()
                .map(CompanyWalletSpend::getShortId).toList());
        spendRepository.findExpiredOtp(clock.instant(), PageRequest.of(0, limit)).forEach(spend -> ids.add(spend.getShortId()));
        return ids;
    }

    /** Sans reponse du gerant dans le delai · la demande est annulee et les fonds rendus. */
    public boolean expire(Long spendId) {
        CompanyWalletSpend spend = spendRepository.lockByShortId(spendId).orElse(null);
        if (spend != null && spend.getStatus() == CompanyWalletSpend.Status.AWAITING_OTP) {
            if (spend.getOtpExpiresAt() == null || spend.getOtpExpiresAt().isAfter(clock.instant())) {
                return false;
            }
            spend.setOtpHash(null);
            decide(spend, CompanyWalletSpend.Status.EXPIRED, "SYSTEM", "Code de confirmation non saisi à temps");
            return true;
        }
        if (spend == null || spend.getStatus() != CompanyWalletSpend.Status.PENDING_APPROVAL
                || spend.getExpiresAt() == null || spend.getExpiresAt().isAfter(clock.instant())) {
            return false;
        }
        releaseHold(spend, "SYSTEM");
        decide(spend, CompanyWalletSpend.Status.EXPIRED, "SYSTEM", "Sans réponse du gérant sous "
                + approvalWindow().toHours() + " heures");
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
        String metadata = com.sni.bokaticowork.core.utils.json.JsonPayload.of("companySpend", spend.getShortId(), "spentByMember", spentBy);
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
                if (source.startsWith("MARKETPLACE")) {
                    return SpendOperation.MARKETPLACE;
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
        String message = spender.getDisplayName() + " demande à régler la facture " + spend.getDocumentNumber() + " ("
                + plain(spend.getAmount()) + " " + spend.getCurrency() + ") avec le portefeuille de " + company.getCompanyName()
                + ". Les fonds sont réservés. Acceptez ou refusez la dépense depuis votre portail ; sans réponse sous "
                + approvalWindow().toHours() + " heures, elle est annulée.";
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
            mailer.notify(owner, "Dépense à valider · " + plain(spend.getAmount()) + " " + spend.getCurrency(), message,
                    spend.getDocumentNumber(), "/companies/" + company.getCustomerId() + "/approvals");
        }
    }

    private void notifySpender(CompanyWalletSpend spend, String subject) {
        memberRepository.findByMemberIdAndDeletedFalse(spend.getSpentByMember()).ifPresent(member -> {
            inAppNotifier.notify("COMPANY_SPEND_DECIDED", "COMPANY_WALLET_SPEND", String.valueOf(spend.getShortId()),
                    member.getEmail(), member.getDisplayName(), member.getMemberId(), subject, payload(spend));
            mailer.notify(member, "Votre dépense sur le portefeuille de l'organisation", subject,
                    spend.getDocumentNumber(), "/company-wallets");
        });
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
                StringUtils.hasText(authorization.getPinHash()), day, month, authorization.getCreatedBy(), authorization.getCreatedAt());
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
