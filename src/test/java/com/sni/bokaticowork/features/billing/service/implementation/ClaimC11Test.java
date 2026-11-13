package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingClaim;
import com.sni.bokaticowork.features.billing.model.BillingClaimReason;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingClaimReasonRepository;
import com.sni.bokaticowork.features.billing.repository.BillingClaimRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C11 : une reclamation ouverte suspend les relances ; acceptee en partie, elle produit un
 * avoir du montant accorde applique sur le reste du ; un refus doit etre motive ; une seule
 * reclamation ouverte par facture.
 */
class ClaimC11Test {

    private final BillingClaimRepository claims = mock(BillingClaimRepository.class);
    private final BillingClaimReasonRepository reasons = mock(BillingClaimReasonRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final BillingDocumentServiceImpl billing = mock(BillingDocumentServiceImpl.class);
    private ClaimService service;
    private final List<BillingClaim> stored = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingDocumentServiceImpl> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(billing);
        service = new ClaimService(claims, reasons, documents, sequences, staff, provider);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("relation@elleaose.com", "Relation client", null));
        when(sequences.next("billing_claim")).thenReturn("RCL-1");
        when(reasons.findByCode("ERREUR_PRIX")).thenReturn(Optional.of(BillingClaimReason.builder().code("ERREUR_PRIX").label("Erreur de prix").active(true).build()));
        when(documents.findByDocumentNumber("FAC-60")).thenReturn(Optional.of(BillingDocument.builder().documentNumber("FAC-60")
                .documentType(BillingDocumentType.INVOICE).status(BillingDocumentStatus.ISSUED).customerCode("ORG-2")
                .totalAmount(new BigDecimal("300000")).balanceDue(new BigDecimal("300000")).build()));
        when(claims.save(any())).thenAnswer(invocation -> {
            BillingClaim claim = invocation.getArgument(0);
            if (!stored.contains(claim)) {
                stored.add(claim);
            }
            return claim;
        });
        when(claims.existsByDocumentNumberAndStatus(anyString(), eq("OPEN")))
                .thenAnswer(invocation -> stored.stream().anyMatch(c -> c.getDocumentNumber().equals(invocation.getArgument(0)) && "OPEN".equals(c.getStatus())));
        when(claims.findByClaimNumber("RCL-1")).thenAnswer(invocation -> stored.stream().findFirst());
    }

    @Test
    void shouldSuspendDunningThenSettleByAPartialCreditNote() {
        service.openByStaff("FAC-60", new ClaimDtos.OpenRequest("ERREUR_PRIX", "Tarif contrat non appliqué", List.of(1), new BigDecimal("50000")));
        assertThat(service.holdReason("FAC-60")).isPresent();
        assertThatThrownBy(() -> service.openByStaff("FAC-60", new ClaimDtos.OpenRequest("ERREUR_PRIX", null, null, null)))
                .isInstanceOf(ConflictException.class);

        BillingDocumentResponse credit = mock(BillingDocumentResponse.class);
        when(credit.documentNumber()).thenReturn("AV-9");
        when(billing.createCreditNote(eq("FAC-60"), any())).thenReturn(credit);
        ClaimDtos.ClaimView view = service.accept("RCL-1", new ClaimDtos.AcceptRequest(new BigDecimal("30000"), "Geste commercial"));

        assertThat(view.status()).isEqualTo("PARTIALLY_ACCEPTED");
        assertThat(view.creditNoteNumber()).isEqualTo("AV-9");
        ArgumentCaptor<CreateCreditNoteRequest> request = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billing).createCreditNote(eq("FAC-60"), request.capture());
        assertThat(request.getValue().amount()).isEqualByComparingTo("30000");
        assertThat(request.getValue().applyImmediately()).isTrue();
        assertThat(service.holdReason("FAC-60")).isEmpty();
    }

    @Test
    void shouldAskAReasonToReject() {
        service.openByStaff("FAC-60", new ClaimDtos.OpenRequest("ERREUR_PRIX", null, null, null));

        assertThatThrownBy(() -> service.reject("RCL-1", " "))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("MOTIF_REQUIS"));
        assertThat(service.reject("RCL-1", "Tarif conforme au contrat signé").status()).isEqualTo("REJECTED");
    }
}
