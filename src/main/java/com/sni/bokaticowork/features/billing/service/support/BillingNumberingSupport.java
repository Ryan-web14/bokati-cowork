package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BillingNumberingSupport {

    private final SequenceGeneratorFacade sequenceGenerator;

    public String nextDocumentNumber(BillingDocumentType type, String customerType) {
        String seqCode = switch (type) {
            case QUOTE               -> "billing_quote";
            case PROFORMA_INVOICE    -> "billing_proforma";
            case INVOICE             -> "billing_invoice";
            case CREDIT_NOTE         -> "billing_credit_note";
            case CORRECTIVE_INVOICE  -> "billing_corrective_invoice";
            case DEBIT_NOTE          -> "billing_debit_note";
        };
        String prefix = switch (type) {
            case QUOTE               -> "QUO";
            case PROFORMA_INVOICE    -> "PRF";
            case INVOICE             -> "INV";
            case CREDIT_NOTE         -> "CRN";
            case CORRECTIVE_INVOICE  -> "REC";
            case DEBIT_NOTE          -> "DBN";
        };
        String ctx = CodeComposer.abbrev(customerType);
        long seq = CodeComposer.extractSeq(sequenceGenerator.next(seqCode));
        return CodeComposer.withDay(prefix, ctx, LocalDate.now(), seq);
    }
}
