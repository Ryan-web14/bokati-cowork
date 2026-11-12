package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le contrat d'un abonnement vit aussi longtemps que lui · il n'expire pas, et ne se reconduit pas
 * par un second contrat.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionContractLifecycleTest {

    @Mock private ContractRepository contracts;
    @Mock private OutboxService outbox;
    @Mock private SequenceGeneratorFacade sequences;
    @Mock private SubscriptionRepository subscriptions;
    @InjectMocks private ContractLifecycleAutomationServiceImpl service;

    private Contract contract(String code) {
        return Contract.builder().contractCode(code).status(ContractStatus.ACTIVE).renewalType(ContractRenewalType.AUTO_RENEW)
                .startDate(LocalDate.now().minusMonths(2)).endDate(LocalDate.now().minusDays(5)).build();
    }

    private void due(Contract... due) {
        when(contracts.findAllByStatusInAndEndDateBeforeAndDeletedFalse(anyList(), any())).thenReturn(List.of(due));
    }

    @Test
    @DisplayName("Abonnement en cours : le contrat dont la date est passee reste en vigueur, aucun second contrat")
    void openSubscriptionKeepsItsContract() {
        Contract carried = contract("CTR-ABO");
        due(carried);
        when(subscriptions.findContractCodesOfOpenSubscriptions()).thenReturn(List.of("CTR-ABO"));

        service.processScheduledTransitions();

        assertThat(carried.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        verify(contracts, never()).save(any());
    }

    @Test
    @DisplayName("Sans abonnement en cours, un contrat arrive a son terme expire comme avant")
    void otherContractsStillExpire() {
        Contract free = contract("CTR-LIBRE");
        due(free);

        service.processScheduledTransitions();

        assertThat(free.getStatus()).isEqualTo(ContractStatus.EXPIRED);
    }

    @Test
    @DisplayName("AUTO_RENEW hors abonnement : un contrat successeur est cree avant que l ancien n expire")
    void autoRenewCreatesSuccessorBeforeExpiry() {
        Contract renewable = contract("CTR-AUTO");
        // Le depot filtre sur le statut courant, comme la requete reelle
        when(contracts.findAllByStatusInAndEndDateBeforeAndDeletedFalse(anyList(), any())).thenAnswer(inv -> {
            List<ContractStatus> statuses = inv.getArgument(0);
            return statuses.contains(renewable.getStatus()) ? List.of(renewable) : List.of();
        });
        when(sequences.next(any(), any())).thenReturn("CTR-NEXT");

        service.processScheduledTransitions();

        ArgumentCaptor<Contract> saved = ArgumentCaptor.forClass(Contract.class);
        verify(contracts, atLeastOnce()).save(saved.capture());
        List<Contract> successors = new ArrayList<>(saved.getAllValues().stream()
                .filter(c -> "CTR-AUTO".equals(c.getRenewedFromCode())).toList());
        assertThat(successors).hasSize(1);
        assertThat(successors.get(0).getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(successors.get(0).getContractCode()).isEqualTo("CTR-NEXT");
        verify(outbox).publish(org.mockito.ArgumentMatchers.eq("CONTRACT_AUTO_RENEWED"), any(), any(), any());
        assertThat(renewable.getStatus()).isEqualTo(ContractStatus.EXPIRED);
    }
}
