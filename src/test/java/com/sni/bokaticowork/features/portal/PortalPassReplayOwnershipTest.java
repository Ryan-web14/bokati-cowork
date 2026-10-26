package com.sni.bokaticowork.features.portal;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassPurchaseRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassCreationOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** La cle d'idempotence d'un autre membre ne rend pas son pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalPassReplayOwnershipTest {

    @Mock private PassRepository passRepository;
    @InjectMocks private PassCreationOperator operator;

    private void existingPassOf(String memberId) {
        Pass pass = new Pass();
        pass.setOwnerType(SubscriberType.MEMBER);
        pass.setOwnerCode(memberId);
        when(passRepository.findByIdempotencyKey("cle-partagee")).thenReturn(Optional.of(pass));
    }

    private static CreatePassPurchaseRequest purchaseBy(String memberId) {
        return new CreatePassPurchaseRequest(SubscriberType.MEMBER, memberId, null, "XAF", null, false, null, "cle-partagee");
    }

    @Test
    @DisplayName("Le pass d'un autre membre n'est pas rendu")
    void anotherMembersKeyIsRefused() {
        existingPassOf("MBR-2");

        assertThatThrownBy(() -> operator.createFromPlan("PLAN-JOUR", purchaseBy("MBR-1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Le rejeu de sa propre demande rend son pass, comme avant")
    void ownReplayStillWorks() {
        existingPassOf("MBR-1");

        assertThat(operator.createFromPlan("PLAN-JOUR", purchaseBy("MBR-1")).getOwnerCode()).isEqualTo("MBR-1");
    }
}
