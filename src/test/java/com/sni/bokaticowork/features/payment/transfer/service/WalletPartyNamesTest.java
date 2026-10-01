package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.TransferView;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * « WLT-00000004 » ne dit rien à personne.
 *
 * <p>Avant de confirmer un transfert, le titulaire doit reconnaître son destinataire · un numéro
 * de portefeuille ne s'apprend pas par cœur. Le nom accompagne donc le numéro partout, et une
 * page de vingt lignes ne relit pas vingt fois le même.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletPartyNamesTest {

    @Mock private TransactionContextResolver contextResolver;

    @InjectMocks
    private WalletPartyNames partyNames;

    private WalletAccount source;
    private WalletAccount target;

    @BeforeEach
    void setUp() {
        source = WalletAccount.builder().id(1L).walletNumber("WLT-00000003").ownerType("MEMBER").ownerCode("MBR-3").build();
        target = WalletAccount.builder().id(2L).walletNumber("WLT-00000004").ownerType("MEMBER").ownerCode("MBR-4").build();
        when(contextResolver.resolveParty("MEMBER", "MBR-3"))
                .thenReturn(new TransactionContextResolver.PartyView("MEMBER", "MBR-3", "Ryan Bikindou", null, null, null, true));
        when(contextResolver.resolveParty("MEMBER", "MBR-4"))
                .thenReturn(new TransactionContextResolver.PartyView("MEMBER", "MBR-4", "Awa Ngoma", null, null, null, true));
    }

    @Test
    void theNameOfTheOwnerIsWhatIsShown() {
        assertEquals("Awa Ngoma", partyNames.of(target));
        assertNull(partyNames.of(null));
    }

    @Test
    void withoutANameTheNumberStandsIn() {
        when(contextResolver.resolveParty("MEMBER", "MBR-4"))
                .thenReturn(TransactionContextResolver.PartyView.unregistered("MEMBER", "MBR-4"));

        assertEquals("WLT-00000004", partyNames.of(target), "jamais une case vide");
    }

    @Test
    void aPageDoesNotReadTheSameNameTwice() {
        WalletPartyNames.Lookup lookup = partyNames.lookup();

        assertEquals("Awa Ngoma", lookup.of(target));
        assertEquals("Awa Ngoma", lookup.of(target));
        assertEquals("Ryan Bikindou", lookup.of(source));

        verify(contextResolver, times(1)).resolveParty(eq("MEMBER"), eq("MBR-4"));
        verify(contextResolver, times(1)).resolveParty(eq("MEMBER"), eq("MBR-3"));
    }

    @Test
    void theViewCarriesTheNameNextToTheNumber() {
        WalletTransfer transfer = WalletTransfer.builder()
                .transferNumber("WTR-202609-000002").sourceWallet(source).targetWallet(target)
                .amount(new BigDecimal("100000")).feeAmount(BigDecimal.ZERO).currency("XAF")
                .status(WalletTransferStatus.PENDING_CONFIRMATION).build();

        TransferView sent = TransferView.of(transfer, source.getId(), partyNames.of(target));
        assertEquals("OUT", sent.direction());
        assertEquals("WLT-00000004", sent.counterpartyWallet());
        assertEquals("Awa Ngoma", sent.counterpartyName());

        TransferView received = TransferView.of(transfer, target.getId(), partyNames.of(source));
        assertEquals("IN", received.direction());
        assertEquals("Ryan Bikindou", received.counterpartyName());
    }
}
