package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * L'alerte de solde bas.
 *
 * <p>Elle part une fois quand le solde passe sous le seuil, et se réarme quand il repasse
 * au-dessus. Sans cela un titulaire à 500 F recevrait un courriel par café.</p>
 */
@ExtendWith(MockitoExtension.class)
class WalletBalanceWatchTest {

    @Mock
    private WalletNotifier notifier;

    @InjectMocks
    private WalletBalanceWatch watch;

    private WalletAccount wallet(String balance, String threshold, Instant alertedAt) {
        return WalletAccount.builder()
                .walletNumber("WAL-1")
                .availableBalance(new BigDecimal(balance))
                .lowBalanceThreshold(threshold == null ? null : new BigDecimal(threshold))
                .lowBalanceAlertedAt(alertedAt)
                .build();
    }

    @Test
    void sansSeuilRienNePart() {
        watch.afterBalanceChange(wallet("10", null, null));

        verify(notifier, never()).lowBalance(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lAlertePartUneFoisSousLeSeuil() {
        WalletAccount wallet = wallet("400", "500", null);

        watch.afterBalanceChange(wallet);
        watch.afterBalanceChange(wallet);
        watch.afterBalanceChange(wallet);

        verify(notifier, times(1)).lowBalance(wallet);
        assertNotNull(wallet.getLowBalanceAlertedAt());
    }

    @Test
    void lAlerteSeRearmeQuandLeSoldeRemonte() {
        WalletAccount wallet = wallet("400", "500", Instant.now());

        wallet.setAvailableBalance(new BigDecimal("600"));
        watch.afterBalanceChange(wallet);
        assertNull(wallet.getLowBalanceAlertedAt(), "Remonté au-dessus du seuil · la prochaine descente méritera une alerte");

        wallet.setAvailableBalance(new BigDecimal("300"));
        watch.afterBalanceChange(wallet);
        verify(notifier, times(1)).lowBalance(wallet);
    }

    @Test
    void auSeuilExactRienNePart() {
        watch.afterBalanceChange(wallet("500", "500", null));

        verify(notifier, never()).lowBalance(org.mockito.ArgumentMatchers.any());
    }
}
