package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAuditEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une extension en cours de periode repousse la fin du contrat, sans avenant.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractTermExtensionTest {

    @Mock private ContractRepository contracts;
    @Mock private ContractAuditEventService audit;
    @InjectMocks private ContractServiceImpl service;

    private Contract contract(ContractStatus status, LocalDate end) {
        Contract contract = Contract.builder().contractCode("CTR-1").status(status).endDate(end).build();
        when(contracts.findByContractCode("CTR-1")).thenReturn(Optional.of(contract));
        return contract;
    }

    @Test
    @DisplayName("La fin est repoussee, meme sur un contrat amende, et l'extension est tracee")
    void extends_() {
        Contract contract = contract(ContractStatus.AMENDED, LocalDate.now().plusDays(3));

        assertThat(service.extendEndDate("CTR-1", LocalDate.now().plusDays(20), "Reprise")).isTrue();

        assertThat(contract.getEndDate()).isEqualTo(LocalDate.now().plusDays(20));
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.AMENDED);
        verify(audit).record(eq("CTR-1"), any(), eq("CONTRACT_TERM_EXTENDED"), any(), anyString(), any(), eq("Reprise"), anyString());
    }

    @Test
    @DisplayName("Un contrat echu dont la nouvelle fin est a venir redevient actif")
    void expiredComesBack() {
        Contract contract = contract(ContractStatus.EXPIRED, LocalDate.now().minusDays(2));

        assertThat(service.extendEndDate("CTR-1", LocalDate.now().plusDays(5), "Reprise")).isTrue();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    @Test
    @DisplayName("Remise en vigueur : seul un contrat echu est concerne")
    void reopensOnlyExpired() {
        Contract expired = contract(ContractStatus.EXPIRED, LocalDate.now().minusDays(2));
        assertThat(service.reopenExpired("CTR-1", "Abonnement en cours")).isTrue();
        assertThat(expired.getStatus()).isEqualTo(ContractStatus.ACTIVE);

        Contract terminated = contract(ContractStatus.TERMINATED, LocalDate.now().minusDays(2));
        assertThat(service.reopenExpired("CTR-1", "Abonnement en cours")).isFalse();
        assertThat(terminated.getStatus()).isEqualTo(ContractStatus.TERMINATED);
    }

    @Test
    @DisplayName("Jamais de raccourcissement · un contrat clos ne bouge pas")
    void neverShortensNorTouchesClosed() {
        contract(ContractStatus.ACTIVE, LocalDate.now().plusDays(30));
        assertThat(service.extendEndDate("CTR-1", LocalDate.now().plusDays(10), "Reprise")).isFalse();

        contract(ContractStatus.TERMINATED, LocalDate.now().minusDays(1));
        assertThat(service.extendEndDate("CTR-1", LocalDate.now().plusDays(10), "Reprise")).isFalse();

        verify(contracts, never()).save(any());
    }
}
