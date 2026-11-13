package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.SecurityDepositDtos;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.SecurityDeposit;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositMovementRepository;
import com.sni.bokaticowork.features.billing.repository.SecurityDepositRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C7 : une caution de 200 000 s'inscrit au registre a l'emission ; une fois la facture soldee,
 * 50 000 sont retenus pour degats (motif requis) et 150 000 restitues. Une caution d'abonnement
 * encore couverte par sa retenue de portefeuille se regle a la sortie d'abonnement.
 */
class SecurityDepositC7Test {

    private final SecurityDepositRepository deposits = mock(SecurityDepositRepository.class);
    private final SecurityDepositMovementRepository movements = mock(SecurityDepositMovementRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final BillingDocumentLineRepository lines = mock(BillingDocumentLineRepository.class);
    private final ServiceCatalogItemRepository catalog = mock(ServiceCatalogItemRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final BillingDocumentServiceImpl billing = mock(BillingDocumentServiceImpl.class);
    private final WalletHoldRepository holds = mock(WalletHoldRepository.class);
    private SecurityDepositService service;
    private BillingDocument invoice;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingDocumentServiceImpl> billings = mock(ObjectProvider.class);
        when(billings.getObject()).thenReturn(billing);
        ObjectProvider<WalletHoldRepository> holdProvider = mock(ObjectProvider.class);
        when(holdProvider.getIfAvailable()).thenReturn(holds);
        service = new SecurityDepositService(deposits, movements, documents, lines, catalog, sequences, staff, billings, holdProvider);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("accueil@elleaose.com", "Accueil", null));
        when(sequences.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-1");
        when(deposits.save(any())).thenAnswer(invocation -> {
            SecurityDeposit deposit = invocation.getArgument(0);
            deposit.setId(1L);
            return deposit;
        });
        when(catalog.findByItemCode(anyString())).thenReturn(Optional.empty());
        invoice = BillingDocument.builder().documentNumber("FAC-40").documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.PAID)
                .customerType("CUSTOMER").customerCode("ORG-9").currency("XAF").balanceDue(BigDecimal.ZERO).build();
        when(documents.findByDocumentNumber("FAC-40")).thenReturn(Optional.of(invoice));
        when(deposits.findByDocumentNumber("FAC-40")).thenReturn(Optional.empty());
    }

    @Test
    void shouldRegisterTheDepositLinesOfAnIssuedInvoice() {
        when(lines.findAllByDocumentOrderByLineOrderAscIdAsc(invoice)).thenReturn(List.of(
                line("SUBSCRIPTION_RECURRING", BillingLineType.SUBSCRIPTION, "300000"),
                line("DEPOSIT", BillingLineType.SERVICE, "200000")));

        service.registerFromInvoice(invoice);

        ArgumentCaptor<SecurityDeposit> saved = ArgumentCaptor.forClass(SecurityDeposit.class);
        verify(deposits).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("200000");
        assertThat(saved.getValue().getSubscriptionNumber()).isEqualTo("SUB-7");
    }

    @Test
    void shouldNotTakeADepositInvoiceForASecurityDeposit() {
        BillingDocument depositInvoice = BillingDocument.builder().documentNumber("FAC-A1").depositForDocumentNumber("DEV-1").build();
        BillingDocumentLine depositLine = line("DEPOSIT", BillingLineType.ADJUSTMENT, "100000");
        assertThat(SecurityDepositService.isSecurityDepositLine(depositLine, depositInvoice, catalog)).isFalse();
        assertThat(SecurityDepositService.isSecurityDepositLine(line("DEPOSIT", BillingLineType.SERVICE, "1"), invoice, catalog)).isTrue();
    }

    @Test
    void shouldRetainWithAReasonThenReleaseTheRest() {
        SecurityDeposit deposit = deposit(null);

        assertThatThrownBy(() -> service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RETAIN", new BigDecimal("50000"), null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("MOTIF_REQUIS"));
        service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RETAIN", new BigDecimal("50000"), "Moquette tachée", null));
        SecurityDepositDtos.DepositView view = service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RELEASE", null, null, null));

        assertThat(view.retainedAmount()).isEqualByComparingTo("50000");
        assertThat(view.releasedAmount()).isEqualByComparingTo("150000");
        assertThat(view.status()).isEqualTo("SETTLED");
        assertThat(deposit.remaining()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RELEASE", BigDecimal.ONE, null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("CAUTION_MONTANT"));
    }

    @Test
    void shouldApplyTheDepositOnAnOpenInvoiceOfTheSameCustomer() {
        deposit(null);
        BillingDocument open = BillingDocument.builder().documentNumber("FAC-41").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).customerCode("ORG-9").balanceDue(new BigDecimal("80000")).build();
        when(documents.findByDocumentNumber("FAC-41")).thenReturn(Optional.of(open));

        service.move("CAU-1", new SecurityDepositDtos.MovementRequest("APPLY", new BigDecimal("80000"), null, "FAC-41"));

        verify(billing).applyPayment("FAC-41", new BigDecimal("80000"));
    }

    @Test
    void shouldRefuseAnUnpaidInvoiceAndAHeldSubscriptionDeposit() {
        deposit("SUB-7");
        when(holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", "SUB-7")).thenReturn(List.of(new WalletHold()));
        assertThatThrownBy(() -> service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RELEASE", null, null, null)))
                .satisfies(error -> assertThat(((ConflictException) error).getReason()).isEqualTo("CAUTION_ABONNEMENT"));

        invoice.setBalanceDue(new BigDecimal("1000"));
        assertThatThrownBy(() -> service.move("CAU-1", new SecurityDepositDtos.MovementRequest("RELEASE", null, null, null)))
                .satisfies(error -> assertThat(((ConflictException) error).getReason()).isEqualTo("CAUTION_NON_ENCAISSEE"));
    }

    private SecurityDeposit deposit(String subscription) {
        SecurityDeposit deposit = SecurityDeposit.builder().id(1L).depositNumber("CAU-1").documentNumber("FAC-40").subscriptionNumber(subscription)
                .customerCode("ORG-9").amount(new BigDecimal("200000")).currency("XAF").status("HELD").build();
        when(deposits.findByDepositNumber("CAU-1")).thenReturn(Optional.of(deposit));
        return deposit;
    }

    private static BillingDocumentLine line(String sourceType, BillingLineType type, String total) {
        return BillingDocumentLine.builder().lineType(type).sourceType(sourceType).sourceCode("SUB-7").description(sourceType)
                .totalAmount(new BigDecimal(total)).optional(false).build();
    }
}
