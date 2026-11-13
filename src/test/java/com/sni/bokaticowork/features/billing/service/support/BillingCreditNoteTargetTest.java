package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot B0 : une rectificative scellee et payee s'annule par avoir (le controle d'encaissement la refusait). */
class BillingCreditNoteTargetTest {

    private final BillingLifecycleSupport lifecycle = new BillingLifecycleSupport();

    @Test
    void shouldAcceptInvoicesCorrectivesAndDebitNotes() {
        for (BillingDocumentType type : new BillingDocumentType[]{BillingDocumentType.INVOICE, BillingDocumentType.CORRECTIVE_INVOICE,
                BillingDocumentType.DEBIT_NOTE}) {
            BillingDocument document = BillingDocument.builder().documentType(type).status(BillingDocumentStatus.PAID).build();
            assertThatCode(() -> lifecycle.ensureCanCredit(document)).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldRefuseQuotesCreditNotesAndCancelledDocuments() {
        assertThatThrownBy(() -> lifecycle.ensureCanCredit(BillingDocument.builder().documentType(BillingDocumentType.QUOTE)
                .status(BillingDocumentStatus.ACCEPTED).build())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> lifecycle.ensureCanCredit(BillingDocument.builder().documentType(BillingDocumentType.CREDIT_NOTE)
                .status(BillingDocumentStatus.ISSUED).build())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> lifecycle.ensureCanCredit(BillingDocument.builder().documentType(BillingDocumentType.CORRECTIVE_INVOICE)
                .status(BillingDocumentStatus.CANCELLED).build())).isInstanceOf(BadRequestException.class);
    }
}
