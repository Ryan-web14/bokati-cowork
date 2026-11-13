package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lot C4 : bons de livraison. Un devis accepte ou une facture emise se livre en une ou plusieurs
 * fois ; chaque bon ne livre que le reste a livrer de ses lignes. A la livraison, le stock sort si
 * {@code billing.stock.issue-on} vaut DELIVERY (la facture ne sort alors plus rien). Les bons livres
 * d'un devis se facturent ensemble, aux quantites livrees.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryNoteService {

    static final String DRAFT = "DRAFT";
    static final String DELIVERED = "DELIVERED";
    static final String CANCELLED = "CANCELLED";
    static final String INVOICED = "INVOICED";
    private static final Set<BillingDocumentStatus> QUOTE_DELIVERABLE = EnumSet.of(BillingDocumentStatus.ACCEPTED,
            BillingDocumentStatus.DEPOSIT_REQUESTED, BillingDocumentStatus.DEPOSIT_PAID, BillingDocumentStatus.CONVERTED);
    private static final Set<BillingDocumentStatus> INVOICE_NOT_DELIVERABLE = EnumSet.of(BillingDocumentStatus.DRAFT,
            BillingDocumentStatus.CANCELLED);

    private final DeliveryNoteRepository notes;
    private final DeliveryNoteLineRepository noteLines;
    private final BillingDocumentRepository documents;
    private final BillingDocumentLineRepository documentLines;
    private final SequenceGeneratorFacade sequences;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingStockService> stockServices;
    private final ObjectProvider<BillingDocumentServiceImpl> billingServices;

    // ================================================================== creation

    @Transactional
    public DeliveryNoteDtos.NoteView create(String sourceNumber, DeliveryNoteDtos.CreateRequest request) {
        BillingDocument source = deliverableSource(sourceNumber);
        Map<Long, BigDecimal> delivered = deliveredBySourceLine(source.getDocumentNumber());
        List<BillingDocumentLine> lines = deliverableLines(source);
        Map<Integer, BigDecimal> wanted = new HashMap<>();
        if (request != null && request.lines() != null) {
            for (DeliveryNoteDtos.LineQuantity line : request.lines()) {
                if (line.lineOrder() == null || line.quantity() == null || line.quantity().signum() <= 0) {
                    throw BadRequestException.coded("LIVRAISON_INVALIDE", "Chaque ligne demande son numéro (lineOrder) et une quantité positive.");
                }
                wanted.merge(line.lineOrder(), line.quantity(), BigDecimal::add);
            }
        }
        DeliveryNote note = DeliveryNote.builder()
                .noteNumber(sequences.next("billing_delivery_note"))
                .sourceDocumentNumber(source.getDocumentNumber())
                .sourceDocumentType(source.getDocumentType().name())
                .customerType(source.getCustomerType()).customerCode(source.getCustomerCode()).customerName(source.getCustomerName())
                .status(DRAFT)
                .deliveryAddress(request != null && StringUtils.hasText(request.deliveryAddress()) ? request.deliveryAddress().trim()
                        : source.getDeliveryAddressJson())
                .notes(request == null ? null : trim(request.notes()))
                .showPrices(request != null && Boolean.TRUE.equals(request.showPrices()))
                .createdBy(staffProfiles.currentAuthor().login())
                .build();
        List<DeliveryNoteLine> created = new ArrayList<>();
        for (BillingDocumentLine line : lines) {
            BigDecimal remaining = line.getQuantity().subtract(delivered.getOrDefault(line.getId(), BigDecimal.ZERO));
            BigDecimal quantity = wanted.isEmpty() ? remaining : wanted.getOrDefault(line.getLineOrder(), BigDecimal.ZERO);
            if (quantity.signum() <= 0) {
                continue;
            }
            if (quantity.compareTo(remaining) > 0) {
                throw BadRequestException.coded("LIVRAISON_AU_DELA_DU_RESTE", "Ligne " + line.getLineOrder() + " (« " + line.getDescription()
                        + " ») : " + plain(quantity) + " demandé(s), il reste " + plain(remaining.max(BigDecimal.ZERO)) + " à livrer.");
            }
            created.add(DeliveryNoteLine.builder().sourceLineId(line.getId()).lineOrder(line.getLineOrder()).itemCode(line.getItemCode())
                    .sourceType(line.getSourceType()).lineType(line.getLineType() == null ? null : line.getLineType().name())
                    .description(line.getDescription()).unit(line.getUnit()).quantity(quantity).unitPrice(line.getUnitPrice()).build());
        }
        Set<Integer> known = lines.stream().map(BillingDocumentLine::getLineOrder).collect(Collectors.toSet());
        wanted.keySet().stream().filter(order -> !known.contains(order)).findFirst().ifPresent(order -> {
            throw BadRequestException.coded("LIVRAISON_INVALIDE", "La ligne " + order + " n'existe pas ou ne se livre pas (ligne optionnelle).");
        });
        if (created.isEmpty()) {
            throw BadRequestException.coded("RIEN_A_LIVRER", "Tout est déjà livré sur " + source.getDocumentNumber() + ".");
        }
        DeliveryNote saved = notes.save(note);
        created.forEach(line -> line.setNoteId(saved.getId()));
        noteLines.saveAll(created);
        return view(saved, created);
    }

    // ================================================================== suivi

    /** Reste a livrer par ligne de la piece d'origine. */
    @Transactional(readOnly = true)
    public DeliveryNoteDtos.DeliveryStatus status(String sourceNumber) {
        BillingDocument source = documents.findByDocumentNumber(sourceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable : " + sourceNumber));
        Map<Long, BigDecimal> delivered = deliveredBySourceLine(source.getDocumentNumber(), Set.of(DELIVERED, INVOICED));
        Map<Long, BigDecimal> planned = deliveredBySourceLine(source.getDocumentNumber(), Set.of(DRAFT));
        List<DeliveryNoteDtos.LineStatus> lines = deliverableLines(source).stream().map(line -> {
            BigDecimal done = delivered.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal draft = planned.getOrDefault(line.getId(), BigDecimal.ZERO);
            return new DeliveryNoteDtos.LineStatus(line.getLineOrder(), line.getItemCode(), line.getDescription(), line.getUnit(),
                    line.getQuantity(), done, draft, line.getQuantity().subtract(done).subtract(draft).max(BigDecimal.ZERO));
        }).toList();
        boolean complete = lines.stream().allMatch(line -> line.remaining().signum() == 0 && line.inDraft().signum() == 0);
        return new DeliveryNoteDtos.DeliveryStatus(source.getDocumentNumber(), source.getDocumentType().name(), complete, lines,
                notes.findBySourceDocumentNumberOrderByCreatedAtAsc(source.getDocumentNumber()).stream().map(this::summary).toList());
    }

    @Transactional(readOnly = true)
    public DeliveryNoteDtos.NoteView get(String number) {
        DeliveryNote note = note(number);
        return view(note, noteLines.findByNoteIdOrderByLineOrderAsc(note.getId()));
    }

    @Transactional(readOnly = true)
    public List<DeliveryNoteDtos.NoteSummary> list(String customerType, String customerCode, String status) {
        List<DeliveryNote> found = StringUtils.hasText(customerType) && StringUtils.hasText(customerCode)
                ? notes.findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(customerType, customerCode)
                : StringUtils.hasText(status) ? notes.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(java.util.Locale.ROOT))
                : notes.findTop200ByOrderByCreatedAtDesc();
        return found.stream()
                .filter(note -> !StringUtils.hasText(status) || note.getStatus().equalsIgnoreCase(status.trim()))
                .map(this::summary).toList();
    }

    // ================================================================== livraison, annulation

    /** Livre : la marchandise est partie. Sortie de stock en mode DELIVERY. */
    @Transactional
    public DeliveryNoteDtos.NoteView deliver(String number, DeliveryNoteDtos.DeliverRequest request) {
        DeliveryNote note = note(number);
        if (!DRAFT.equals(note.getStatus())) {
            throw ConflictException.coded("LIVRAISON_STATUT", "Le bon " + number + " est " + label(note.getStatus()) + " : seul un brouillon se livre.");
        }
        List<DeliveryNoteLine> lines = noteLines.findByNoteIdOrderByLineOrderAsc(note.getId());
        // Le reste a livrer a pu changer depuis la preparation (autre bon livre entre-temps).
        Map<Long, BigDecimal> done = deliveredBySourceLine(note.getSourceDocumentNumber(), Set.of(DELIVERED, INVOICED));
        Map<Long, BillingDocumentLine> sourceLines = documentLines.findAllById(lines.stream().map(DeliveryNoteLine::getSourceLineId).toList())
                .stream().collect(Collectors.toMap(BillingDocumentLine::getId, line -> line));
        for (DeliveryNoteLine line : lines) {
            BillingDocumentLine origin = sourceLines.get(line.getSourceLineId());
            if (origin != null && done.getOrDefault(origin.getId(), BigDecimal.ZERO).add(line.getQuantity()).compareTo(origin.getQuantity()) > 0) {
                throw BadRequestException.coded("LIVRAISON_AU_DELA_DU_RESTE", "Ligne " + line.getLineOrder()
                        + " : la quantité dépasse désormais le reste à livrer ; corrigez le bon.");
            }
        }
        BillingStockService stock = stockServices.getIfAvailable();
        if (stock != null) {
            String quote = BillingDocumentType.QUOTE.name().equals(note.getSourceDocumentType()) ? note.getSourceDocumentNumber() : null;
            note.setStockIssued(stock.issueForDelivery(note.getNoteNumber(), quote, asLines(lines)));
        }
        note.setStatus(DELIVERED);
        note.setDeliveredAt(Instant.now());
        note.setDeliveredBy(staffProfiles.currentAuthor().login());
        note.setReceivedBy(request == null ? null : trim(request.receivedBy()));
        return view(notes.save(note), lines);
    }

    /** Annule : un bon livre et non facture rend sa marchandise au stock si elle en etait sortie. */
    @Transactional
    public DeliveryNoteDtos.NoteView cancel(String number, String reason) {
        DeliveryNote note = note(number);
        if (CANCELLED.equals(note.getStatus())) {
            throw ConflictException.coded("LIVRAISON_STATUT", "Le bon " + number + " est déjà annulé.");
        }
        if (INVOICED.equals(note.getStatus())) {
            throw ConflictException.coded("LIVRAISON_STATUT", "Le bon " + number + " est facturé (" + note.getInvoiceNumber()
                    + ") : corrigez par un avoir sur la facture.");
        }
        if (!StringUtils.hasText(reason)) {
            throw BadRequestException.coded("MOTIF_REQUIS", "Indiquez le motif de l'annulation.");
        }
        List<DeliveryNoteLine> lines = noteLines.findByNoteIdOrderByLineOrderAsc(note.getId());
        BillingStockService stock = stockServices.getIfAvailable();
        if (DELIVERED.equals(note.getStatus()) && Boolean.TRUE.equals(note.getStockIssued()) && stock != null) {
            stock.returnForDelivery(note.getNoteNumber(), asLines(lines));
        }
        note.setStatus(CANCELLED);
        note.setCancelledAt(Instant.now());
        note.setCancelledBy(staffProfiles.currentAuthor().login());
        note.setCancelReason(reason.trim());
        return view(notes.save(note), lines);
    }

    /** Copie signee par le client : reference du fichier depose en GED. */
    @Transactional
    public DeliveryNoteDtos.NoteView signedCopy(String number, String reference) {
        if (!StringUtils.hasText(reference)) {
            throw BadRequestException.coded("LIVRAISON_INVALIDE", "Indiquez la référence du document déposé (GED).");
        }
        DeliveryNote note = note(number);
        note.setSignedCopyReference(reference.trim());
        return view(notes.save(note), noteLines.findByNoteIdOrderByLineOrderAsc(note.getId()));
    }

    // ================================================================== facturation

    /**
     * Facture des bons livres d'un meme devis, aux quantites livrees. Les bons passent en
     * INVOICED ; le devis passe en CONVERTED a sa premiere facture et reste la reference des
     * livraisons suivantes.
     */
    @Transactional
    public BillingDocumentResponse invoice(List<String> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            throw BadRequestException.coded("LIVRAISON_INVALIDE", "Choisissez au moins un bon livré.");
        }
        List<DeliveryNote> selected = numbers.stream().distinct().map(this::note).toList();
        String source = selected.get(0).getSourceDocumentNumber();
        for (DeliveryNote note : selected) {
            if (!DELIVERED.equals(note.getStatus())) {
                throw ConflictException.coded("LIVRAISON_STATUT", "Le bon " + note.getNoteNumber() + " est " + label(note.getStatus())
                        + " : seuls les bons livrés et non facturés se facturent.");
            }
            if (!BillingDocumentType.QUOTE.name().equals(note.getSourceDocumentType())) {
                throw BadRequestException.coded("LIVRAISON_DEJA_FACTUREE", "Le bon " + note.getNoteNumber()
                        + " livre une facture déjà émise (" + note.getSourceDocumentNumber() + ") : il n'y a rien à refacturer.");
            }
            if (!source.equals(note.getSourceDocumentNumber())) {
                throw BadRequestException.coded("LIVRAISON_INVALIDE", "Les bons d'une même facture viennent d'un même devis.");
            }
        }
        Map<Integer, BigDecimal> quantities = new LinkedHashMap<>();
        noteLines.findByNoteIdIn(selected.stream().map(DeliveryNote::getId).toList())
                .forEach(line -> quantities.merge(line.getLineOrder(), line.getQuantity(), BigDecimal::add));
        BillingDocumentResponse invoice = billingServices.getObject().invoiceDeliveredQuantities(source, quantities,
                selected.stream().map(DeliveryNote::getNoteNumber).collect(Collectors.joining(", ")));
        for (DeliveryNote note : selected) {
            note.setStatus(INVOICED);
            note.setInvoiceNumber(invoice.documentNumber());
            notes.save(note);
        }
        return invoice;
    }

    // ================================================================== outils

    private BillingDocument deliverableSource(String number) {
        BillingDocument source = documents.findByDocumentNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable : " + number));
        BillingDocumentType type = source.getDocumentType();
        boolean ok = type == BillingDocumentType.QUOTE ? QUOTE_DELIVERABLE.contains(source.getStatus())
                : type == BillingDocumentType.INVOICE && !INVOICE_NOT_DELIVERABLE.contains(source.getStatus())
                && !StringUtils.hasText(source.getDepositForDocumentNumber());
        if (!ok) {
            throw BadRequestException.coded("LIVRAISON_IMPOSSIBLE", "Un bon de livraison se tire d'un devis accepté ou d'une facture émise ("
                    + number + " : " + type + " " + source.getStatus() + ").");
        }
        return source;
    }

    private List<BillingDocumentLine> deliverableLines(BillingDocument source) {
        return documentLines.findAllByDocumentOrderByLineOrderAscIdAsc(source).stream()
                .filter(line -> !Boolean.TRUE.equals(line.getOptional()))
                .filter(line -> line.getQuantity() != null && line.getQuantity().signum() > 0)
                .filter(line -> line.getLineType() != BillingLineType.ADJUSTMENT)
                .toList();
    }

    private Map<Long, BigDecimal> deliveredBySourceLine(String sourceNumber) {
        return deliveredBySourceLine(sourceNumber, Set.of(DRAFT, DELIVERED, INVOICED));
    }

    private Map<Long, BigDecimal> deliveredBySourceLine(String sourceNumber, Set<String> statuses) {
        List<Long> ids = notes.findBySourceDocumentNumberOrderByCreatedAtAsc(sourceNumber).stream()
                .filter(note -> statuses.contains(note.getStatus())).map(DeliveryNote::getId).toList();
        Map<Long, BigDecimal> result = new HashMap<>();
        if (!ids.isEmpty()) {
            noteLines.findByNoteIdIn(ids).forEach(line -> result.merge(line.getSourceLineId(), line.getQuantity(), BigDecimal::add));
        }
        return result;
    }

    /** Lignes du bon sous forme de lignes de piece, pour le stock. */
    private static List<BillingDocumentLine> asLines(List<DeliveryNoteLine> lines) {
        return lines.stream().map(line -> BillingDocumentLine.builder().lineOrder(line.getLineOrder()).itemCode(line.getItemCode())
                .sourceType(line.getSourceType()).lineType(line.getLineType() == null ? null : BillingLineType.valueOf(line.getLineType()))
                .description(line.getDescription()).quantity(line.getQuantity()).optional(false).build()).toList();
    }

    private DeliveryNote note(String number) {
        return notes.findByNoteNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Bon de livraison introuvable : " + number));
    }

    private DeliveryNoteDtos.NoteSummary summary(DeliveryNote note) {
        return new DeliveryNoteDtos.NoteSummary(note.getNoteNumber(), note.getSourceDocumentNumber(), note.getSourceDocumentType(),
                note.getCustomerName(), note.getStatus(), note.getDeliveredAt(), note.getInvoiceNumber(), note.getCreatedAt());
    }

    DeliveryNoteDtos.NoteView view(DeliveryNote note, List<DeliveryNoteLine> lines) {
        return new DeliveryNoteDtos.NoteView(note.getNoteNumber(), note.getSourceDocumentNumber(), note.getSourceDocumentType(),
                note.getCustomerType(), note.getCustomerCode(), note.getCustomerName(), note.getStatus(), note.getDeliveryAddress(),
                note.getNotes(), Boolean.TRUE.equals(note.getShowPrices()), Boolean.TRUE.equals(note.getStockIssued()),
                note.getDeliveredAt(), note.getDeliveredBy(), note.getReceivedBy(), note.getSignedCopyReference(), note.getInvoiceNumber(),
                note.getCancelledAt(), note.getCancelReason(), note.getCreatedBy(), note.getCreatedAt(),
                lines.stream().map(line -> new DeliveryNoteDtos.NoteLine(line.getLineOrder(), line.getItemCode(), line.getDescription(),
                        line.getUnit(), line.getQuantity(), line.getUnitPrice())).toList());
    }

    private static String label(String status) {
        return switch (status) {
            case DRAFT -> "en brouillon";
            case DELIVERED -> "livré";
            case CANCELLED -> "annulé";
            case INVOICED -> "facturé";
            default -> status;
        };
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
