package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingRecipientRequest;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingDocumentRecipientUpdateTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private BillingDocumentEditHistoryRepository editHistoryRepository;

    @Mock
    private BillingEventWriter eventWriter;

    @Mock
    private BillingDocumentMapper mapper;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    private BillingDocument document;

    @BeforeEach
    void setUp() {
        document = BillingDocument.builder()
                .documentNumber("INV-001")
                .customerType("CUSTOMER")
                .customerCode("CUS-42")
                .customerName("Ancien nom")
                .customerEmail("ancien@example.com")
                .customerPhone("0600000000")
                .customerNiu("NIU-OLD")
                .locked(Boolean.TRUE)
                .build();
        when(documentRepository.findByDocumentNumber("INV-001")).thenReturn(Optional.of(document));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldUpdateThePrintedRecipientWithoutTouchingOwnership() {
        document.setLocked(Boolean.FALSE);
        service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                "Nouveau nom", "nouveau@example.com", null, null, null, null, null, null,
                "Erreur de saisie a la creation", "ryan"));

        assertThat(document.getCustomerName()).isEqualTo("Nouveau nom");
        assertThat(document.getCustomerEmail()).isEqualTo("nouveau@example.com");
        // Un champ absent de la requete garde sa valeur.
        assertThat(document.getCustomerPhone()).isEqualTo("0600000000");
        // Le proprietaire du document n'est jamais modifiable par ce chemin : le DTO ne le porte
        // meme pas, et customerCode entre dans la chaine de hachage fiscale.
        assertThat(document.getCustomerType()).isEqualTo("CUSTOMER");
        assertThat(document.getCustomerCode()).isEqualTo("CUS-42");
    }

    @Test
    void shouldClearAFieldWhenAnEmptyStringIsSent() {
        document.setLocked(Boolean.FALSE);
        service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                null, null, null, "", null, null, null, null, "NIU errone", "ryan"));

        // Distinction null / chaine vide : sans elle, un NIU errone serait ineffacable.
        assertThat(document.getCustomerNiu()).isNull();
    }

    @Test
    void shouldRefuseToChangeTheLegalIdentityOfASealedDocument() {
        // trg_billing_document_immutable protege customer_name / customer_niu /
        // billing_address_json des qu'un document est scelle. Sans cette garde applicative,
        // la contrainte remontait en JpaSystemException au flush, donc en 500 opaque.
        assertThat(catchThrowable(() -> service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                "Nom corrige", null, null, null, null, null, null, null, "correction", "ryan"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("customerName")
                .hasMessageContaining("avoir");

        assertThat(catchThrowable(() -> service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                null, null, null, "NIU-NEW", null, null, null, null, "correction", "ryan"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("customerNiu");

        assertThat(document.getCustomerName()).isEqualTo("Ancien nom");
        assertThat(document.getCustomerNiu()).isEqualTo("NIU-OLD");
    }

    @Test
    void shouldStillAllowContactDetailsOnASealedDocument() {
        // L'email et le telephone ne sont pas couverts par le declencheur : les corriger sur une
        // facture validee est le cas d'usage courant (relance qui n'arrive pas au bon contact).
        service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                null, "nouveau@example.com", "0611111111", null, null, null, null, null,
                "contact errone", "ryan"));

        assertThat(document.getCustomerEmail()).isEqualTo("nouveau@example.com");
        assertThat(document.getCustomerPhone()).isEqualTo("0611111111");
    }

    @Test
    void shouldAcceptResendingAnIdenticalValueOnASealedDocument() {
        // Renvoyer la valeur deja en place n'est pas une modification : un frontend qui poste
        // le formulaire complet ne doit pas etre rejete parce qu'il inclut le nom inchange.
        service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                "Ancien nom", "nouveau@example.com", null, null, null, null, null, null,
                "correction email", "ryan"));

        assertThat(document.getCustomerEmail()).isEqualTo("nouveau@example.com");
    }

    @Test
    void shouldRecordTheCorrectionInTheEditHistory() {
        document.setLocked(Boolean.FALSE);
        service.updateRecipient("INV-001", new UpdateBillingRecipientRequest(
                "Nouveau nom", null, null, null, null, null, null, null, "Erreur de saisie", "ryan"));

        ArgumentCaptor<BillingDocumentEditHistory> captor =
                ArgumentCaptor.forClass(BillingDocumentEditHistory.class);
        verify(editHistoryRepository).save(captor.capture());
        BillingDocumentEditHistory history = captor.getValue();

        assertThat(history.getEditType()).isEqualTo("RECIPIENT_UPDATED");
        assertThat(history.getChangedBy()).isEqualTo("ryan");
        assertThat(history.getSnapshotJson())
                .contains("Ancien nom")
                .contains("Nouveau nom")
                .contains("Erreur de saisie");
    }
}
