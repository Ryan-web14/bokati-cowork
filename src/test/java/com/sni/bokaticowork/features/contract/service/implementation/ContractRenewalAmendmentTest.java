package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import com.sni.bokaticowork.features.contract.model.ContractAmendmentVariable;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentSectionRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentVariableRepository;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAmendmentService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAuditEventService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le renouvellement paye prolonge le meme contrat · un avenant applique, des dates qui suivent.
 */
class ContractRenewalAmendmentTest {

    private static final LocalDate END = LocalDate.of(2026, 10, 31);
    private static final ContractAmendmentService.RenewalTerms TERMS = new ContractAmendmentService.RenewalTerms(
            "SUB-1", LocalDate.of(2026, 10, 1), END, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 1));

    private final ContractAmendmentRepository amendments = mock(ContractAmendmentRepository.class);
    private final ContractAmendmentVariableRepository variables = mock(ContractAmendmentVariableRepository.class);
    private final ContractRepository contracts = mock(ContractRepository.class);
    private final ContractService contractService = mock(ContractService.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final ContractAmendmentServiceImpl service = new ContractAmendmentServiceImpl(amendments,
            mock(ContractAmendmentSectionRepository.class), variables, contracts, contractService,
            mock(ContractAuditEventService.class), sequences, mock(DocumentService.class));

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = Contract.builder().contractCode("CTR-1").status(ContractStatus.ACTIVE).endDate(END).expiryAlertSentDays(7).build();
        when(contractService.serviceByCode("CTR-1")).thenReturn(contract);
        when(sequences.next(eq("AMENDMENT"), any(LocalDate.class))).thenReturn("AVN-1");
        when(amendments.save(any(ContractAmendment.class))).thenAnswer(call -> call.getArgument(0));
        when(amendments.findAllByOriginalContractCodeAndStatusIn(anyString(), anyList())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Avenant applique sans signature, fin du contrat reportee, contrat marque amende")
    void appliesAndMovesTheDates() {
        Optional<ContractAmendmentResponse> applied = service.applyRenewal("CTR-1", TERMS);

        assertThat(applied).isPresent();
        ArgumentCaptor<ContractAmendment> amendment = ArgumentCaptor.forClass(ContractAmendment.class);
        verify(amendments).save(amendment.capture());
        assertThat(amendment.getValue().getStatus()).isEqualTo(AmendmentStatus.ACTIVE);
        assertThat(amendment.getValue().getEffectiveDate()).isEqualTo(TERMS.newStart());
        assertThat(amendment.getValue().getOriginalContractCode()).isEqualTo("CTR-1");
        assertThat(contract.getEndDate()).isEqualTo(TERMS.newEnd());
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.AMENDED);
        assertThat(contract.getExpiryAlertSentDays()).isNull();
        ArgumentCaptor<ContractAmendmentVariable> variable = ArgumentCaptor.forClass(ContractAmendmentVariable.class);
        verify(variables, org.mockito.Mockito.times(3)).save(variable.capture());
        assertThat(variable.getAllValues()).extracting(ContractAmendmentVariable::getNewValue)
                .containsExactly("2026-11-01", "2026-11-30", "2026-12-01");
    }

    @Test
    @DisplayName("Un contrat echu en attendant le paiement et un contrat deja amende se prolongent aussi")
    void expiredAndAmendedAreRenewable() {
        contract.setStatus(ContractStatus.EXPIRED);
        assertThat(service.applyRenewal("CTR-1", TERMS)).isPresent();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.AMENDED);

        assertThat(service.applyRenewal("CTR-1", new ContractAmendmentService.RenewalTerms("SUB-1", TERMS.newStart(), TERMS.newEnd(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))).isPresent();
        assertThat(contract.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Evenement rejoue : pas de second avenant pour la meme periode")
    void replayIsIdempotent() {
        ContractAmendment existing = ContractAmendment.builder().code("AVN-0").originalContractCode("CTR-1")
                .status(AmendmentStatus.ACTIVE).effectiveDate(TERMS.newStart()).description("x").build();
        when(amendments.findAllByOriginalContractCodeAndStatusIn("CTR-1", List.of(AmendmentStatus.ACTIVE))).thenReturn(List.of(existing));
        contract.setEndDate(TERMS.newEnd());

        assertThat(service.applyRenewal("CTR-1", TERMS)).isPresent();

        verify(amendments, never()).save(any());
    }

    @Test
    @DisplayName("Un ancien avenant signe a la main sans bouger les dates n'empeche pas le rattrapage")
    void signedAmendmentWithoutDatesDoesNotBlock() {
        ContractAmendment signed = ContractAmendment.builder().code("AVN-0").originalContractCode("CTR-1")
                .status(AmendmentStatus.ACTIVE).effectiveDate(TERMS.newStart()).description("x").build();
        when(amendments.findAllByOriginalContractCodeAndStatusIn("CTR-1", List.of(AmendmentStatus.ACTIVE))).thenReturn(List.of(signed));

        assertThat(service.applyRenewal("CTR-1", TERMS)).isPresent();

        assertThat(contract.getEndDate()).isEqualTo(TERMS.newEnd());
    }

    @Test
    @DisplayName("Un ancien avenant de renouvellement bloque en examen est remplace · un avenant saisi par l'equipe reste")
    void staleRenewalAmendmentIsSuperseded() {
        ContractAmendment stale = ContractAmendment.builder().code("AVN-OLD").status(AmendmentStatus.UNDER_REVIEW)
                .description("Renouvellement de l'abonnement SUB-1 · nouvelle période du 2026-10-01 au 2026-10-31").build();
        ContractAmendment manual = ContractAmendment.builder().code("AVN-MAN").status(AmendmentStatus.UNDER_REVIEW)
                .proposedBy(5L).description("Changement d'adresse").build();
        when(amendments.findAllByOriginalContractCodeAndStatusIn(eq("CTR-1"),
                eq(List.of(AmendmentStatus.DRAFT, AmendmentStatus.UNDER_REVIEW, AmendmentStatus.PENDING_SIGNATURE))))
                .thenReturn(List.of(stale, manual));

        assertThat(service.applyRenewal("CTR-1", TERMS)).isPresent();

        assertThat(stale.getStatus()).isEqualTo(AmendmentStatus.CANCELLED);
        assertThat(manual.getStatus()).isEqualTo(AmendmentStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Contrat pas encore signe : la date est reportee, sans avenant · contrat clos : rien")
    void noAmendmentBeforeSignatureOrAfterTheEnd() {
        contract.setStatus(ContractStatus.AWAITING_SIGNATURE);
        assertThat(service.applyRenewal("CTR-1", TERMS)).isEmpty();
        verify(contractService).extendEndDate(eq("CTR-1"), eq(TERMS.newEnd()), anyString());

        contract.setStatus(ContractStatus.TERMINATED);
        assertThat(service.applyRenewal("CTR-1", TERMS)).isEmpty();
        verify(amendments, never()).save(any());
    }
}
