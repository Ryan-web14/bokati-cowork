package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.DeliveryNoteDtos;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.DeliveryNote;
import com.sni.bokaticowork.features.billing.model.DeliveryNoteLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.DeliveryNoteLineRepository;
import com.sni.bokaticowork.features.billing.repository.DeliveryNoteRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C4 : cinq chaises livrees en deux fois (3 puis 2), stock sorti a chaque livraison, refus
 * au-dela du reste, retour en stock a l'annulation, facture des deux bons aux quantites livrees.
 */
class DeliveryNoteC4Test {

    private final DeliveryNoteRepository notes = mock(DeliveryNoteRepository.class);
    private final DeliveryNoteLineRepository noteLines = mock(DeliveryNoteLineRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final BillingDocumentLineRepository documentLines = mock(BillingDocumentLineRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final BillingStockService stock = mock(BillingStockService.class);
    private final BillingDocumentServiceImpl billing = mock(BillingDocumentServiceImpl.class);
    private DeliveryNoteService service;

    private final List<DeliveryNote> savedNotes = new ArrayList<>();
    private final List<DeliveryNoteLine> savedLines = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(1);
    private BillingDocument quote;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingStockService> stocks = mock(ObjectProvider.class);
        when(stocks.getIfAvailable()).thenReturn(stock);
        ObjectProvider<BillingDocumentServiceImpl> billings = mock(ObjectProvider.class);
        when(billings.getObject()).thenReturn(billing);
        service = new DeliveryNoteService(notes, noteLines, documents, documentLines, sequences, staff, stocks, billings);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("magasin@elleaose.com", "Magasin", null));
        when(sequences.next("billing_delivery_note")).thenAnswer(invocation -> "BL-" + ids.get());

        quote = BillingDocument.builder().id(10L).documentNumber("DEV-7").documentType(BillingDocumentType.QUOTE)
                .status(BillingDocumentStatus.ACCEPTED).customerType("CUSTOMER").customerCode("ORG-1").customerName("Société Mbote").build();
        BillingDocumentLine chairs = BillingDocumentLine.builder().id(100L).lineOrder(1).itemCode("ART-CHAISE").lineType(BillingLineType.PRODUCT)
                .description("Chaise").quantity(new BigDecimal("5")).unitPrice(new BigDecimal("25000")).optional(false).build();
        when(documents.findByDocumentNumber("DEV-7")).thenReturn(Optional.of(quote));
        when(documentLines.findAllByDocumentOrderByLineOrderAscIdAsc(quote)).thenReturn(List.of(chairs));
        when(documentLines.findAllById(any())).thenReturn(List.of(chairs));

        when(notes.save(any())).thenAnswer(invocation -> {
            DeliveryNote note = invocation.getArgument(0);
            if (note.getId() == null) {
                note.setId(ids.getAndIncrement());
                savedNotes.add(note);
            }
            return note;
        });
        when(notes.findBySourceDocumentNumberOrderByCreatedAtAsc("DEV-7")).thenAnswer(invocation -> List.copyOf(savedNotes));
        when(notes.findByNoteNumber(anyString())).thenAnswer(invocation -> savedNotes.stream()
                .filter(note -> note.getNoteNumber().equals(invocation.getArgument(0))).findFirst());
        when(noteLines.saveAll(anyList())).thenAnswer(invocation -> {
            savedLines.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(noteLines.findByNoteIdIn(any())).thenAnswer(invocation -> {
            Collection<Long> wanted = invocation.getArgument(0);
            return savedLines.stream().filter(line -> wanted.contains(line.getNoteId())).toList();
        });
        when(noteLines.findByNoteIdOrderByLineOrderAsc(any())).thenAnswer(invocation -> savedLines.stream()
                .filter(line -> line.getNoteId().equals(invocation.getArgument(0))).toList());
        when(stock.issueForDelivery(anyString(), eq("DEV-7"), anyList())).thenReturn(true);
    }

    @Test
    void shouldDeliverFiveChairsInTwoNotesAndRefuseMoreThanTheRest() {
        String first = service.create("DEV-7", request(3)).noteNumber();
        service.deliver(first, new DeliveryNoteDtos.DeliverRequest("M. Okemba"));

        assertThatThrownBy(() -> service.create("DEV-7", request(3)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("LIVRAISON_AU_DELA_DU_RESTE"));

        DeliveryNoteDtos.NoteView second = service.create("DEV-7", null);
        assertThat(second.lines()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualByComparingTo("2"));
        service.deliver(second.noteNumber(), null);

        DeliveryNoteDtos.DeliveryStatus status = service.status("DEV-7");
        assertThat(status.complete()).isTrue();
        assertThat(status.lines().get(0).delivered()).isEqualByComparingTo("5");
        verify(stock, org.mockito.Mockito.times(2)).issueForDelivery(anyString(), eq("DEV-7"), anyList());
        assertThatThrownBy(() -> service.create("DEV-7", null))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("RIEN_A_LIVRER"));
    }

    @Test
    void shouldReturnTheGoodsWhenADeliveredNoteIsCancelled() {
        String number = service.create("DEV-7", request(3)).noteNumber();
        service.deliver(number, null);

        assertThatThrownBy(() -> service.cancel(number, " ")).isInstanceOf(BadRequestException.class);
        DeliveryNoteDtos.NoteView cancelled = service.cancel(number, "Chaises abîmées au transport");

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        verify(stock).returnForDelivery(eq(number), anyList());
        // Le reste a livrer revient a cinq.
        assertThat(service.status("DEV-7").lines().get(0).remaining()).isEqualByComparingTo("5");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInvoiceTheDeliveredQuantitiesOfSeveralNotes() {
        String first = service.create("DEV-7", request(3)).noteNumber();
        service.deliver(first, null);
        String second = service.create("DEV-7", request(1)).noteNumber();
        assertThatThrownBy(() -> service.invoice(List.of(first, second)))
                .isInstanceOf(ConflictException.class);
        service.deliver(second, null);
        BillingDocumentResponse invoice = mock(BillingDocumentResponse.class);
        when(invoice.documentNumber()).thenReturn("FAC-20");
        when(billing.invoiceDeliveredQuantities(eq("DEV-7"), any(), anyString())).thenReturn(invoice);

        service.invoice(List.of(first, second));

        ArgumentCaptor<Map<Integer, BigDecimal>> quantities = ArgumentCaptor.forClass(Map.class);
        verify(billing).invoiceDeliveredQuantities(eq("DEV-7"), quantities.capture(), anyString());
        assertThat(quantities.getValue().get(1)).isEqualByComparingTo("4");
        assertThat(service.get(first).status()).isEqualTo("INVOICED");
        assertThat(service.get(first).invoiceNumber()).isEqualTo("FAC-20");
        assertThatThrownBy(() -> service.cancel(first, "Erreur")).isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRefuseADraftQuote() {
        quote.setStatus(BillingDocumentStatus.DRAFT);
        assertThatThrownBy(() -> service.create("DEV-7", null))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("LIVRAISON_IMPOSSIBLE"));
    }

    private static DeliveryNoteDtos.CreateRequest request(int quantity) {
        return new DeliveryNoteDtos.CreateRequest(List.of(new DeliveryNoteDtos.LineQuantity(1, new BigDecimal(quantity))), null, null, false);
    }
}
