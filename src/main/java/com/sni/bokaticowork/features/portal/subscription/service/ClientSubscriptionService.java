package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.security.service.WalletMerchantPaymentGuard;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;

import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientCancelSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PassSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientSubscriptionService {

    private static final SubscriberType MEMBER = SubscriberType.MEMBER;

    private final SubscriptionService subscriptionService;
    private final PassService passService;
    private final EntitlementService entitlementService;
    private final BillingDocumentService billingDocumentService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final WalletMerchantPaymentGuard merchantPaymentGuard;


    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionResponse> listSubscriptions(Member member,
                                                                      SubscriptionStatus status,
                                                                      Pageable pageable) {
        SubscriptionSearchCriteria criteria = SubscriptionSearchCriteria.builder()
                .subscriberType(MEMBER)
                .subscriberCode(member.getMemberId())
                .status(status)
                .build();
        return subscriptionService.list(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(Member member) {
        return subscriptionService.current(MEMBER, member.getMemberId());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Member member, String subscriptionNumber) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        return sub;
    }

    @Transactional(readOnly = true)
    public List<EntitlementGrantResponse> getSubscriptionEntitlements(Member member,
                                                                       String subscriptionNumber) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        return subscriptionService.listEntitlements(subscriptionNumber);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionHistoryResponse> getSubscriptionHistory(Member member,
                                                                     String subscriptionNumber) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        return subscriptionService.history(subscriptionNumber);
    }

    @Transactional(readOnly = true)
    public BillingScheduleResponse getBillingSchedule(Member member, String subscriptionNumber) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        return subscriptionService.billingSchedule(subscriptionNumber);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Member member, String subscriptionNumber,
                                                    ClientCancelSubscriptionRequest request) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        String reason = request != null ? request.getReason() : null;
        SubscriptionStatusChangeRequest change = new SubscriptionStatusChangeRequest(
                reason, member.getMemberId(), true
        );
        return subscriptionService.cancel(subscriptionNumber, change);
    }


    @Transactional(readOnly = true)
    public PaginatedResponse<PassResponse> listPasses(Member member, PassType passType,
                                                       PassStatus status, Pageable pageable) {
        PassSearchCriteria criteria = PassSearchCriteria.builder()
                .ownerType(MEMBER)
                .ownerCode(member.getMemberId())
                .passType(passType)
                .status(status)
                .build();
        return passService.list(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public PassResponse getPass(Member member, String passNumber) {
        PassResponse pass = passService.get(passNumber);
        verifyPassOwnership(member, pass);
        return pass;
    }


    @Transactional(readOnly = true)
    public List<EntitlementGrantResponse> getEntitlementBalances(Member member) {
        return entitlementService.balances(MEMBER, member.getMemberId());
    }


    // ── Subscription payment ─────────────────────────────────────────────

    @Transactional
    public MobileMoneyDepositResponse paySubscriptionWithMobileMoney(Member member,
                                                                      String subscriptionNumber,
                                                                      String phoneNumber,
                                                                      CongoCorrespondent correspondent) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        BillingDocumentResponse invoice = findPayableInvoice("SUBSCRIPTION", subscriptionNumber, sub.currency());
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(invoice.documentNumber(), null, null, null, null)
        );
        return paymentService.initiateMobileMoneyDeposit(
                intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(
                        intent.intentNumber(), phoneNumber, correspondent, null, member.getMemberId(), null
                )
        );
    }

    @Transactional
    public PaymentTransactionResponse paySubscriptionWithWallet(Member member, String subscriptionNumber, String pin) {
        SubscriptionResponse sub = subscriptionService.get(subscriptionNumber);
        verifySubscriptionOwnership(member, sub);
        BillingDocumentResponse invoice = findPayableInvoice("SUBSCRIPTION", subscriptionNumber, sub.currency());
        WalletResponse wallet = walletService.getOrCreate(MEMBER.name(), member.getMemberId(), sub.currency());
        merchantPaymentGuard.assertAllowed(walletService.serviceWallet(wallet.walletNumber()),
                WalletOperationType.BILL_PAYMENT, invoice.balanceDue(), pin);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(invoice.documentNumber(), null, null, null, null)
        );
        return paymentService.payWithWallet(
                intent.intentNumber(),
                new WalletPaymentRequest(wallet.walletNumber(), null, member.getMemberId(), null)
        );
    }

    // ── Pass payment ────────────────────────────────────────────────────

    @Transactional
    public MobileMoneyDepositResponse payPassWithMobileMoney(Member member,
                                                               String passNumber,
                                                               String phoneNumber,
                                                               CongoCorrespondent correspondent) {
        PassResponse pass = passService.get(passNumber);
        verifyPassOwnership(member, pass);
        BillingDocumentResponse invoice = findPayableInvoice("PASS", passNumber, null);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(invoice.documentNumber(), null, null, null, null)
        );
        return paymentService.initiateMobileMoneyDeposit(
                intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(
                        intent.intentNumber(), phoneNumber, correspondent, null, member.getMemberId(), null
                )
        );
    }

    @Transactional
    public PaymentTransactionResponse payPassWithWallet(Member member, String passNumber, String pin) {
        PassResponse pass = passService.get(passNumber);
        verifyPassOwnership(member, pass);
        BillingDocumentResponse invoice = findPayableInvoice("PASS", passNumber, null);
        String currency = invoice.currency();
        WalletResponse wallet = walletService.getOrCreate(MEMBER.name(), member.getMemberId(), currency);
        merchantPaymentGuard.assertAllowed(walletService.serviceWallet(wallet.walletNumber()),
                WalletOperationType.BILL_PAYMENT, invoice.balanceDue(), pin);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(invoice.documentNumber(), null, null, null, null)
        );
        return paymentService.payWithWallet(
                intent.intentNumber(),
                new WalletPaymentRequest(wallet.walletNumber(), null, member.getMemberId(), null)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Nombre de factures examinees avant de renoncer. Un abonnement en produit plusieurs des sa
     * creation · le loyer, les frais d'entree, la caution. Sur plusieurs periodes, elles
     * s'accumulent.
     */
    private static final int INVOICE_LOOKUP_PAGE = 100;

    /**
     * Premiere facture restant a payer, la plus ancienne d'abord.
     *
     * <p>Deux defauts se combinaient ici, et le second masquait le premier.</p>
     *
     * <p>La recherche ne demandait <b>qu'une seule</b> facture, puis filtrait en memoire celles dont
     * le solde est non nul. Comme la requete rend les plus recentes d'abord, il suffisait que la
     * derniere emise soit deja reglee pour que le filtre vide la liste et que l'appel reponde
     * « aucune facture a payer », alors qu'une facture impayee attendait juste derriere. C'est
     * exactement ce qui arrive a un abonnement, qui emet des sa creation une facture de loyer, une
     * de frais d'entree et une de caution : regler la derniere rendait les autres inaccessibles.</p>
     *
     * <p>Et l'ordre etait le mauvais. On solde une dette en commencant par la plus ancienne · payer
     * la plus recente en premier laisse vieillir celle qui deviendra exigible la premiere.</p>
     */
    private BillingDocumentResponse findPayableInvoice(String sourceType, String sourceCode, String currency) {
        List<BillingDocumentResponse> payable = billingDocumentService.list(
                        BillingDocumentType.INVOICE, null, null, null,
                        sourceType, sourceCode, null, null, null,
                        Pageable.ofSize(INVOICE_LOOKUP_PAGE))
                .getData().stream()
                .filter(this::isPayable)
                .filter(document -> matchesCurrency(document, currency))
                .sorted(Comparator.comparing(BillingDocumentResponse::issueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (payable.isEmpty()) {
            throw new ResourceNotFoundException("Aucune facture à payer pour " + sourceCode);
        }
        return payable.getFirst();
    }

    private boolean isPayable(BillingDocumentResponse document) {
        return document.balanceDue() != null
                && document.balanceDue().compareTo(BigDecimal.ZERO) > 0
                && document.status() != BillingDocumentStatus.CANCELLED
                && document.status() != BillingDocumentStatus.DRAFT;
    }

    /**
     * Une facture dans une autre devise que celle attendue n'est pas payable ici · le portefeuille
     * est mono-devise, et une conversion silencieuse est une perte pour quelqu'un.
     */
    private boolean matchesCurrency(BillingDocumentResponse document, String currency) {
        return currency == null
                || document.currency() == null
                || currency.equalsIgnoreCase(document.currency());
    }

    private void verifySubscriptionOwnership(Member member, SubscriptionResponse sub) {
        if (sub.subscriberType() != MEMBER
                || !member.getMemberId().equals(sub.subscriberCode())) {
            throw new ResourceNotFoundException("Subscription not found");
        }
    }

    private void verifyPassOwnership(Member member, PassResponse pass) {
        if (pass.ownerType() != MEMBER
                || !member.getMemberId().equals(pass.ownerCode())) {
            throw new ResourceNotFoundException("Pass not found");
        }
    }
}
