package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.PaymentOriginResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La recharge administrative passe par le double visa au-dela du seuil, et son auteur est le compte connecte. */
class WalletTopUpFourEyesTest {

    private final WalletService wallets = mock(WalletService.class);
    private final WalletAdminActionService actions = mock(WalletAdminActionService.class);
    private final PaymentOriginResolver origin = mock(PaymentOriginResolver.class);
    private final WalletController controller = new WalletController(wallets, actions, origin);

    private WalletTopUpRequest request(String amount) {
        return new WalletTopUpRequest("MEMBER", "MBR-1", "XAF", new BigDecimal(amount), "un autre nom", "REF-9", null);
    }

    @Test
    void belowTheThresholdItCreditsAtOnceInTheNameOfTheConnectedAccount() {
        when(origin.currentActor()).thenReturn("agent@elleaose.com");
        when(actions.exceedsFourEyesThreshold(any())).thenReturn(false);

        ResponseEntity<?> response = controller.adminTopUp(request("5000"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ArgumentCaptor<WalletTopUpRequest> sent = ArgumentCaptor.forClass(WalletTopUpRequest.class);
        verify(wallets).adminTopUp(sent.capture());
        assertThat(sent.getValue().createdBy()).isEqualTo("agent@elleaose.com");
    }

    @Test
    void aboveTheThresholdItWaitsForASecondVisa() {
        when(origin.currentActor()).thenReturn("agent@elleaose.com");
        when(actions.exceedsFourEyesThreshold(any())).thenReturn(true);
        when(wallets.getOrCreate("MEMBER", "MBR-1", "XAF")).thenReturn(mock(WalletResponse.class, invocation ->
                "walletNumber".equals(invocation.getMethod().getName()) ? "W-1" : null));
        WalletAccount account = new WalletAccount();
        account.setWalletNumber("W-1");
        WalletAdminAction pending = WalletAdminAction.builder().actionNumber("ACT-1").wallet(account)
                .actionType(WalletAdminAction.Type.TOPUP).status(WalletAdminAction.Status.PENDING_APPROVAL).build();
        when(actions.request(eq("W-1"), any(), eq("agent@elleaose.com"))).thenReturn(pending);

        ResponseEntity<?> response = controller.adminTopUp(request("250000"));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(wallets, never()).adminTopUp(any());
    }
}
