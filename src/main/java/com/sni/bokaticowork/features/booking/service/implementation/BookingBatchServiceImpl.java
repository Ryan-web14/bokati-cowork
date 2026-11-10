package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.BaseException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingBatchRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingBatch;
import com.sni.bokaticowork.features.booking.repository.BookingBatchRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingBatchService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingBillableBridge;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation multi-ressources.
 *
 * <p>Chaque couple ligne et membre passe par la creation ordinaire d'une reservation, avec tous
 * ses controles (disponibilite, politique, droits, tarif). Seule la facturation change : elle est
 * mise en attente le temps du lot, puis chaque membre recoit une seule facture pour l'ensemble de
 * ses reservations. Le lot passe en entier ou pas du tout · la transaction est annulee des qu'une
 * reservation est impossible, et la reponse liste tout ce qui bloque.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BookingBatchServiceImpl implements BookingBatchService {

    static final int MAX_BOOKINGS = 200;
    private static final DateTimeFormatter SLOT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BookingBatchRepository batchRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final BookingBillableBridge billableBridge;
    private final BookingMapper bookingMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemRepository billableItemRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingDocumentService billingDocumentService;

    /** Une reservation a creer · {@code label} la nomme dans les erreurs. */
    record Planned(CreateBookingRequest request, String label) {
    }

    /** Ce qui fait une facture : un titulaire, l'organisation qui paie, une devise. */
    record InvoiceKey(String subscriberType, String subscriberCode, String billingCompanyCode, String currency) {
        static InvoiceKey of(BillableItem item) {
            return new InvoiceKey(item.getSubscriberType() == null ? null : item.getSubscriberType().name(),
                    item.getSubscriberCode(), item.getBillingCompanyCode(), item.getCurrency());
        }
    }

    @Override
    public BookingBatchResponse create(CreateBookingBatchRequest request) {
        String idempotencyKey = StringUtils.hasText(request.idempotencyKey()) ? request.idempotencyKey().trim() : null;
        if (idempotencyKey != null) {
            BookingBatch existing = batchRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                return view(existing);
            }
        }
        if (request.paymentMode() == BookingPaymentMode.WALLET) {
            // Le portefeuille debite la facture de chaque reservation au moment de son blocage ; une
            // facture commune a plusieurs reservations le ferait payer plusieurs fois.
            throw new BadRequestException("Le portefeuille n'est pas pris en charge pour une réservation multi-ressources : "
                    + "choisissez DIRECT, SUBSCRIPTION ou PASS, ou réservez une par une.");
        }

        String batchNumber = sequenceGenerator.next("booking_batch");
        List<Planned> planned = plan(request, batchNumber);

        List<String> errors = new ArrayList<>();
        List<Booking> created = new ArrayList<>();
        List<BillableItem> items;
        billableBridge.beginDeferredInvoicing();
        try {
            for (Planned booking : planned) {
                try {
                    BookingResponse response = bookingService.create(booking.request());
                    Booking entity = bookingRepository.findByBookingNumber(response.bookingNumber())
                            .orElseThrow(() -> new IllegalStateException("Booking " + response.bookingNumber() + " not found after creation"));
                    entity.setBatchNumber(batchNumber);
                    created.add(bookingRepository.save(entity));
                } catch (BaseException ex) {
                    errors.add(booking.label() + " : " + ex.getMessage());
                }
            }
        } finally {
            items = billableBridge.endDeferredInvoicing();
        }
        if (!errors.isEmpty()) {
            // La transaction est annulee : aucune reservation, aucune facture du lot ne reste.
            throw new ValidationException("Réservation multi-ressources refusée : " + errors.size() + " réservation(s) sur "
                    + planned.size() + " impossible(s). Rien n'a été créé.", errors);
        }

        Set<String> members = new LinkedHashSet<>();
        created.forEach(booking -> members.add(booking.getOwnerCode()));
        BookingBatch batch = batchRepository.save(BookingBatch.builder()
                .batchNumber(batchNumber)
                .idempotencyKey(idempotencyKey)
                .paymentMode(request.paymentMode())
                .billingCompanyCode(trim(request.billingCompanyCode()))
                .bookingCount(created.size())
                .memberCount(members.size())
                .notes(trim(request.notes()))
                .createdBy(currentActor())
                .build());

        invoicePerMember(batchNumber, items, created);
        log.info("Lot {} : {} reservation(s) pour {} membre(s)", batchNumber, created.size(), members.size());
        return view(batch);
    }

    /**
     * Les couples ligne et membre, dans l'ordre de la demande. Les erreurs de forme (ligne sans
     * membre, lot trop grand) sont rendues avant toute creation.
     */
    List<Planned> plan(CreateBookingBatchRequest request, String batchNumber) {
        List<String> defaults = memberIds(request.memberIds());
        List<String> errors = new ArrayList<>();
        List<Planned> planned = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            CreateBookingBatchRequest.Line line = request.lines().get(i);
            List<String> lineMembers = memberIds(line.memberIds());
            List<String> members = lineMembers.isEmpty() ? defaults : lineMembers;
            String where = "Ligne " + (i + 1) + " (" + line.resourceCode().trim() + ", "
                    + SLOT.format(line.startedAt()) + " - " + SLOT.format(line.endedAt()) + ")";
            if (members.isEmpty()) {
                errors.add(where + " : aucun membre · renseignez memberIds sur la ligne ou sur le lot");
                continue;
            }
            for (String memberId : members) {
                planned.add(new Planned(new CreateBookingRequest(
                        line.resourceCode().trim(), memberId, null, null, null, null, null, null, null, null,
                        null, null, line.startedAt(), line.endedAt(), line.quantity(), request.paymentMode(),
                        request.confirmImmediately(), request.sendEmail(),
                        StringUtils.hasText(line.notes()) ? line.notes() : request.notes(),
                        null, line.bookingUnit(), line.participants(), trim(request.billingCompanyCode())),
                        where + ", membre " + memberId));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Réservation multi-ressources refusée : lignes sans membre. Rien n'a été créé.", errors);
        }
        if (planned.size() > MAX_BOOKINGS) {
            throw new BadRequestException("Un lot compte au plus " + MAX_BOOKINGS + " réservations (lignes x membres) ; celui-ci en compte "
                    + planned.size() + ".");
        }
        return planned;
    }

    /** Une facture par titulaire : les elements mis de cote pendant le lot, regroupes. */
    private void invoicePerMember(String batchNumber, List<BillableItem> items, List<Booking> bookings) {
        Map<InvoiceKey, List<BillableItem>> groups = new LinkedHashMap<>();
        for (BillableItem item : items) {
            groups.computeIfAbsent(InvoiceKey.of(item), key -> new ArrayList<>()).add(item);
        }
        Map<String, Booking> byNumber = new LinkedHashMap<>();
        bookings.forEach(booking -> byNumber.put(booking.getBookingNumber(), booking));
        for (List<BillableItem> group : groups.values()) {
            String holder = StringUtils.hasText(group.get(0).getSubscriberName())
                    ? group.get(0).getSubscriberName() : group.get(0).getSubscriberCode();
            List<String> resources = group.stream()
                    .map(item -> byNumber.get(item.getSourceId()))
                    .filter(Objects::nonNull)
                    .map(booking -> booking.getResource().getName())
                    .distinct()
                    .toList();
            String title = "Réservations " + batchNumber + " · " + holder;
            String description = group.size() + " réservation(s) : " + String.join(", ", resources);
            billableBridge.invoiceGroup(group, title, description);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingBatchResponse get(String batchNumber) {
        BookingBatch batch = batchRepository.findByBatchNumber(batchNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Lot de réservations introuvable : " + batchNumber));
        return view(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BookingBatchResponse.Summary> list(Pageable pageable) {
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PaginatedResponse<>(batchRepository.findAll(page).map(batch -> new BookingBatchResponse.Summary(
                batch.getBatchNumber(), batch.getPaymentMode(), batch.getBillingCompanyCode(), batch.getBookingCount(),
                batch.getMemberCount(), batch.getNotes(), batch.getCreatedBy(), batch.getCreatedAt())));
    }

    private BookingBatchResponse view(BookingBatch batch) {
        List<Booking> bookings = bookingRepository.findAllByBatchNumber(batch.getBatchNumber());
        Map<Long, List<String>> bookingsByInvoice = new LinkedHashMap<>();
        List<String> uninvoiced = new ArrayList<>();
        for (Booking booking : bookings) {
            Long invoiceId = StringUtils.hasText(booking.getBillableNumber())
                    ? billableItemRepository.findByBillableNumber(booking.getBillableNumber())
                    .map(BillableItem::getInvoiceId).orElse(null)
                    : null;
            if (invoiceId == null) {
                uninvoiced.add(booking.getBookingNumber());
            } else {
                bookingsByInvoice.computeIfAbsent(invoiceId, id -> new ArrayList<>()).add(booking.getBookingNumber());
            }
        }
        List<BookingBatchResponse.BatchInvoice> invoices = new ArrayList<>();
        bookingsByInvoice.forEach((invoiceId, numbers) -> billingDocumentRepository.findById(invoiceId)
                .map(document -> billingDocumentService.get(document.getDocumentNumber()))
                .ifPresent(invoice -> invoices.add(invoice(invoice, numbers))));
        return new BookingBatchResponse(
                batch.getBatchNumber(), batch.getPaymentMode(), batch.getBillingCompanyCode(), batch.getBookingCount(),
                batch.getMemberCount(), batch.getNotes(), batch.getCreatedBy(), batch.getCreatedAt(),
                invoices, uninvoiced, bookings.stream().map(bookingMapper::toResponse).toList());
    }

    private static BookingBatchResponse.BatchInvoice invoice(BillingDocumentResponse invoice, List<String> bookingNumbers) {
        return new BookingBatchResponse.BatchInvoice(invoice.documentNumber(), invoice.status(), invoice.customerType(),
                invoice.customerCode(), invoice.customerName(), invoice.totalAmount(), invoice.paidAmount(),
                invoice.balanceDue(), invoice.currency(), bookingNumbers);
    }

    private static List<String> memberIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
