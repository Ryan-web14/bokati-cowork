package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos.TransferView;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import com.sni.bokaticowork.core.generator.uuid.TimeOrderedUuid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * La vue d'un transfert nomme le portefeuille d'en face · hors de toute session.
 *
 * <p>Le controleur construit la reponse une fois la transaction du service refermee. Tant que
 * l'association restait paresseuse, la reponse partait en erreur interne alors que le transfert
 * etait bien enregistre · le titulaire voyait echouer une operation qui avait reussi. Ce test
 * lit la vue hors transaction, comme le controleur, et echouerait a nouveau si le chargement
 * disparaissait.</p>
 */
class WalletTransferViewIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired private WalletAccountRepository walletRepository;
    @Autowired private WalletTransferRepository transferRepository;

    private WalletAccount source;
    private WalletAccount target;

    @BeforeEach
    void setUp() {
        source = walletRepository.save(wallet("WLT-TEST-SRC", "MBR-SRC"));
        target = walletRepository.save(wallet("WLT-TEST-TGT", "MBR-TGT"));
    }

    private static WalletAccount wallet(String number, String owner) {
        return WalletAccount.builder()
                .walletNumber(number).ownerType("MEMBER").ownerCode(owner).currency("XAF")
                .status(WalletStatus.ACTIVE)
                .availableBalance(new BigDecimal("100000")).ledgerBalance(new BigDecimal("100000"))
                .heldBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void theViewReadsTheCounterpartyOutsideAnySession() {
        WalletTransfer saved = transferRepository.save(WalletTransfer.builder()
                .transferNumber("WTR-TEST-0001")
                .transferUuid(TimeOrderedUuid.next())
                .sourceWallet(source).targetWallet(target)
                .amount(new BigDecimal("2500")).feeAmount(BigDecimal.ZERO).currency("XAF")
                .status(WalletTransferStatus.PENDING_CONFIRMATION)
                .initiatedBy("MBR-SRC")
                .expiresAt(Instant.now().plusSeconds(600))
                .build());

        // Relu comme le fait le service, puis rendu comme le fait le controleur · hors transaction.
        WalletTransfer reloaded = transferRepository.findByTransferNumber(saved.getTransferNumber()).orElseThrow();

        assertThatCode(() -> {
            TransferView view = TransferView.of(reloaded, source.getId());
            assertThat(view.direction()).isEqualTo("OUT");
            assertThat(view.counterpartyWallet()).isEqualTo("WLT-TEST-TGT");
        }).doesNotThrowAnyException();

        TransferView received = TransferView.of(reloaded, target.getId());
        assertThat(received.direction()).isEqualTo("IN");
        assertThat(received.counterpartyWallet()).isEqualTo("WLT-TEST-SRC");
    }

    @Test
    void theListedTransfersCarryTheirCounterpartyToo() {
        transferRepository.save(WalletTransfer.builder()
                .transferNumber("WTR-TEST-0002")
                .transferUuid(TimeOrderedUuid.next())
                .sourceWallet(source).targetWallet(target)
                .amount(new BigDecimal("1000")).feeAmount(BigDecimal.ZERO).currency("XAF")
                .status(WalletTransferStatus.COMPLETED)
                .initiatedBy("MBR-SRC")
                .build());

        var page = transferRepository.findInvolving(source.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThatCode(() -> page.getContent().forEach(transfer -> {
            TransferView view = TransferView.of(transfer, source.getId());
            assertThat(view.counterpartyWallet()).isNotBlank();
        })).doesNotThrowAnyException();
    }
}
