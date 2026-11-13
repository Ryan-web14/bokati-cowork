package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.DepositPlanRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.DepositResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDepositMode;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B5 : une proforma n'est ni une creance, ni une piece scellee, ni encaissable directement ; un
 * paiement recu sur elle devient un acompte du devis issu d'elle (decision D2).
 */
class ProformaB5Test {

    private final BillingLifecycleSupport lifecycle = new BillingLifecycleSupport();

    @Test
    void shouldNotCountAProformaAsAReceivable() {
        assertThat(BillingReceivables.receivable(BillingDocumentType.PROFORMA_INVOICE, BillingDocumentStatus.SENT)).isFalse();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.PROFORMA_INVOICE, BillingDocumentStatus.SENT,
                new BigDecimal("100000"), new BigDecimal("100000"))).isEqualByComparingTo("0");
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT)).isTrue();
    }

    @Test
    void shouldNeitherSealNorCollectAProforma() {
        BillingDocument proforma = BillingDocument.builder().documentNumber("PRF-1").documentType(BillingDocumentType.PROFORMA_INVOICE)
                .status(BillingDocumentStatus.SENT).locked(false).build();

        assertThatThrownBy(() -> lifecycle.ensureCanValidate(proforma)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> lifecycle.ensureCanPay(proforma)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> lifecycle.ensureCanCredit(proforma)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldTurnAPaymentOnAProformaIntoADepositOfTheQuote() {
        BillingDocumentServiceImpl service = mock(BillingDocumentServiceImpl.class, CALLS_REAL_METHODS);
        DepositService deposits = mock(DepositService.class);
        ObjectProvider<DepositService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(deposits);
        ReflectionTestUtils.setField(service, "depositServices", provider);
        BillingDocument proforma = BillingDocument.builder().documentNumber("PRF-1").documentType(BillingDocumentType.PROFORMA_INVOICE)
                .status(BillingDocumentStatus.SENT).totalAmount(new BigDecimal("1000000")).build();
        doReturn(proforma).when(service).serviceByNumber("PRF-1");
        BillingDocumentResponse quote = mock(BillingDocumentResponse.class);
        when(quote.documentNumber()).thenReturn("DEV-1");
        doReturn(quote).when(service).proformaToQuote("PRF-1");
        when(deposits.plan(eq("DEV-1"), any())).thenReturn(List.of(new DepositResponse(1L, "DEV-1", null, 1, null, "AMOUNT",
                new BigDecimal("400000"), null, new BigDecimal("400000"), null, "INVOICED", "FAC-A1", BigDecimal.ZERO, null, null, null, null, null,
                Instant.now())));
        BillingDocument depositInvoice = BillingDocument.builder().documentNumber("FAC-A1").documentType(BillingDocumentType.INVOICE).build();
        doReturn(depositInvoice).when(service).applyPayment(eq("FAC-A1"), any());

        BillingDocument allocated = service.applyPayment("PRF-1", new BigDecimal("400000"));

        // L'imputation se fait sur la facture d'acompte, pas sur la proforma.
        assertThat(allocated.getDocumentNumber()).isEqualTo("FAC-A1");
        ArgumentCaptor<DepositPlanRequest> plan = ArgumentCaptor.forClass(DepositPlanRequest.class);
        verify(deposits).plan(eq("DEV-1"), plan.capture());
        assertThat(plan.getValue().deposits().get(0).mode()).isEqualTo(BillingDepositMode.AMOUNT);
        assertThat(plan.getValue().deposits().get(0).value()).isEqualByComparingTo("400000");
    }
}
