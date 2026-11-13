package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppService;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot C2 : un devis, une proforma ou un avoir envoye par WhatsApp part avec son propre modele,
 * jamais avec « la facture est a regler ».
 */
class BillingWhatsAppC2Test {

    @Test
    void shouldChooseTheTemplateFromTheDocumentType() {
        BillingDocument quote = document(BillingDocumentType.QUOTE, "DEV-2026-00087", "450000", "450000");
        BillingSendingService.WhatsAppContent content = BillingSendingService.whatsAppContent(quote);
        assertThat(content.purpose()).isEqualTo("QUOTE_AVAILABLE");
        assertThat(content.values()).containsExactly("Awa", "DEV-2026-00087", "450 000", "15/12/2026");
        assertThat(content.portalPath()).isEqualTo("devis");

        assertThat(BillingSendingService.whatsAppContent(document(BillingDocumentType.PROFORMA_INVOICE, "PRF-1", "100000", "100000")).purpose())
                .isEqualTo("PROFORMA_AVAILABLE");

        BillingDocument credit = document(BillingDocumentType.CREDIT_NOTE, "AV-5", "25000", "0");
        credit.setOriginalDocumentNumber("FAC-12");
        assertThat(BillingSendingService.whatsAppContent(credit).values()).containsExactly("Awa", "AV-5", "25 000", "FAC-12");

        assertThat(BillingSendingService.whatsAppContent(document(BillingDocumentType.INVOICE, "FAC-9", "118000", "118000")).purpose())
                .isEqualTo("PAYMENT_REMINDER");
        assertThat(BillingSendingService.whatsAppContent(document(BillingDocumentType.INVOICE, "FAC-9", "118000", "0")).purpose())
                .isEqualTo("INVOICE_PAID");
    }

    @Test
    void shouldOfferTheNewTemplatesForCreation() {
        assertThat(WhatsAppService.DEFAULTS).extracting(WhatsAppService.DefaultTemplate::purpose)
                .contains("QUOTE_AVAILABLE", "PROFORMA_AVAILABLE", "CREDIT_NOTE_ISSUED");
        // Le nombre d'exemples par variable est verifie pour tous les modeles par WhatsAppServiceTest.
        WhatsAppService.DEFAULTS.stream()
                .filter(template -> List.of("QUOTE_AVAILABLE", "PROFORMA_AVAILABLE", "CREDIT_NOTE_ISSUED").contains(template.purpose()))
                .forEach(template -> assertThat(template.text()).as(template.name()).contains("ELLE A OSE"));
    }

    private static BillingDocument document(BillingDocumentType type, String number, String total, String balance) {
        BillingDocument document = new BillingDocument();
        document.setDocumentType(type);
        document.setDocumentNumber(number);
        document.setCustomerName("Awa Mabiala");
        document.setTotalAmount(new BigDecimal(total));
        document.setBalanceDue(new BigDecimal(balance));
        document.setPaidAmount(new BigDecimal(total).subtract(new BigDecimal(balance)));
        document.setDueDate(LocalDate.of(2026, 12, 15));
        return document;
    }
}
