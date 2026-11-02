package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.utils.json.JsonPayload;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Le depot de garantie d'un abonnement a son annulation · rendu au portefeuille, puis employe a
 * regler les factures impayees de l'abonne, de la plus ancienne a la plus recente.
 *
 * <p>Le depot etait « encaisse pour couvrir les impayes » : la retenue devenait un debit du
 * portefeuille, mais aucune facture n'etait reglee. L'argent sortait du portefeuille et les
 * factures restaient dues. Ici chaque montant preleve paie une facture, par le circuit de paiement
 * habituel (intention sur la facture, reglement par portefeuille, recu) ; ce qui reste du depot
 * redevient disponible.</p>
 *
 * <p>Dans sa propre transaction : un echec laisse le depot retenu, a traiter par l'administration,
 * sans faire echouer l'annulation.</p>
 */
@Slf4j
@Component
public class SubscriptionDepositSettlement {

    static final String HOLD_SOURCE = "SUBSCRIPTION";

    /** Une facture reglee par le depot. */
    public record Paid(String documentNumber, BigDecimal amount, String transactionNumber) {
    }

    /** Ce que le depot est devenu · regle sur les factures, et rendu. */
    public record Outcome(String holdNumber, BigDecimal deposit, List<Paid> paid, BigDecimal returned) {

        public String toJson() {
            List<Object> invoices = new ArrayList<>();
            for (Paid p : paid) {
                invoices.add(java.util.Map.of("documentNumber", p.documentNumber(), "amount", p.amount(),
                        "transactionNumber", p.transactionNumber() == null ? "" : p.transactionNumber()));
            }
            return JsonPayload.of("depositHold", holdNumber, "deposit", deposit, "paidInvoices", invoices,
                    "returnedToWallet", returned);
        }
    }

    private final WalletHoldRepository holdRepository;
    private final WalletHoldService holdService;
    private final BillingDocumentRepository documentRepository;
    private final PaymentService paymentService;

    public SubscriptionDepositSettlement(WalletHoldRepository holdRepository, WalletHoldService holdService,
                                         BillingDocumentRepository documentRepository, @Lazy PaymentService paymentService) {
        this.holdRepository = holdRepository;
        this.holdService = holdService;
        this.documentRepository = documentRepository;
        this.paymentService = paymentService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Outcome> settle(String subscriptionNumber, String subscriberType, String subscriberCode) {
        List<Outcome> outcomes = new ArrayList<>();
        for (WalletHold hold : holdRepository.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", HOLD_SOURCE, subscriptionNumber)) {
            holdService.release(hold.getHoldNumber(), "SYSTEM");
            BigDecimal remaining = hold.getAmount();
            List<Paid> paid = new ArrayList<>();
            String walletNumber = hold.getWallet().getWalletNumber();
            for (BillingDocument document : documentRepository.findRecoverableDocuments(subscriberType, subscriberCode)) {
                if (remaining.signum() <= 0) {
                    break;
                }
                if (document.getCurrency() != null && !document.getCurrency().equalsIgnoreCase(hold.getCurrency())) {
                    continue;
                }
                BigDecimal amount = document.getBalanceDue().min(remaining);
                PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(new CreatePaymentIntentFromBillingDocumentRequest(
                        document.getDocumentNumber(), amount,
                        "DEPOSIT:" + hold.getHoldNumber() + ":" + document.getDocumentNumber(), null,
                        JsonPayload.of("depositHold", hold.getHoldNumber(), "subscriptionNumber", subscriptionNumber)));
                var transaction = paymentService.payWithWallet(intent.intentNumber(),
                        new WalletPaymentRequest(walletNumber, amount, "SYSTEM",
                                JsonPayload.of("depositHold", hold.getHoldNumber(), "cause", "SUBSCRIPTION_CANCELLED")));
                paid.add(new Paid(document.getDocumentNumber(), amount, transaction == null ? null : transaction.transactionNumber()));
                remaining = remaining.subtract(amount);
            }
            log.info("Abonnement {} · depot {} de {} : {} facture(s) reglee(s), {} rendu(s) au portefeuille",
                    subscriptionNumber, hold.getHoldNumber(), hold.getAmount().toPlainString(), paid.size(), remaining.toPlainString());
            outcomes.add(new Outcome(hold.getHoldNumber(), hold.getAmount(), paid, remaining));
        }
        return outcomes;
    }
}
