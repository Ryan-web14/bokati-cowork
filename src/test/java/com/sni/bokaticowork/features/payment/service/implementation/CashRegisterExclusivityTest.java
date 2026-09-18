package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.dto.request.OpenCashSessionRequest;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * L'exclusivité de la caisse automatique.
 *
 * <p>Une caisse automatique n'a pas de caissier à qui demander des comptes. Sa seule vérification
 * sérieuse est le rapprochement de son total avec le journal du moyen de paiement qu'elle enregistre
 * · et ce rapprochement ne veut plus rien dire dès qu'une écriture étrangère s'y est glissée, qu'elle
 * vienne d'une main humaine ou d'un autre moyen de paiement.</p>
 *
 * <p>Deux règles, donc, et elles sont distinctes : on ne saisit pas à la main sur une caisse tenue
 * par le système, et on n'y enregistre pas un moyen de paiement qui n'est pas le sien.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashRegisterExclusivityTest {

    @Mock
    private CashRegisterRepository registerRepository;

    @Mock
    private CashSessionRepository sessionRepository;

    @Mock
    private CashMovementRepository movementRepository;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private CashSessionSummarySupport summarySupport;

    @Mock
    private CashRegisterMapper mapper;

    @Mock
    private com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier emailNotifier;

    @Mock
    private com.sni.bokaticowork.features.payment.repository.CashMovementAttachmentRepository attachmentRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CashRegisterServiceImpl service;

    private CashRegister walletRegister() {
        return CashRegister.builder()
                .id(10L)
                .registerCode("CSR-AUTO-WALLET")
                .name("Caisse automatique - Portefeuille client")
                .businessEntityCode("BIZ-AUTO-PAYMENT")
                .active(true)
                .cashControlEnabled(true)
                .systemManaged(true)
                .restrictedToMethod(PaymentMethod.WALLET)
                .build();
    }

    private CashRegister ordinaryRegister() {
        return CashRegister.builder()
                .id(11L)
                .registerCode("CSR-ACCUEIL")
                .name("Caisse accueil")
                .businessEntityCode("BIZ-1")
                .active(true)
                .cashControlEnabled(true)
                .systemManaged(false)
                .build();
    }

    private CashSession openSessionOn(CashRegister register) {
        return CashSession.builder()
                .id(100L)
                .sessionNumber("CSS-0001")
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openingAmount(BigDecimal.ZERO)
                .build();
    }

    // ---------------------------------------------------------------------------------------

    @Test
    void ouvrirUneSessionSurUneCaisseAutomatiqueEstRefuse() {
        when(registerRepository.findByRegisterCode("CSR-AUTO-WALLET")).thenReturn(Optional.of(walletRegister()));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> service.openSession(new OpenCashSessionRequest("CSR-AUTO-WALLET", "caissier", BigDecimal.ZERO)));

        assertTrue(thrown.getMessage().contains("CSR-AUTO-WALLET"),
                "Le refus doit nommer la caisse · un caissier qui tombe là-dessus s'est trompé de caisse");
    }

    @Test
    void unePieceDeCaisseSurUneCaisseAutomatiqueEstRefusee() {
        when(sessionRepository.findBySessionNumber("CSS-0001"))
                .thenReturn(Optional.of(openSessionOn(walletRegister())));

        CreateCashVoucherRequest request = voucher();

        assertThrows(BadRequestException.class, () -> service.createEntryVoucher("CSS-0001", request));
        assertThrows(BadRequestException.class, () -> service.createExitVoucher("CSS-0001", request));
    }

    @Test
    void unePieceDeCaisseSurUneCaisseOrdinaireResteAcceptee() {
        // Le garde ne doit pas se refermer sur les caisses normales : c'est la moitié de la règle
        // qui se casse le plus facilement en la posant trop haut.
        when(sessionRepository.findBySessionNumber("CSS-0001"))
                .thenReturn(Optional.of(openSessionOn(ordinaryRegister())));
        when(sequenceGenerator.next(anyString())).thenReturn("CSM-0001");
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(summarySupport.expectedClosingAmount(any())).thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() -> service.createEntryVoucher("CSS-0001", voucher()));
    }

    @Test
    void enregistrerUnPaiementALaMainSurUneCaisseAutomatiqueEstRefuse() {
        when(sessionRepository.findBySessionNumber("CSS-0001"))
                .thenReturn(Optional.of(openSessionOn(walletRegister())));

        assertThrows(BadRequestException.class,
                () -> service.recordPayment("CSS-0001", new BigDecimal("1000"), "PTX-1", "caissier"));
    }

    @Test
    void unPaiementMobileMoneyNEntrePasDansUneCaisseRestreinteAuPortefeuille() {
        // Configuration cassée : la caisse trouvée sous le code du mobile money porte la restriction
        // du portefeuille. Le service ne la réécrit pas en silence · la réécrire effacerait le garde
        // au moment précis où il a quelque chose à dire, et le total de la caisse cesserait d'être
        // rapprochable du journal du portefeuille sans que personne ne l'apprenne.
        CashRegister mismatched = walletRegister();
        mismatched.setRegisterCode("CSR-AUTO-MOBILE-MONEY");
        when(registerRepository.findByRegisterCode("CSR-AUTO-MOBILE-MONEY")).thenReturn(Optional.of(mismatched));
        when(movementRepository.existsByReferenceTypeAndReferenceCode(anyString(), anyString())).thenReturn(false);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("PTX-1")
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .status(PaymentTransactionStatus.SUCCEEDED)
                .amount(new BigDecimal("1000"))
                .currency("XAF")
                .build();

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> service.recordAutomaticPayment(transaction));
        assertTrue(thrown.getMessage().contains("Portefeuille client"),
                "Le refus doit dire ce que la caisse accepte, pas seulement ce qu'elle refuse");
    }

    @Test
    void uneCaisseAutomatiqueAncienneRecoitSaMarqueAuPremierPassage() {
        // Une caisse créée avant cette règle n'en porte pas la marque. On la lui pose plutôt que de
        // la laisser ouverte à la saisie jusqu'à la prochaine migration.
        CashRegister legacy = CashRegister.builder()
                .id(12L)
                .registerCode("CSR-AUTO-WALLET")
                .name("Caisse automatique - Portefeuille client")
                .active(true)
                .cashControlEnabled(true)
                .systemManaged(false)
                .build();
        when(registerRepository.findByRegisterCode("CSR-AUTO-WALLET")).thenReturn(Optional.of(legacy));
        when(registerRepository.save(any(CashRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.existsByReferenceTypeAndReferenceCode(anyString(), anyString())).thenReturn(false);
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findFirstByCashRegisterIdAndStatusForUpdate(any(), anyString()))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenReturn("SEQ-0001");
        when(summarySupport.expectedClosingAmount(any())).thenReturn(BigDecimal.ZERO);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("PTX-2")
                .paymentMethod(PaymentMethod.WALLET)
                .status(PaymentTransactionStatus.SUCCEEDED)
                .amount(new BigDecimal("1000"))
                .currency("XAF")
                .build();
        service.recordAutomaticPayment(transaction);

        // La caisse est corrigée avant tout enregistrement · c'est ce moment-là qui compte.
        assertTrue(Boolean.TRUE.equals(legacy.getSystemManaged()));
        assertTrue(legacy.getRestrictedToMethod() == PaymentMethod.WALLET);
    }

    private CreateCashVoucherRequest voucher() {
        return new CreateCashVoucherRequest(new BigDecimal("1000"), "DOC-1", "MISC",
                null, null, null, null, null, "motif", "caissier",
                CashDocumentType.ENTRY_VOUCHER, null);
    }
}
