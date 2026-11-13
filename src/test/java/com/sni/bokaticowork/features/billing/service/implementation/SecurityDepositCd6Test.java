package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.SecurityDeposit;
import com.sni.bokaticowork.features.billing.model.SecurityDepositMovement;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositMovementRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Decision CD6 : la caution d'abonnement encaissee est creditee une fois sur le portefeuille, ou la
 * retenue la bloque ; a la sortie d'abonnement, le registre note les imputations et la restitution
 * reglees par le portefeuille, sans ecriture propre.
 */
class SecurityDepositCd6Test {

    private final SecurityDepositRepository deposits = mock(SecurityDepositRepository.class);
    private final SecurityDepositMovementRepository movements = mock(SecurityDepositMovementRepository.class);
    private final WalletHoldRepository holds = mock(WalletHoldRepository.class);
    private final WalletLedgerService ledger = mock(WalletLedgerService.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private SecurityDepositService service;
    private SecurityDeposit deposit;
    private final WalletAccount wallet = new WalletAccount();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<WalletHoldRepository> holdProvider = mock(ObjectProvider.class);
        when(holdProvider.getIfAvailable()).thenReturn(holds);
        service = new SecurityDepositService(deposits, movements, mock(BillingDocumentRepository.class), mock(BillingDocumentLineRepository.class),
                mock(ServiceCatalogItemRepository.class), sequences, mock(StaffProfileService.class), mock(ObjectProvider.class), holdProvider);
        ReflectionTestUtils.setField(service, "walletLedger", ledger);
        when(sequences.next(anyString())).thenReturn("MVC-1");
        deposit = SecurityDeposit.builder().id(5L).depositNumber("CAU-5").documentNumber("FAC-80").subscriptionNumber("SUB-9")
                .amount(new BigDecimal("200000")).currency("XAF").status("HELD").build();
        when(deposits.findByDocumentNumber("FAC-80")).thenReturn(Optional.of(deposit));
        when(deposits.findBySubscriptionNumberOrderByCreatedAtAsc("SUB-9")).thenReturn(List.of(deposit));
        WalletHold hold = new WalletHold();
        hold.setWallet(wallet);
        when(holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", "SUB-9")).thenReturn(List.of(hold));
        WalletLedgerEntry entry = new WalletLedgerEntry();
        entry.setEntryNumber("WLE-1");
        when(ledger.credit(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(entry);
    }

    @Test
    void shouldCreditTheWalletOnceWhenTheInvoiceIsPaid() {
        BillingDocument invoice = BillingDocument.builder().documentNumber("FAC-80").balanceDue(BigDecimal.ZERO).build();

        service.onInvoicePaid(invoice);
        service.onInvoicePaid(invoice);

        verify(ledger, times(1)).credit(eq(wallet), eq(new BigDecimal("200000")), eq(WalletEntryType.ADJUSTMENT),
                eq(SecurityDepositService.WALLET_SOURCE), eq("CAU-5"), eq("FAC-80"), eq("SYSTEM"), eq("CAUTION:CAU-5"));
        assertThat(deposit.getWalletEntryNumber()).isEqualTo("WLE-1");
    }

    @Test
    void shouldNotCreditBeforeTheInvoiceIsSettled() {
        service.onInvoicePaid(BillingDocument.builder().documentNumber("FAC-80").balanceDue(new BigDecimal("1000")).build());

        verify(ledger, never()).credit(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRecordTheSubscriptionExitWithoutPosting() {
        service.onSubscriptionSettled("SUB-9", List.<String[]>of(new String[]{"FAC-81", "50000"}), new BigDecimal("150000"));

        ArgumentCaptor<SecurityDepositMovement> saved = ArgumentCaptor.forClass(SecurityDepositMovement.class);
        verify(movements, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allMatch(SecurityDepositMovement::getWalletSettled);
        assertThat(deposit.getAppliedAmount()).isEqualByComparingTo("50000");
        assertThat(deposit.getReleasedAmount()).isEqualByComparingTo("150000");
        assertThat(deposit.getStatus()).isEqualTo("SETTLED");
    }
}
