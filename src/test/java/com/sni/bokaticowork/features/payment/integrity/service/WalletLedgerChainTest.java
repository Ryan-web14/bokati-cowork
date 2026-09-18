package com.sni.bokaticowork.features.payment.integrity.service;

import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Le chaînage du journal de portefeuille.
 *
 * <p>Ce que ces tests vérifient n'est pas que l'empreinte soit « une empreinte », mais qu'elle
 * réagisse à chacun des champs qu'une falsification voudrait toucher. Une empreinte insensible au
 * montant, ou au solde obtenu, donnerait l'apparence d'une chaîne sans en avoir la propriété · et
 * l'apparence est pire que rien, puisqu'on cesserait de regarder.</p>
 */
class WalletLedgerChainTest {

    private static final Instant MOMENT = Instant.parse("2026-09-18T10:15:30Z");

    private WalletLedgerEntry entry() {
        WalletAccount wallet = WalletAccount.builder().walletNumber("WAL-0001").build();
        return WalletLedgerEntry.builder()
                .entryNumber("WLE-0001")
                .wallet(wallet)
                .direction(WalletEntryDirection.DEBIT)
                .amount(new BigDecimal("1500.0000"))
                .currency("XAF")
                .balanceAfter(new BigDecimal("8500.0000"))
                .entryType(WalletEntryType.PAYMENT)
                .createdAt(MOMENT)
                .build();
    }

    @Test
    void lEmpreinteEstStableAEcritureIdentique() {
        assertEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(entry(), "abc"),
                "Une empreinte qui varie d'un calcul à l'autre rendrait toute vérification impossible");
    }

    @Test
    void leMontantEntreDansLEmpreinte() {
        WalletLedgerEntry altered = entry();
        altered.setAmount(new BigDecimal("1501.0000"));

        assertNotEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(altered, "abc"));
    }

    @Test
    void leSoldeObtenuEntreDansLEmpreinte() {
        // Le solde résultant y figure parce que c'est lui qu'une falsification cherche à déplacer :
        // on peut contrefaire un montant sans toucher au solde affiché, l'inverse est plus rare.
        WalletLedgerEntry altered = entry();
        altered.setBalanceAfter(new BigDecimal("9500.0000"));

        assertNotEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(altered, "abc"));
    }

    @Test
    void leSensDeLEcritureEntreDansLEmpreinte() {
        WalletLedgerEntry altered = entry();
        altered.setDirection(WalletEntryDirection.CREDIT);

        assertNotEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(altered, "abc"));
    }

    @Test
    void lInstantEntreDansLEmpreinte() {
        WalletLedgerEntry altered = entry();
        altered.setCreatedAt(MOMENT.plusSeconds(1));

        assertNotEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(altered, "abc"));
    }

    @Test
    void leMaillonPrecedentEntreDansLEmpreinte() {
        // C'est ce point précis qui fait la chaîne : sans lui, chaque ligne serait scellée seule et
        // l'on pourrait en retirer une du milieu sans que rien ne le signale.
        assertNotEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(entry(), "def"));
    }

    @Test
    void unMaillonAbsentVautLOrigine() {
        assertEquals(WalletLedgerChain.hash(entry(), WalletLedgerChain.GENESIS),
                WalletLedgerChain.hash(entry(), null),
                "La première écriture d'un portefeuille n'a rien derrière elle, et cela doit être un cas défini");
    }

    @Test
    void laMisesAEchelleDuMontantNeChangePasLEmpreinte() {
        // 1500 et 1500,0000 sont le même montant. Les distinguer ferait échouer la vérification
        // d'un journal parfaitement intact au premier changement d'échelle décimale.
        WalletLedgerEntry rescaled = entry();
        rescaled.setAmount(new BigDecimal("1500"));
        rescaled.setBalanceAfter(new BigDecimal("8500"));

        assertEquals(WalletLedgerChain.hash(entry(), "abc"), WalletLedgerChain.hash(rescaled, "abc"));
    }
}
