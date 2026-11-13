package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.DeliveryNoteDtos;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.implementation.DeliveryNotePdf;
import com.sni.bokaticowork.features.billing.service.implementation.DeliveryNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lot C4 : bons de livraison. Un devis accepte ou une facture emise se livre en une ou plusieurs
 * fois ; les bons livres d'un devis se facturent aux quantites livrees.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final DeliveryNoteService deliveryNotes;
    private final DeliveryNotePdf pdf;

    /** Nouveau bon (brouillon) tire d'un devis accepte ou d'une facture ; sans lignes, tout le reste a livrer. */
    @PostMapping("/documents/{documentNumber}/delivery-notes")
    public ResponseEntity<DeliveryNoteDtos.NoteView> create(@PathVariable String documentNumber,
                                                            @Valid @RequestBody(required = false) DeliveryNoteDtos.CreateRequest request) {
        return ResponseEntity.ok(deliveryNotes.create(documentNumber, request));
    }

    /** Reste a livrer par ligne, et les bons de la piece. */
    @GetMapping("/documents/{documentNumber}/delivery-status")
    public ResponseEntity<DeliveryNoteDtos.DeliveryStatus> status(@PathVariable String documentNumber) {
        return ResponseEntity.ok(deliveryNotes.status(documentNumber));
    }

    @GetMapping("/delivery-notes")
    public ResponseEntity<List<DeliveryNoteDtos.NoteSummary>> list(@RequestParam(required = false) String customerType,
                                                                   @RequestParam(required = false) String customerCode,
                                                                   @RequestParam(required = false) String status) {
        return ResponseEntity.ok(deliveryNotes.list(customerType, customerCode, status));
    }

    @GetMapping("/delivery-notes/{noteNumber}")
    public ResponseEntity<DeliveryNoteDtos.NoteView> get(@PathVariable String noteNumber) {
        return ResponseEntity.ok(deliveryNotes.get(noteNumber));
    }

    @GetMapping("/delivery-notes/{noteNumber}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String noteNumber) {
        DeliveryNoteDtos.NoteView note = deliveryNotes.get(noteNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + note.noteNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.render(note));
    }

    /** Livre : sortie de stock si billing.stock.issue-on vaut DELIVERY. */
    @PatchMapping("/delivery-notes/{noteNumber}/deliver")
    public ResponseEntity<DeliveryNoteDtos.NoteView> deliver(@PathVariable String noteNumber,
                                                             @Valid @RequestBody(required = false) DeliveryNoteDtos.DeliverRequest request) {
        return ResponseEntity.ok(deliveryNotes.deliver(noteNumber, request));
    }

    /** Annule (motif requis) ; un bon livre rend sa marchandise au stock. Un bon facture se corrige par avoir. */
    @PatchMapping("/delivery-notes/{noteNumber}/cancel")
    public ResponseEntity<DeliveryNoteDtos.NoteView> cancel(@PathVariable String noteNumber,
                                                            @Valid @RequestBody DeliveryNoteDtos.CancelRequest request) {
        return ResponseEntity.ok(deliveryNotes.cancel(noteNumber, request.reason()));
    }

    /** Copie signee par le client, deposee en GED : sa reference. */
    @PatchMapping("/delivery-notes/{noteNumber}/signed-copy")
    public ResponseEntity<DeliveryNoteDtos.NoteView> signedCopy(@PathVariable String noteNumber,
                                                                @Valid @RequestBody DeliveryNoteDtos.SignedCopyRequest request) {
        return ResponseEntity.ok(deliveryNotes.signedCopy(noteNumber, request.reference()));
    }

    /** Facture des bons livres d'un meme devis, aux quantites livrees (facture en brouillon). */
    @PostMapping("/delivery-notes/invoice")
    public ResponseEntity<BillingDocumentResponse> invoice(@Valid @RequestBody DeliveryNoteDtos.InvoiceRequest request) {
        return ResponseEntity.ok(deliveryNotes.invoice(request.noteNumbers()));
    }
}
