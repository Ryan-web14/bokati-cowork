package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingRecipientPreference;
import com.sni.bokaticowork.features.billing.repository.BillingRecipientPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * WhatsApp {@code facture_payee} quand un paiement solde une facture, quel que soit le moyen de
 * paiement, en plus du courriel qui envoie la facture payee. Un paiement partiel n'envoie rien : aucun modele approuve ne le decrit.
 *
 * <p>Numero : le premier numero WhatsApp des preferences d'envoi du client, sinon le numero du
 * client ou du membre, sinon celui de la facture. Un membre qui a coupe la confirmation de paiement
 * sur WhatsApp ne recoit rien.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWhatsAppNotifier {

    static final String PURPOSE = "INVOICE_PAID";
    static final String PORTAL_PATH = "factures";

    private final WhatsAppNotifier whatsApp;
    private final MemberBillingContact memberContact;
    private final BillingRecipientPreferenceRepository preferences;

    /**
     * @param document facture apres le paiement
     * @param paid     montant de ce paiement sur cette facture ; vide, le montant deja paye de la facture
     */
    public void notify(BillingDocumentResponse document, BigDecimal paid) {
        if (document == null || !settled(document) || !whatsApp.active()) {
            return;
        }
        if (!memberContact.allowsWhatsApp(document.customerType(), document.customerCode(), EventType.PAYMENT_RECEIVED)) {
            log.info("WhatsApp de reglement de {} non envoye : preference du client {}", document.documentNumber(), document.customerCode());
            return;
        }
        phone(document).ifPresent(phone -> whatsApp.notify(phone, PURPOSE, values(document, paid), document.documentNumber(), PORTAL_PATH));
    }

    /** Facture (ou facture de solde) en XAF dont il ne reste rien a payer. */
    static boolean settled(BillingDocumentResponse document) {
        if (document.documentType() == BillingDocumentType.QUOTE || document.documentType() == BillingDocumentType.PROFORMA_INVOICE
                || document.documentType() == BillingDocumentType.CREDIT_NOTE) {
            return false;
        }
        if (StringUtils.hasText(document.currency()) && !"XAF".equalsIgnoreCase(document.currency().trim())) {
            return false;
        }
        return document.balanceDue() == null || document.balanceDue().signum() <= 0;
    }

    /** Prenom, montant de ce paiement, numero de la facture. */
    static List<String> values(BillingDocumentResponse document, BigDecimal paid) {
        String name = StringUtils.hasText(document.customerName()) ? document.customerName().trim().split("\\s+")[0] : "client";
        return List.of(name, amount(paid != null && paid.signum() > 0 ? paid : document.paidAmount()), document.documentNumber());
    }

    private Optional<String> phone(BillingDocumentResponse document) {
        Optional<String> preferred = Optional.empty();
        try {
            preferred = preferences.findByCustomerTypeAndCustomerCode(document.customerType(), document.customerCode())
                    .map(BillingRecipientPreference::getWhatsappNumbers)
                    .flatMap(numbers -> Arrays.stream(numbers.split("[,;\\s]+")).filter(StringUtils::hasText).findFirst());
        } catch (RuntimeException ex) {
            log.debug("Preferences d'envoi de {} illisibles : {}", document.customerCode(), ex.getMessage());
        }
        return preferred
                .or(() -> memberContact.whatsAppPhone(document.customerType(), document.customerCode()))
                .or(() -> MemberBillingContact.firstPhone(document.customerPhone()));
    }

    static String amount(BigDecimal value) {
        return value == null ? "0" : String.format(Locale.FRANCE, "%,d", value.setScale(0, RoundingMode.HALF_UP).longValue())
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }
}
