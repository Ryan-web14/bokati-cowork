package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.enums.OfflineMobileMoneyOperator;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManualPaymentDetailsSupportTest {

    @Mock private PaymentTransactionRepository transactionRepository;
    @InjectMocks private ManualPaymentDetailsSupport support;

    @Test
    @DisplayName("Mobile money hors plateforme : l'operateur devient le fournisseur, le numero est garde")
    void offlineMobileMoneyKeepsTheOperator() {
        var details = support.validate(PaymentMethod.MOBILE_MONEY_OFFLINE, " MP2611.1234 ", null, "Grace M.",
                "+242 06 123 45 67", OfflineMobileMoneyOperator.MTN_MOMO);

        assertThat(details.provider()).isEqualTo("MTN_MOMO");
        assertThat(details.reference()).isEqualTo("MP2611.1234");
        assertThat(details.payerPhone()).isEqualTo("+242 06 123 45 67");
        assertThat(PaymentMethodLabels.label(details.method(), details.provider()))
                .isEqualTo("Mobile money hors plateforme (MTN MoMo Pay)");
    }

    @Test
    @DisplayName("Mobile money hors plateforme sans operateur : refuse")
    void offlineMobileMoneyNeedsAnOperator() {
        assertThatThrownBy(() -> support.validate(PaymentMethod.MOBILE_MONEY_OFFLINE, "REF", null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("opérateur");
    }

    @Test
    @DisplayName("Cheque : numero et banque obligatoires")
    void chequeNeedsNumberAndBank() {
        assertThatThrownBy(() -> support.validate(PaymentMethod.CHEQUE, null, "BGFI", null, null, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("numéro du chèque");
        assertThatThrownBy(() -> support.validate(PaymentMethod.CHEQUE, "0012345", null, null, null, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("banque");

        var details = support.validate(PaymentMethod.CHEQUE, "0012345", "BGFI", "SARL Alpha", "060000000", null);
        assertThat(details.provider()).isEqualTo("CHEQUE");
        assertThat(details.bankName()).isEqualTo("BGFI");
        assertThat(details.payerPhone()).as("le numero ne concerne que le mobile money").isNull();
    }

    @Test
    @DisplayName("Virement : reference obligatoire, banque facultative")
    void transferNeedsAReference() {
        assertThatThrownBy(() -> support.validate(PaymentMethod.BANK_TRANSFER, " ", null, null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThat(support.validate(PaymentMethod.BANK_TRANSFER, "VIR-889", null, null, null, null).provider())
                .isEqualTo("BANK_TRANSFER");
    }

    @Test
    @DisplayName("Une meme reference deja encaissee pour le meme moyen : refusee")
    void sameReferenceTwiceIsRefused() {
        when(transactionRepository.findAllByPaymentMethodAndProviderAndProviderReferenceIgnoreCaseAndStatus(
                eq(PaymentMethod.MOBILE_MONEY_OFFLINE), eq("AIRTEL_MONEY"), eq("AM-1"), eq(PaymentTransactionStatus.SUCCEEDED)))
                .thenReturn(List.of(PaymentTransaction.builder().transactionNumber("TXN-1").build()));

        assertThatThrownBy(() -> support.validate(PaymentMethod.MOBILE_MONEY_OFFLINE, "AM-1", null, null, null,
                OfflineMobileMoneyOperator.AIRTEL_MONEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("AM-1");
    }

    @Test
    @DisplayName("Le meme numero de cheque d'une autre banque reste accepte")
    void sameChequeNumberFromAnotherBankIsAccepted() {
        when(transactionRepository.findAllByPaymentMethodAndProviderAndProviderReferenceIgnoreCaseAndStatus(
                any(), anyString(), anyString(), any()))
                .thenReturn(List.of(PaymentTransaction.builder().bankName("LCB").build()));

        assertThat(support.validate(PaymentMethod.CHEQUE, "0012345", "BGFI", null, null, null)).isNotNull();
        assertThatThrownBy(() -> support.validate(PaymentMethod.CHEQUE, "0012345", "lcb", null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Especes ou mobile money de la plateforme : pas un paiement hors plateforme")
    void platformMethodsAreRefused() {
        assertThatThrownBy(() -> support.validate(PaymentMethod.CASH, "X", null, null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> support.validate(PaymentMethod.MOBILE_MONEY, "X", null, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }
}
