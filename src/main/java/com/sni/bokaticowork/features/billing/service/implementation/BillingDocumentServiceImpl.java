package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentAdvanceRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.SimulateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateReservationInvoiceRequest;
import com.sni.bokaticowork.features.billing.dto.request.SelectQuoteOptionsRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentDiscountResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentVersionResponse;
import com.sni.bokaticowork.features.billing.dto.response.SimulateBillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceStatus;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentAdvance;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.billing.repository.specification.criteria.BillingDocumentSearchCriteria;
import com.sni.bokaticowork.features.billing.service.fiscal.DocumentSequenceService;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalAuditService;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalHashService;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalSignatureService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentVersionService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentWriter;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport;
import com.sni.bokaticowork.features.billing.service.support.BillingNumberingSupport;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingRecipientRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingDocumentServiceImpl implements BillingDocumentService {

    private final ObjectMapper objectMapper;
    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentLineRepository lineRepository;
    private final BillingDocumentAdvanceRepository advanceRepository;
    private final BillingDocumentDiscountRepository discountRepository;
    private final BillingDocumentTaxRepository taxRepository;
    private final BillingDocumentClauseRepository clauseRepository;
    private final BillingDocumentEditHistoryRepository editHistoryRepository;
    private final BillableItemRepository billableItemRepository;
    private final BookingRepository bookingRepository;
    private final BillingCalculationService calculationService;
    private final BillingDiscountGuard discountGuard;
    private final BillingDocumentVersionService versionService;
    private final WalletService walletService;
    private final WalletLedgerEntryRepository walletLedgerEntryRepository;

    /** Devise retenue quand la simulation n'en precise aucune · celle du catalogue. */
    @org.springframework.beans.factory.annotation.Value("${bokati.billing.default-currency:XAF}")
    private String defaultCurrency;
    private final BillingCustomerSnapshotResolver customerSnapshotResolver;
    private final BillingNumberingSupport numberingSupport;
    private final BillingDocumentWriter documentWriter;
    private final BillingLifecycleSupport lifecycleSupport;
    private final BillingEventWriter eventWriter;
    private final BillingDocumentMapper mapper;
    private final PaymentIntentRepository intentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final DocumentSequenceService documentSequenceService;
    private final FiscalAuditService fiscalAuditService;
    private final FiscalHashService fiscalHashService;
    private final FiscalSignatureService fiscalSignatureService;
    private final MemberInAppNotifier memberInAppNotifier;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public SimulateBillingDocumentResponse simulate(SimulateBillingDocumentRequest request) {
        BillingCalculationService.CalculatedDocument calculation =
                calculationService.calculate(request.lines(), request.resolvedDiscounts());

        List<SimulateBillingDocumentResponse.SimulatedLine> lines = calculation.lines().stream()
                .map(this::simulatedLine)
                .toList();

        // La remise document n'est pas exposee separement par le calcul · elle se deduit de
        // l'ecart entre la remise totale et la somme des remises portees par les lignes.
        BigDecimal lineDiscount = calculation.lines().stream()
                .map(line -> line.getDiscountAmount() == null ? BigDecimal.ZERO : line.getDiscountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal documentDiscount = calculation.discountAmount().subtract(lineDiscount).max(BigDecimal.ZERO);

        // La simulation constate mais ne refuse jamais · c'est l'emission qui bloque.
        List<BillingDiscountGuard.DiscountViolation> violations = discountGuard.evaluate(
                calculation.lines(), calculation.subtotalAmount(), calculation.discountAmount());
        boolean blocked = violations.stream()
                .anyMatch(violation -> BillingDiscountGuard.Severity.BLOCK.name().equals(violation.severity()));

        return new SimulateBillingDocumentResponse(
                request.resolvedDocumentType(),
                request.resolvedCurrency(defaultCurrency),
                request.title(),
                lines,
                calculation.subtotalAmount(),
                lineDiscount,
                documentDiscount,
                calculation.discountAmount(),
                calculation.taxableAmount(),
                calculation.vatAmount(),
                calculation.additionalCentAmount(),
                calculation.taxAmount(),
                calculation.totalAmount(),
                violations,
                blocked);
    }

    /** Auteur d'une derogation · le nom du principal, ou SYSTEM hors contexte authentifie. */
    private String currentUserName() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication == null || !StringUtils.hasText(authentication.getName())
                ? "SYSTEM"
                : authentication.getName();
    }

    private SimulateBillingDocumentResponse.SimulatedLine simulatedLine(BillingDocumentLine line) {
        BigDecimal gross = line.getSubtotalAmount() == null ? BigDecimal.ZERO : line.getSubtotalAmount();
        BigDecimal discount = line.getDiscountAmount() == null ? BigDecimal.ZERO : line.getDiscountAmount();
        BigDecimal net = gross.subtract(discount);
        // Le taux effectif est recalcule meme lorsque la remise a ete saisie en montant : c'est
        // precisement le chiffre que l'on cherchait a la calculatrice.
        BigDecimal effectiveRate = gross.signum() == 0
                ? BigDecimal.ZERO
                : discount.multiply(BigDecimal.valueOf(100)).divide(gross, 4, RoundingMode.HALF_UP);

        return new SimulateBillingDocumentResponse.SimulatedLine(
                line.getItemCode(),
                line.getDescription(),
                line.getCategory(),
                line.getUnit(),
                line.getQuantity(),
                line.getUnitPrice(),
                gross,
                line.getDiscountRate(),
                discount,
                effectiveRate,
                net,
                line.getTaxAmount(),
                line.getTotalAmount(),
                line.getTaxable(),
                line.getOptional());
    }

    @Override
    public BillingDocumentResponse create(CreateBillingDocumentRequest request) {
        BillingCustomerSnapshotResolver.CustomerSnapshot customer = customerSnapshotResolver.resolve(request);
        BillingCalculationService.CalculatedDocument calculation = calculationService.calculate(request.lines(), request.discounts());
        BillingDocument document = buildDocument(request, customer, calculation);
        BillingDocument saved = documentWriter.save(document, calculation, request.discounts(), request.clauses(), request.advance());
        documentWriter.saveEarlyPaymentDiscount(saved, request.earlyPaymentDiscount());
        eventWriter.write(saved, "BILLING_DOCUMENT_CREATED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse update(String documentNumber, UpdateBillingDocumentRequest request) {
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureNotLocked(document);
        boolean isDraft = document.getStatus() == BillingDocumentStatus.DRAFT;
        boolean isRestrictedEdit = document.getStatus() == BillingDocumentStatus.ISSUED
                || document.getStatus() == BillingDocumentStatus.SENT;

        if (!isDraft && !isRestrictedEdit) {
            throw new BadRequestException("Document in status " + document.getStatus() + " cannot be modified");
        }

        // L'etat precedent est archive AVANT toute modification · c'est lui qui permet de
        // reconstituer ce que le client avait sous les yeux. Le document ne s'ecrase plus.
        versionService.archive(document,
                lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document),
                isDraft ? "FULL_EDIT" : "RESTRICTED_EDIT");

        // Internal notes and restricted fields are always editable when status allows modification
        if (StringUtils.hasText(request.internalNotes())) {
            document.setInternalNotes(request.internalNotes().trim());
        } else if (request.internalNotes() != null) {
            document.setInternalNotes(null);
        }
        if (request.dueDate() != null) {
            document.setDueDate(request.dueDate());
        }
        if (StringUtils.hasText(request.terms())) {
            document.setTerms(request.terms().trim());
        }
        if (StringUtils.hasText(request.paymentInstructions())) {
            document.setPaymentInstructions(request.paymentInstructions().trim());
        }

        if (isDraft) {
            if (StringUtils.hasText(request.title())) document.setTitle(request.title().trim());
            if (StringUtils.hasText(request.description())) document.setDescription(request.description().trim());
            if (request.issueDate() != null) document.setIssueDate(request.issueDate());
            if (StringUtils.hasText(request.customerReference())) document.setCustomerReference(request.customerReference().trim());
            if (StringUtils.hasText(request.poNumber())) document.setPoNumber(request.poNumber().trim());
            if (StringUtils.hasText(request.projectCode())) document.setProjectCode(request.projectCode().trim());
            if (StringUtils.hasText(request.salespersonCode())) document.setSalespersonCode(request.salespersonCode().trim());
            if (StringUtils.hasText(request.deliveryAddressJson())) document.setDeliveryAddressJson(request.deliveryAddressJson().trim());
            if (StringUtils.hasText(request.language())) document.setLanguage(request.language().trim());
            if (request.exchangeRate() != null) document.setExchangeRate(request.exchangeRate());
            if (StringUtils.hasText(request.paymentReference())) document.setPaymentReference(request.paymentReference().trim());
            if (StringUtils.hasText(request.bankDetailsJson())) document.setBankDetailsJson(request.bankDetailsJson().trim());

            applyLineUpdates(document, request.lines());

            if (request.discounts() != null) {
                discountRepository.deleteAllByDocument(document);
            }
            if (request.clauses() != null) {
                clauseRepository.deleteAllByDocument(document);
            }

            // Recalculate totals from remaining lines
            List<BillingDocumentLine> remainingLines = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document);
            if (!remainingLines.isEmpty()) {
                List<CreateBillingDocumentLineRequest> lineRequests = remainingLines.stream()
                        .map(this::toCreateLineRequest).toList();
                BillingCalculationService.CalculatedDocument recalc = calculationService.calculate(
                        lineRequests,
                        request.discounts() != null ? request.discounts() : List.of()
                );
                document.setSubtotalAmount(recalc.subtotalAmount());
                document.setDiscountAmount(recalc.discountAmount());
                document.setTaxableAmount(recalc.taxableAmount());
                document.setVatAmount(recalc.vatAmount());
                document.setAdditionalCentAmount(recalc.additionalCentAmount());
                document.setTaxAmount(recalc.taxAmount());
                document.setTotalAmount(recalc.totalAmount());
                document.setBalanceDue(recalc.totalAmount().subtract(document.getPaidAmount()));

                // Rebuild discounts, clauses and taxes
                if (request.discounts() != null) {
                    documentWriter.save(document, recalc, request.discounts(), List.of(), null);
                }
                if (request.clauses() != null) {
                    taxRepository.deleteAllByDocument(document);
                    documentWriter.save(document, recalc, List.of(), request.clauses(), null);
                }
            }
        }

        BillingDocument saved = documentRepository.save(document);
        eventWriter.write(saved, "BILLING_DOCUMENT_UPDATED", null);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingDocumentVersionResponse> versions(String documentNumber) {
        return versionService.versions(serviceByNumber(documentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocumentVersionResponse version(String documentNumber, Integer versionNumber) {
        return versionService.version(serviceByNumber(documentNumber), versionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocumentVersionResponse.Diff versionDiff(String documentNumber, Integer from, Integer to) {
        return versionService.diff(serviceByNumber(documentNumber), from, to);
    }

    @Override
    public BillingDocumentResponse updateRecipient(String documentNumber, UpdateBillingRecipientRequest request) {
        if (request == null) {
            throw new BadRequestException("Recipient payload is required");
        }
        BillingDocument document = serviceByNumber(documentNumber);
        ensureRecipientChangeAllowed(document, request);

        Map<String, Object> before = recipientSnapshot(document);

        applyIfPresent(request.customerName(), document::setCustomerName);
        applyIfPresent(request.customerEmail(), document::setCustomerEmail);
        applyIfPresent(request.customerPhone(), document::setCustomerPhone);
        applyIfPresent(request.customerNiu(), document::setCustomerNiu);
        applyIfPresent(request.customerCategory(), document::setCustomerCategory);
        applyIfPresent(request.customerReference(), document::setCustomerReference);
        applyIfPresent(request.billingAddressJson(), document::setBillingAddressJson);
        applyIfPresent(request.deliveryAddressJson(), document::setDeliveryAddressJson);

        BillingDocument saved = documentRepository.save(document);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("before", before);
        auditPayload.put("after", recipientSnapshot(saved));
        auditPayload.put("reason", request.reason());
        editHistoryRepository.save(BillingDocumentEditHistory.builder()
                .document(saved)
                .editType("RECIPIENT_UPDATED")
                .changedBy(StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "SYSTEM")
                .changedAt(Instant.now())
                .snapshotJson(writeJson(auditPayload))
                .build());
        eventWriter.write(saved, "BILLING_DOCUMENT_RECIPIENT_UPDATED", Map.of("reason", String.valueOf(request.reason())));
        return mapper.toResponse(saved);
    }

    /**
     * Sur un document scelle, seules les coordonnees de contact restent corrigeables.
     * <p>
     * L'identite legale du destinataire - raison sociale, NIU, adresse de facturation - est
     * protegee par le declencheur {@code trg_billing_document_immutable}, qui est plus strict
     * que la chaine de hachage fiscale : celle-ci ne couvre que {@code customer_code}, mais la
     * base considere que l'identite imprimee fait partie du document emis. Corriger une raison
     * sociale sur une facture validee passe donc par un avoir ou une rectificative.
     * <p>
     * Sans ce controle, la contrainte remontait en {@code JpaSystemException} au flush, donc en
     * 500 opaque cote client au lieu d'une erreur metier exploitable.
     */
    private void ensureRecipientChangeAllowed(BillingDocument document, UpdateBillingRecipientRequest request) {
        if (!Boolean.TRUE.equals(document.getLocked())) {
            return;
        }
        List<String> blocked = new ArrayList<>();
        if (changes(request.customerName(), document.getCustomerName())) {
            blocked.add("customerName");
        }
        if (changes(request.customerNiu(), document.getCustomerNiu())) {
            blocked.add("customerNiu");
        }
        if (changes(request.billingAddressJson(), document.getBillingAddressJson())) {
            blocked.add("billingAddressJson");
        }
        if (!blocked.isEmpty()) {
            throw new BadRequestException(
                    "Le document " + document.getDocumentNumber() + " est validé : "
                            + String.join(", ", blocked)
                            + " ne peuvent plus être modifiés. Émettez un avoir ou une facture rectificative. "
                            + "Les coordonnées de contact (email, téléphone, catégorie, référence, adresse de livraison) "
                            + "restent corrigeables.");
        }
    }

    /** Un champ absent de la requete ou identique a la valeur en place n'est pas une modification. */
    private boolean changes(String requested, String current) {
        if (requested == null) {
            return false;
        }
        String normalized = requested.isBlank() ? null : requested.trim();
        return !java.util.Objects.equals(normalized, current);
    }

    /**
     * {@code null} laisse la valeur en place, une chaine vide la vide : sans cette distinction,
     * il serait impossible d'effacer un NIU ou un telephone errone.
     */
    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value == null) {
            return;
        }
        setter.accept(value.isBlank() ? null : value.trim());
    }

    private Map<String, Object> recipientSnapshot(BillingDocument document) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("customerName", document.getCustomerName());
        snapshot.put("customerEmail", document.getCustomerEmail());
        snapshot.put("customerPhone", document.getCustomerPhone());
        snapshot.put("customerNiu", document.getCustomerNiu());
        snapshot.put("customerCategory", document.getCustomerCategory());
        snapshot.put("customerReference", document.getCustomerReference());
        snapshot.put("billingAddressJson", document.getBillingAddressJson());
        snapshot.put("deliveryAddressJson", document.getDeliveryAddressJson());
        return snapshot;
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            // L'historique ne doit jamais faire echouer la correction elle-meme.
            log.warn("Impossible de serialiser l'historique de correction destinataire", ex);
            return null;
        }
    }

    private void applyLineUpdates(BillingDocument document, List<UpdateBillingDocumentLineRequest> lineUpdates) {
        if (lineUpdates == null || lineUpdates.isEmpty()) {
            return;
        }
        List<BillingDocumentLine> existing = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document);
        for (UpdateBillingDocumentLineRequest upd : lineUpdates) {
            if ("REMOVE".equalsIgnoreCase(upd.action())) {
                existing.stream()
                        .filter(l -> l.getLineOrder().equals(upd.lineOrder()))
                        .findFirst()
                        .ifPresent(lineRepository::delete);
            } else {
                // UPSERT
                BillingDocumentLine line = existing.stream()
                        .filter(l -> l.getLineOrder().equals(upd.lineOrder()))
                        .findFirst()
                        .orElse(null);
                CreateBillingDocumentLineRequest lineReq = toUpdateLineAsCreate(upd);
                BillingCalculationService.CalculatedDocument singleCalc =
                        calculationService.calculate(List.of(lineReq), List.of());
                BillingDocumentLine calculated = singleCalc.lines().get(0);
                if (line == null) {
                    calculated.setDocument(document);
                    lineRepository.save(calculated);
                } else {
                    line.setLineType(calculated.getLineType());
                    line.setItemCode(calculated.getItemCode());
                    line.setCategory(calculated.getCategory());
                    line.setDescription(calculated.getDescription());
                    line.setDetailedDescription(calculated.getDetailedDescription());
                    line.setQuantity(calculated.getQuantity());
                    line.setUnit(calculated.getUnit());
                    line.setUnitPrice(calculated.getUnitPrice());
                    line.setDiscountRate(calculated.getDiscountRate());
                    line.setDiscountAmount(calculated.getDiscountAmount());
                    line.setTaxable(calculated.getTaxable());
                    line.setTaxIncluded(calculated.getTaxIncluded());
                    line.setVatRate(calculated.getVatRate());
                    line.setAdditionalCentRate(calculated.getAdditionalCentRate());
                    line.setSubtotalAmount(calculated.getSubtotalAmount());
                    line.setTaxableAmount(calculated.getTaxableAmount());
                    line.setVatAmount(calculated.getVatAmount());
                    line.setAdditionalCentAmount(calculated.getAdditionalCentAmount());
                    line.setTaxAmount(calculated.getTaxAmount());
                    line.setTotalAmount(calculated.getTotalAmount());
                    line.setSourceType(calculated.getSourceType());
                    line.setSourceCode(calculated.getSourceCode());
                    line.setExternalReference(calculated.getExternalReference());
                    line.setNotes(calculated.getNotes());
                    line.setOptional(calculated.getOptional());
                    lineRepository.save(line);
                }
            }
        }
    }

    private CreateBillingDocumentLineRequest toUpdateLineAsCreate(UpdateBillingDocumentLineRequest upd) {
        return new CreateBillingDocumentLineRequest(
                upd.lineOrder(),
                upd.lineType(),
                upd.itemCode(),
                upd.description(),
                upd.detailedDescription(),
                upd.quantity(),
                upd.unitPrice(),
                upd.discountRate(),
                upd.discountAmount(),
                upd.taxable(),
                upd.taxIncluded(),
                upd.vatRate(),
                upd.additionalCentRate(),
                upd.sourceType(),
                upd.sourceCode(),
                upd.unit(),
                upd.externalReference(),
                upd.notes(),
                upd.optional(),
                upd.category()
        );
    }

    @Override
    public BillingDocumentResponse createManualInvoice(CreateManualBillingDocumentRequest request) {
        return create(toDocumentRequest(BillingDocumentType.INVOICE, request));
    }

    @Override
    public BillingDocumentResponse createManualQuote(CreateManualBillingDocumentRequest request) {
        return create(toDocumentRequest(BillingDocumentType.QUOTE, request));
    }

    @Override
    public BillingDocumentResponse createManualQuotation(CreateManualBillingDocumentRequest request) {
        return createManualQuote(request);
    }

    @Override
    public BillingDocumentResponse createInvoiceFromBillableItems(CreateInvoiceFromBillableItemsRequest request) {
        List<BillableItem> billableItems = resolveBillableItems(request.billableNumbers());
        BillableItem firstItem = billableItems.get(0);
        String currency = billableItems.get(0).getCurrency();
        List<CreateBillingDocumentLineRequest> lines = billableItems.stream()
                .map(item -> new CreateBillingDocumentLineRequest(
                        null,
                        lineTypeFromBillable(item),
                        item.getBillableNumber(),
                        item.getDescription(),
                        item.getMetadataJson(),
                        BigDecimal.ONE,
                        item.getAmount(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        null,
                        item.getSourceType(),
                        item.getSourceId(),
                        null, null, null, null, null
                ))
                .toList();
        CreateBillingDocumentRequest createRequest = new CreateBillingDocumentRequest(
                BillingDocumentType.INVOICE,
                firstItem.getSubscriberType().name(),
                firstItem.getSubscriberCode(),
                firstItem.getSubscriberName(),
                firstItem.getSubscriberEmail(),
                firstItem.getSubscriberPhone(),
                null,
                "BILLABLE_ITEM",
                String.join(",", request.billableNumbers()),
                request.title(),
                request.description(),
                null,
                currency,
                request.issueDate(),
                request.dueDate(),
                null,
                lines,
                List.of(),
                defaultInvoiceClauses(),
                null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        BillingDocumentResponse response = create(createRequest);
        BillingDocument invoice = serviceByNumber(response.documentNumber());
        billableItems.forEach(item -> {
            item.setInvoiceId(invoice.getId());
            item.setStatus(BillableItemStatus.INVOICED);
            billableItemRepository.save(item);
        });
        return mapper.toResponse(invoice);
    }

    @Override
    public BillingDocumentResponse createInvoiceFromReservation(CreateReservationInvoiceRequest request) {
        if ("BOOKING".equalsIgnoreCase(request.mode())) {
            return createInvoiceFromExistingBooking(request);
        }
        return createInvoiceFromExternalBooking(request);
    }

    private BillingDocumentResponse createInvoiceFromExistingBooking(CreateReservationInvoiceRequest request) {
        if (!StringUtils.hasText(request.bookingNumber())) {
            throw new BadRequestException("bookingNumber is required for mode BOOKING");
        }
        Booking booking = bookingRepository.findPublicByBookingNumberWithResource(request.bookingNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.bookingNumber()));

        if (StringUtils.hasText(booking.getBillableNumber())) {
            throw new BadRequestException("Booking " + booking.getBookingNumber() + " is already billed: " + booking.getBillableNumber());
        }

        var bookingStatuses = Set.of(
                com.sni.bokaticowork.features.booking.enums.BookingStatus.CONFIRMED,
                com.sni.bokaticowork.features.booking.enums.BookingStatus.IN_PROGRESS,
                com.sni.bokaticowork.features.booking.enums.BookingStatus.COMPLETED
        );
        if (!bookingStatuses.contains(booking.getStatus())) {
            throw new BadRequestException("Booking status " + booking.getStatus() + " is not billable");
        }

        String currency = StringUtils.hasText(booking.getCurrency()) ? booking.getCurrency() : "XAF";
        BigDecimal unitPrice = booking.getUnitPrice() != null ? booking.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.valueOf(booking.getQuantity() != null ? booking.getQuantity() : 1);
        String unit = booking.getBookingUnit() != null ? booking.getBookingUnit().name() : null;
        String resourceName = booking.getResource() != null ? booking.getResource().getName() : "Ressource";
        String resourceDesc = booking.getResource() != null ? booking.getResource().getDescription() : null;
        String period = (booking.getStartedAt() != null && booking.getEndedAt() != null)
                ? booking.getStartedAt() + " → " + booking.getEndedAt()
                : null;
        String lineDesc = resourceName + (period != null ? " · " + period : "");

        List<CreateBillingDocumentLineRequest> lines = new ArrayList<>();
        lines.add(new CreateBillingDocumentLineRequest(
                1,
                BillingLineType.BOOKING,
                booking.getBookingNumber(),
                lineDesc,
                resourceDesc,
                quantity,
                unitPrice,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Boolean.TRUE,
                Boolean.FALSE,
                null,
                null,
                "BOOKING",
                booking.getBookingNumber(),
                unit,
                null,
                booking.getNotes(),
                null,
                "Réservation"
        ));
        if (request.extraLines() != null) {
            int order = 2;
            for (CreateBillingDocumentLineRequest extra : request.extraLines()) {
                lines.add(new CreateBillingDocumentLineRequest(
                        order++,
                        extra.lineType(),
                        extra.itemCode(),
                        extra.description(),
                        extra.detailedDescription(),
                        extra.quantity(),
                        extra.unitPrice(),
                        extra.discountRate(),
                        extra.discountAmount(),
                        extra.taxable(),
                        extra.taxIncluded(),
                        extra.vatRate(),
                        extra.additionalCentRate(),
                        extra.sourceType(),
                        extra.sourceCode(),
                        extra.unit(),
                        extra.externalReference(),
                        extra.notes(),
                        extra.optional(),
                        extra.category()
                ));
            }
        }

        BillingDocumentType docType = request.documentType() != null ? request.documentType() : BillingDocumentType.INVOICE;
        CreateBillingDocumentRequest createRequest = new CreateBillingDocumentRequest(
                docType,
                booking.getOwnerType().name(),
                booking.getOwnerCode(),
                StringUtils.hasText(booking.getContactName()) ? booking.getContactName() : booking.getOwnerCode(),
                booking.getContactEmail(),
                booking.getContactPhone(),
                null,
                "BOOKING",
                booking.getBookingNumber(),
                "Facture réservation · " + booking.getBookingNumber(),
                trim(request.description()),
                trim(request.terms()),
                currency,
                request.issueDate() != null ? request.issueDate() : LocalDate.now(),
                request.dueDate(),
                null,
                lines,
                List.of(),
                defaultInvoiceClauses(),
                null,
                trim(request.paymentInstructions()),
                null,
                null,
                null, null, null, null, null, null, null, null
        );

        BillingDocumentResponse response = create(createRequest);

        // Apply internalNotes if provided
        if (StringUtils.hasText(request.internalNotes())) {
            BillingDocument doc = serviceByNumber(response.documentNumber());
            doc.setInternalNotes(request.internalNotes().trim());
            documentRepository.save(doc);
        }

        // Link booking → billing document
        booking.setBillableNumber(response.documentNumber());
        bookingRepository.save(booking);

        return get(response.documentNumber());
    }

    private BillingDocumentResponse createInvoiceFromExternalBooking(CreateReservationInvoiceRequest request) {
        if (request.externalBooking() == null) {
            throw new BadRequestException("externalBooking details are required for mode EXTERNAL");
        }
        if (!StringUtils.hasText(request.customerType()) || !StringUtils.hasText(request.customerCode())) {
            throw new BadRequestException("customerType and customerCode are required for mode EXTERNAL");
        }

        var ext = request.externalBooking();
        String period = (ext.checkInAt() != null && ext.checkOutAt() != null)
                ? ext.checkInAt() + " → " + ext.checkOutAt()
                : null;
        String lineDesc = ext.resourceName() + (period != null ? " · " + period : "");

        CreateBillingDocumentLineRequest line = new CreateBillingDocumentLineRequest(
                1,
                BillingLineType.BOOKING,
                null,
                lineDesc,
                ext.resourceDescription(),
                ext.quantity() != null ? ext.quantity() : BigDecimal.ONE,
                ext.unitPrice(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Boolean.TRUE,
                Boolean.FALSE,
                null,
                null,
                "EXTERNAL_BOOKING",
                trim(ext.externalReference()),
                ext.bookingUnit() != null ? ext.bookingUnit().name() : null,
                trim(ext.externalReference()),
                trim(ext.notes()),
                null,
                "Réservation"
        );

        List<CreateBillingDocumentLineRequest> lines = new ArrayList<>();
        lines.add(line);
        if (request.extraLines() != null) {
            int order = 2;
            for (CreateBillingDocumentLineRequest extra : request.extraLines()) {
                lines.add(new CreateBillingDocumentLineRequest(
                        order++, extra.lineType(), extra.itemCode(), extra.description(),
                        extra.detailedDescription(), extra.quantity(), extra.unitPrice(),
                        extra.discountRate(), extra.discountAmount(), extra.taxable(), extra.taxIncluded(),
                        extra.vatRate(), extra.additionalCentRate(), extra.sourceType(), extra.sourceCode(),
                        extra.unit(), extra.externalReference(), extra.notes(), extra.optional(), extra.category()
                ));
            }
        }

        String currency = StringUtils.hasText(request.currency()) ? request.currency() : "XAF";
        BillingDocumentType docType = request.documentType() != null ? request.documentType() : BillingDocumentType.INVOICE;
        CreateBillingDocumentRequest createRequest = new CreateBillingDocumentRequest(
                docType,
                request.customerType(),
                request.customerCode(),
                null, null, null, null,
                "EXTERNAL_BOOKING",
                trim(ext.externalReference()),
                "Facture réservation externe · " + ext.resourceName(),
                null,
                trim(request.terms()),
                currency,
                request.issueDate() != null ? request.issueDate() : LocalDate.now(),
                request.dueDate(),
                null,
                lines,
                List.of(),
                defaultInvoiceClauses(),
                null, trim(request.paymentInstructions()), null, null,
                null, null, null, null, null, null, null, null
        );

        BillingDocumentResponse response = create(createRequest);

        if (StringUtils.hasText(request.internalNotes())) {
            BillingDocument doc = serviceByNumber(response.documentNumber());
            doc.setInternalNotes(request.internalNotes().trim());
            documentRepository.save(doc);
        }

        return get(response.documentNumber());
    }

    @Override
    public BillingDocumentResponse duplicate(String documentNumber) {
        BillingDocument source = serviceByNumber(documentNumber);
        List<BillingDocumentLine> sourceLines = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(source);
        if (sourceLines.isEmpty()) {
            throw new BadRequestException("Cannot duplicate a document with no lines");
        }
        List<CreateBillingDocumentLineRequest> lines = sourceLines.stream()
                .map(this::toCreateLineRequest)
                .toList();
        List<CreateBillingDocumentDiscountRequest> discounts = discountRepository.findAllByDocumentOrderByIdAsc(source)
                .stream()
                .map(d -> new CreateBillingDocumentDiscountRequest(d.getDiscountCode(), d.getDescription(), d.getDiscountType(), d.getValue()))
                .toList();
        List<CreateBillingDocumentClauseRequest> clauses = clauseRepository.findAllByDocumentOrderByDisplayOrderAscIdAsc(source)
                .stream()
                .map(c -> new CreateBillingDocumentClauseRequest(c.getClauseCode(), c.getTitle(), c.getBody(), c.getDisplayOrder()))
                .toList();
        CreateBillingDocumentRequest request = new CreateBillingDocumentRequest(
                source.getDocumentType(),
                source.getCustomerType(),
                source.getCustomerCode(),
                source.getCustomerName(),
                source.getCustomerEmail(),
                source.getCustomerPhone(),
                source.getBillingAddressJson(),
                source.getSourceType(),
                source.getSourceCode(),
                source.getTitle(),
                source.getDescription(),
                source.getTerms(),
                source.getCurrency(),
                LocalDate.now(),
                source.getDueDate(),
                source.getMetadataJson(),
                lines,
                discounts,
                clauses,
                source.getPaymentReference(),
                source.getPaymentInstructions(),
                source.getBankDetailsJson(),
                null,
                source.getCustomerReference(),
                source.getPoNumber(),
                source.getProjectCode(),
                source.getSalespersonCode(),
                source.getDeliveryAddressJson(),
                source.getLanguage(),
                source.getExchangeRate(),
                null
        );
        BillingDocumentResponse duplicated = create(request);
        if (StringUtils.hasText(source.getInternalNotes())) {
            BillingDocument doc = serviceByNumber(duplicated.documentNumber());
            doc.setInternalNotes("[Copie de " + source.getDocumentNumber() + "] " + source.getInternalNotes());
            documentRepository.save(doc);
        }
        eventWriter.write(serviceByNumber(duplicated.documentNumber()), "BILLING_DOCUMENT_DUPLICATED",
                java.util.Map.of("sourceDocumentNumber", source.getDocumentNumber()));
        return get(duplicated.documentNumber());
    }

    @Override
    public BillingDocumentResponse issue(String documentNumber) {
        return issue(documentNumber, null);
    }

    @Override
    public BillingDocumentResponse issue(String documentNumber, String discountOverrideReason) {
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanIssue(document);

        // Les limites de remise ne bloquent qu'ici, jamais au brouillon : le commercial
        // construit son offre librement, on l'arrete avant qu'elle n'engage.
        String override = discountGuard.enforce(
                discountGuard.evaluate(
                        lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document),
                        document.getSubtotalAmount(),
                        document.getDiscountAmount()),
                discountOverrideReason);
        if (override != null) {
            document.setDiscountOverrideReason(override);
            document.setDiscountOverrideBy(currentUserName());
            document.setDiscountOverrideAt(Instant.now());
            fiscalAuditService.log("DISCOUNT_OVERRIDE", "BILLING_DOCUMENT", document.getDocumentNumber(),
                    null, "reason=" + override);
        }

        document.setStatus(document.getDocumentType() == BillingDocumentType.QUOTE ? BillingDocumentStatus.SENT : BillingDocumentStatus.ISSUED);
        document.setIssuedAt(Instant.now());
        BillingDocument saved = documentRepository.save(document);
        eventWriter.write(saved, "BILLING_DOCUMENT_ISSUED", null);
        memberInAppNotifier.notify("BILLING_DOCUMENT_ISSUED", "BILLING_DOCUMENT", saved.getDocumentNumber(),
                saved.getCustomerEmail(), saved.getCustomerName(), saved.getCustomerCode(),
                "Facture emise " + saved.getDocumentNumber(),
                java.util.Map.of("documentNumber", saved.getDocumentNumber(),
                        "documentType", saved.getDocumentType() != null ? saved.getDocumentType().name() : "",
                        "status", saved.getStatus() != null ? saved.getStatus().name() : ""));
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse send(String documentNumber) {
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanSend(document);
        document.setStatus(document.getDocumentType() == BillingDocumentType.QUOTE ? BillingDocumentStatus.SENT : BillingDocumentStatus.SENT);
        document.setSentAt(Instant.now());
        BillingDocument saved = documentRepository.save(document);
        // Sans horodatage de transmission, rien ne distingue un brouillon retouche d'une
        // proposition reellement envoyee · seules les versions transmises ont valeur probante.
        versionService.markLastVersionSent(saved);
        eventWriter.write(saved, "BILLING_DOCUMENT_SENT", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse selectQuoteOptions(String quoteNumber, SelectQuoteOptionsRequest request) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() == BillingDocumentStatus.CONVERTED || quote.getStatus() == BillingDocumentStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify options on a converted or cancelled quote");
        }
        Set<Integer> selected = request.selectedLineOrders() == null ? Set.of() : Set.copyOf(request.selectedLineOrders());
        List<BillingDocumentLine> allLines = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(quote);

        for (BillingDocumentLine line : allLines) {
            if (!Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            if (selected.contains(line.getLineOrder())) {
                line.setOptional(Boolean.FALSE);
                lineRepository.save(line);
            } else {
                lineRepository.delete(line);
            }
        }

        // Recalculate document totals from remaining non-optional lines
        List<BillingDocumentLine> remaining = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(quote)
                .stream().filter(l -> !Boolean.TRUE.equals(l.getOptional())).toList();
        BigDecimal subtotal = sum(remaining, BillingDocumentLine::getSubtotalAmount);
        BigDecimal discount = sum(remaining, BillingDocumentLine::getDiscountAmount);
        BigDecimal taxable  = sum(remaining, BillingDocumentLine::getTaxableAmount);
        BigDecimal vat      = sum(remaining, BillingDocumentLine::getVatAmount);
        BigDecimal addCent  = sum(remaining, BillingDocumentLine::getAdditionalCentAmount);
        BigDecimal tax      = sum(remaining, BillingDocumentLine::getTaxAmount);
        BigDecimal total    = sum(remaining, BillingDocumentLine::getTotalAmount);

        quote.setSubtotalAmount(scale(subtotal));
        quote.setDiscountAmount(scale(discount));
        quote.setTaxableAmount(scale(taxable));
        quote.setVatAmount(scale(vat));
        quote.setAdditionalCentAmount(scale(addCent));
        quote.setTaxAmount(scale(tax));
        quote.setTotalAmount(scale(total));
        quote.setBalanceDue(scale(total.subtract(quote.getPaidAmount())));
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_OPTIONS_SELECTED", java.util.Map.of("selectedLineOrders", selected.toString()));
        return mapper.toResponse(saved);
    }

    private BigDecimal sum(List<BillingDocumentLine> lines, java.util.function.Function<BillingDocumentLine, BigDecimal> getter) {
        return lines.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public BillingDocumentResponse markViewed(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() == BillingDocumentStatus.SENT) {
            quote.setStatus(BillingDocumentStatus.VIEWED);
            BillingDocument saved = documentRepository.save(quote);
            eventWriter.write(saved, "QUOTE_VIEWED", null);
            return mapper.toResponse(saved);
        }
        return mapper.toResponse(quote);
    }

    @Override
    public BillingDocumentResponse startNegotiation(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() != BillingDocumentStatus.SENT
                && quote.getStatus() != BillingDocumentStatus.VIEWED
                && quote.getStatus() != BillingDocumentStatus.ACCEPTED) {
            throw new BadRequestException("Cannot start negotiation in status " + quote.getStatus());
        }
        quote.setStatus(BillingDocumentStatus.NEGOTIATION);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_NEGOTIATION_STARTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse requestDeposit(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() != BillingDocumentStatus.ACCEPTED) {
            throw new BadRequestException("Deposit can only be requested on an accepted quote");
        }
        quote.setStatus(BillingDocumentStatus.DEPOSIT_REQUESTED);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_DEPOSIT_REQUESTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse markDepositPaid(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() != BillingDocumentStatus.DEPOSIT_REQUESTED) {
            throw new BadRequestException("Quote must be in DEPOSIT_REQUESTED status to mark deposit as paid");
        }
        quote.setStatus(BillingDocumentStatus.DEPOSIT_PAID);
        BillingDocument saved = documentRepository.save(quote);
        advanceRepository.findByDocument(saved).ifPresent(adv -> {
            adv.setStatus(BillingAdvanceStatus.PAID);
            adv.setPaidAt(Instant.now());
            createDepositPaymentRecord(saved, adv.getComputedAmount());
            advanceRepository.save(adv);
        });
        eventWriter.write(saved, "QUOTE_DEPOSIT_PAID", null);
        return mapper.toResponse(saved);
    }

    private void createDepositPaymentRecord(BillingDocument quote, BigDecimal amount) {
        String customerCtx = CodeComposer.abbrev(quote.getCustomerType());
        long intentSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_intent"));
        String intentNumber = CodeComposer.withDay("INT", customerCtx, LocalDate.now(), intentSeq);

        PaymentIntent intent = intentRepository.save(PaymentIntent.builder()
                .intentNumber(intentNumber)
                .customerType(quote.getCustomerType())
                .customerCode(quote.getCustomerCode())
                .amount(amount)
                .currency(quote.getCurrency())
                .status(PaymentIntentStatus.SUCCEEDED)
                .purpose("DEPOSIT_PAYMENT")
                .sourceType("QUOTE_DEPOSIT")
                .sourceCode(quote.getDocumentNumber())
                .build());

        String methodCtx = CodeComposer.abbrev(PaymentMethod.BANK_TRANSFER.name());
        long txnSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String txnNumber = CodeComposer.withDay("TXN", methodCtx, LocalDate.now(), txnSeq);

        PaymentTransaction transaction = transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(txnNumber)
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .provider("MANUAL")
                .amount(amount)
                .currency(quote.getCurrency())
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.now())
                .build());

        allocationRepository.save(PaymentAllocation.builder()
                .paymentTransaction(transaction)
                .billingDocumentNumber(quote.getDocumentNumber())
                .allocatedAmount(amount)
                .build());
    }

    @Override
    public BillingDocumentResponse acceptQuote(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() != BillingDocumentStatus.DRAFT && quote.getStatus() != BillingDocumentStatus.SENT
                && quote.getStatus() != BillingDocumentStatus.VIEWED
                && quote.getStatus() != BillingDocumentStatus.NEGOTIATION) {
            throw new BadRequestException("Only draft, sent, viewed or negotiation quotes can be accepted");
        }
        quote.setStatus(BillingDocumentStatus.ACCEPTED);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_ACCEPTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse rejectQuote(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() == BillingDocumentStatus.CONVERTED) {
            throw new BadRequestException("Converted quote cannot be rejected");
        }
        quote.setStatus(BillingDocumentStatus.REJECTED);
        quote.setBalanceDue(BigDecimal.ZERO);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_REJECTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse convertQuoteToInvoice(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        lifecycleSupport.ensureQuoteCanConvert(quote);

        Optional<BillingDocumentAdvance> quoteAdvanceOpt = advanceRepository.findByDocument(quote);
        boolean depositPaid = quote.getStatus() == BillingDocumentStatus.DEPOSIT_PAID
                && quoteAdvanceOpt.isPresent()
                && quoteAdvanceOpt.get().getStatus() == BillingAdvanceStatus.PAID;

        BillingDocumentResponse quoteResponse = mapper.toResponse(quote);

        CreateBillingDocumentAdvanceRequest invoiceAdvanceRequest = depositPaid
                ? toAdvanceRequest(quoteAdvanceOpt.get(), quoteNumber)
                : null;

        CreateBillingDocumentRequest request = new CreateBillingDocumentRequest(
                BillingDocumentType.INVOICE,
                quoteResponse.customerType(),
                quoteResponse.customerCode(),
                quoteResponse.customerName(),
                quoteResponse.customerEmail(),
                quoteResponse.customerPhone(),
                quoteResponse.billingAddressJson(),
                "QUOTE",
                quoteResponse.documentNumber(),
                quoteResponse.title(),
                quoteResponse.description(),
                quoteResponse.terms(),
                quoteResponse.currency(),
                LocalDate.now(),
                null,
                quoteResponse.metadataJson(),
                quoteResponse.lines().stream().map(this::toCreateLineRequest).toList(),
                quoteResponse.discounts().stream().map(this::toCreateDiscountRequest).toList(),
                quoteResponse.clauses().stream().map(this::toCreateClauseRequest).toList(),
                quoteResponse.paymentReference(),
                quoteResponse.paymentInstructions(),
                quoteResponse.bankDetailsJson(),
                invoiceAdvanceRequest,
                quoteResponse.customerReference(),
                quoteResponse.poNumber(),
                quoteResponse.projectCode(),
                quoteResponse.salespersonCode(),
                quoteResponse.deliveryAddressJson(),
                quoteResponse.language(),
                quoteResponse.exchangeRate(),
                null
        );

        BillingDocumentResponse invoice = create(request);

        // Apply deposit as pre-payment on the invoice
        if (depositPaid) {
            BigDecimal depositAmount = quoteAdvanceOpt.get().getComputedAmount();
            BillingDocument invoiceDoc = serviceByNumber(invoice.documentNumber());
            BigDecimal paid = depositAmount.min(invoiceDoc.getTotalAmount());
            invoiceDoc.setPaidAmount(paid);
            invoiceDoc.setBalanceDue(invoiceDoc.getTotalAmount().subtract(paid).max(BigDecimal.ZERO));
            invoiceDoc.setStatus(invoiceDoc.getBalanceDue().signum() == 0
                    ? BillingDocumentStatus.PAID : BillingDocumentStatus.PARTIALLY_PAID);
            if (invoiceDoc.getStatus() == BillingDocumentStatus.PAID) {
                invoiceDoc.setPaidAt(Instant.now());
            }
            documentRepository.save(invoiceDoc);

            // Mark the advance on the invoice as PAID
            advanceRepository.findByDocument(invoiceDoc).ifPresent(adv -> {
                adv.setStatus(BillingAdvanceStatus.PAID);
                adv.setPaidAt(quoteAdvanceOpt.get().getPaidAt() != null
                        ? quoteAdvanceOpt.get().getPaidAt() : Instant.now());
                advanceRepository.save(adv);
            });

            // Reuse the deposit PaymentTransaction to create an allocation on the invoice
            final BigDecimal allocatedOnInvoice = paid;
            final BillingDocument finalInvoiceDoc = invoiceDoc;
            intentRepository.findSucceededBySourceTypeAndSourceCode("QUOTE_DEPOSIT", quote.getDocumentNumber())
                    .ifPresent(depositIntent -> transactionRepository
                            .findAllByPaymentIntentIdOrderByCreatedAtDesc(depositIntent.getId())
                            .stream()
                            .filter(t -> t.getStatus() == PaymentTransactionStatus.SUCCEEDED)
                            .findFirst()
                            .ifPresent(depositTxn -> allocationRepository.save(PaymentAllocation.builder()
                                    .paymentTransaction(depositTxn)
                                    .billingDocumentNumber(finalInvoiceDoc.getDocumentNumber())
                                    .allocatedAmount(allocatedOnInvoice)
                                    .build())));

            invoice = mapper.toResponse(invoiceDoc);
        }

        quote.setStatus(BillingDocumentStatus.CONVERTED);
        quote.setBalanceDue(BigDecimal.ZERO);
        documentRepository.save(quote);
        eventWriter.write(quote, "QUOTE_CONVERTED", java.util.Map.of("invoiceNumber", invoice.documentNumber()));
        return invoice;
    }

    @Override
    public BillingDocumentResponse createCreditNote(String invoiceNumber, CreateCreditNoteRequest request) {
        BillingDocument invoice = serviceByNumber(invoiceNumber);
        lifecycleSupport.ensureCanPay(invoice);
        BigDecimal amount = request.amount() == null ? invoice.getBalanceDue() : request.amount();
        if (amount.signum() <= 0 || amount.compareTo(invoice.getTotalAmount()) > 0) {
            throw new BadRequestException("Invalid credit note amount");
        }
        List<CreateBillingDocumentLineRequest> lines = request.lines() == null || request.lines().isEmpty()
                ? List.of(new CreateBillingDocumentLineRequest(null, BillingLineType.ADJUSTMENT, invoice.getDocumentNumber(), request.reason(), null, BigDecimal.ONE, amount, BigDecimal.ZERO, BigDecimal.ZERO, false, false, BigDecimal.ZERO, BigDecimal.ZERO, "INVOICE", invoice.getDocumentNumber(), null, null, null, null, null))
                : request.lines();
        BillingDocumentResponse creditNoteResponse = create(new CreateBillingDocumentRequest(
                BillingDocumentType.CREDIT_NOTE,
                invoice.getCustomerType(),
                invoice.getCustomerCode(),
                invoice.getCustomerName(),
                invoice.getCustomerEmail(),
                invoice.getCustomerPhone(),
                invoice.getBillingAddressJson(),
                "INVOICE",
                invoice.getDocumentNumber(),
                "Avoir " + invoice.getDocumentNumber(),
                request.reason(),
                null,
                invoice.getCurrency(),
                LocalDate.now(),
                null,
                null,
                lines,
                List.of(),
                List.of(new CreateBillingDocumentClauseRequest("CREDIT_NOTE_REASON", "Motif de l'avoir", request.reason(), 1)),
                null, null, null, null,
                null, null, null, null, null, null, null, null
        ));

        // Stocker le lien vers la facture d'origine
        BillingDocument creditDoc = serviceByNumber(creditNoteResponse.documentNumber());
        creditDoc.setOriginalDocumentNumber(invoiceNumber);
        creditDoc.setOriginalDocumentType(invoice.getDocumentType().name());
        creditDoc.setCreditNoteReason(request.reason());
        documentRepository.save(creditDoc);

        fiscalAuditService.log(FiscalAuditService.CREDIT_NOTE_CREATED,
                "BILLING_DOCUMENT", creditDoc.getDocumentNumber(), invoiceNumber, null);

        // Validation SEFC immédiate si demandée (doit précéder applyImmediately)
        if (Boolean.TRUE.equals(request.validateImmediately())) {
            validate(creditDoc.getDocumentNumber());
        }
        if (Boolean.TRUE.equals(request.applyImmediately())) {
            applyCreditNote(creditDoc.getDocumentNumber());
        }
        return get(creditDoc.getDocumentNumber());
    }

    /**
     * Reverse un avoir au portefeuille du client, au lieu de l'imputer sur une facture.
     *
     * <p>Utile lorsqu'il n'y a rien a imputer : le client n'a plus de facture ouverte, mais la
     * creance existe et doit lui rester acquise pour ses prochaines commandes.
     *
     * <p>La cle d'idempotence rend un second appel inoffensif · il retrouve l'ecriture d'origine
     * au lieu de recrediter. C'est indispensable ici : un double clic ou un rejeu de requete
     * doublerait sinon un avoir bel et bien fini.
     */
    @Override
    public BillingDocumentResponse refundCreditNoteToWallet(String creditNoteNumber, String reason) {
        BillingDocument creditNote = serviceByNumber(creditNoteNumber);

        if (creditNote.getDocumentType() != BillingDocumentType.CREDIT_NOTE) {
            throw new BadRequestException("Le document " + creditNoteNumber + " n'est pas un avoir");
        }
        if (!Boolean.TRUE.equals(creditNote.getLocked())) {
            throw new BadRequestException(
                    "L'avoir " + creditNoteNumber + " doit etre valide (SEFC) avant d'etre reverse · "
                            + "un avoir non scelle n'a pas de valeur");
        }
        if (creditNote.getStatus() == BillingDocumentStatus.ISSUED) {
            throw new BadRequestException("L'avoir " + creditNoteNumber + " a deja ete consomme");
        }
        BigDecimal amount = creditNote.getTotalAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("L'avoir " + creditNoteNumber + " est d'un montant nul");
        }

        // Les portefeuilles sont uniques par proprietaire et devise depuis V206 · demander celui
        // de la devise de l'avoir donne donc le bon compte, sans conversion implicite.
        var wallet = walletService.serviceWallet(
                walletService.getOrCreate(creditNote.getCustomerType(), creditNote.getCustomerCode(),
                        creditNote.getCurrency()).walletNumber());

        String motive = StringUtils.hasText(reason)
                ? reason.trim()
                : "Avoir " + creditNoteNumber;
        walletService.credit(wallet, amount, WalletEntryType.REFUND, "CREDIT_NOTE", creditNoteNumber,
                motive, currentUserName(), "CREDIT_NOTE_REFUND:" + creditNoteNumber);

        creditNote.setStatus(BillingDocumentStatus.ISSUED);
        BillingDocument saved = documentRepository.save(creditNote);

        fiscalAuditService.log("CREDIT_NOTE_REFUNDED_TO_WALLET", "BILLING_DOCUMENT", creditNoteNumber,
                creditNote.getOriginalDocumentNumber(),
                "wallet=" + wallet.getWalletNumber() + "|amount=" + amount.toPlainString());
        eventWriter.write(saved, "CREDIT_NOTE_REFUNDED_TO_WALLET",
                java.util.Map.of("walletNumber", wallet.getWalletNumber(),
                        "amount", amount.toPlainString(),
                        "reason", motive));

        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse applyCreditNote(String creditNoteNumber) {
        BillingDocument creditNote = serviceByNumber(creditNoteNumber);
        if (creditNote.getDocumentType() != BillingDocumentType.CREDIT_NOTE || !"INVOICE".equalsIgnoreCase(creditNote.getSourceType())) {
            throw new BadRequestException("Credit note is not linked to an invoice");
        }
        BillingDocument invoice = applyPayment(creditNote.getSourceCode(), creditNote.getTotalAmount());
        creditNote.setStatus(BillingDocumentStatus.ISSUED);
        BillingDocument saved = documentRepository.save(creditNote);
        eventWriter.write(saved, "CREDIT_NOTE_APPLIED", java.util.Map.of("invoiceNumber", invoice.getDocumentNumber()));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatementResponse customerStatement(String customerType, String customerCode, Pageable pageable) {
        Pageable unsortedPageable = pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : pageable;
        String normalizedCustomerCode = requiredCustomerCode(customerCode);
        String normalizedCustomerType = normalizeCustomerType(customerType, normalizedCustomerCode);
        var documents = documentRepository.statementDocuments(normalizedCustomerType, normalizedCustomerCode, unsortedPageable).map(mapper::toResponse);
        List<Object[]> totalsResult = documentRepository.statementTotals(normalizedCustomerType, normalizedCustomerCode);
        Object[] row = totalsResult.isEmpty() ? null : totalsResult.get(0);
        BigDecimal totalInvoiced = asBigDecimal(row != null ? row[0] : null);
        BigDecimal totalPaid = asBigDecimal(row != null ? row[1] : null);
        BigDecimal balance = asBigDecimal(row != null ? row[2] : null);
        BigDecimal creditAvailable = asBigDecimal(row != null ? row[3] : null);
        BigDecimal draft = asBigDecimal(row != null ? row[4] : null);
        // Un avoir disponible s'impute sur ce qui est du · s'il depasse, le client n'est pas
        // debiteur pour autant, il garde un credit. Le net s'arrete donc a zero.
        BigDecimal net = balance.subtract(creditAvailable).max(BigDecimal.ZERO);
        return new CustomerStatementResponse(normalizedCustomerType, normalizedCustomerCode,
                totalInvoiced, totalPaid, balance, creditAvailable, net, draft, documents.getContent());
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }

    @Override
    public int markOverdueDocuments() {
        return documentRepository.markOverdueDocuments();
    }

    @Override
    public BillingDocumentResponse acceptQuotation(String quotationNumber) {
        return acceptQuote(quotationNumber);
    }

    @Override
    public BillingDocumentResponse rejectQuotation(String quotationNumber) {
        return rejectQuote(quotationNumber);
    }

    @Override
    public BillingDocumentResponse convertQuotationToInvoice(String quotationNumber) {
        return convertQuoteToInvoice(quotationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocumentResponse get(String documentNumber) {
        return mapper.toResponse(serviceByNumber(documentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BillingDocumentResponse> list(BillingDocumentType type,
                                                           BillingDocumentStatus status,
                                                           String customerType,
                                                           String customerCode,
                                                           String sourceType,
                                                           String sourceCode,
                                                           LocalDate fromDate,
                                                           LocalDate toDate,
                                                           String searchText,
                                                           Pageable pageable) {
        BillingDocumentSearchCriteria criteria = new BillingDocumentSearchCriteria(
                type,
                status,
                trim(customerType),
                trim(customerCode),
                trim(sourceType),
                trim(sourceCode),
                fromDate,
                toDate,
                trim(searchText)
        );
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(documentRepository.search(
                criteria.typeValue(),
                criteria.statusValue(),
                criteria.customerType(),
                criteria.customerCode(),
                criteria.sourceType(),
                criteria.sourceCode(),
                criteria.fromDate(),
                criteria.toDate(),
                criteria.searchText(),
                unsortedPageable
        ).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocument serviceByNumber(String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            throw new BadRequestException("Billing document number is required");
        }
        return documentRepository.findByDocumentNumber(documentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Billing document not found"));
    }

    /**
     * Charge un document sous {@code SELECT ... FOR UPDATE} pour serialiser les mutations de
     * montant paye.
     * <p>
     * Le {@code refresh} est indispensable : sans lui Hibernate rendrait l'instance deja chargee
     * dans le contexte de persistance avec ses valeurs d'origine, donc le verrou serait pose mais
     * le calcul se ferait sur un montant paye perime. Le {@code flush} prealable couvre le cas
     * d'un document cree dans la meme transaction et pas encore ecrit en base.
     */
    private BillingDocument lockedByNumber(String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            throw new BadRequestException("Billing document number is required");
        }
        entityManager.flush();
        BillingDocument document = documentRepository.lockByDocumentNumber(documentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Billing document not found"));
        entityManager.refresh(document, LockModeType.PESSIMISTIC_WRITE);
        return document;
    }

    @Override
    public BillingDocument applyPayment(String documentNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be positive");
        }
        BillingDocument document = lockedByNumber(documentNumber);
        lifecycleSupport.ensureCanPay(document);
        BigDecimal paid = document.getPaidAmount().add(amount).min(document.getTotalAmount());
        document.setPaidAmount(paid);
        document.setBalanceDue(document.getTotalAmount().subtract(paid));
        if (document.getBalanceDue().signum() == 0) {
            document.setStatus(BillingDocumentStatus.PAID);
            document.setPaidAt(Instant.now());
        } else {
            document.setStatus(BillingDocumentStatus.PARTIALLY_PAID);
        }
        return documentRepository.save(document);
    }

    @Override
    public BillingDocument reversePayment(String documentNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Reversal amount must be positive");
        }
        BillingDocument document = lockedByNumber(documentNumber);
        BigDecimal paid = document.getPaidAmount().subtract(amount);
        if (paid.signum() < 0) {
            paid = BigDecimal.ZERO;
        }
        document.setPaidAmount(paid);
        document.setBalanceDue(document.getTotalAmount().subtract(paid));
        if (paid.signum() == 0) {
            document.setStatus(BillingDocumentStatus.ISSUED);
            document.setPaidAt(null);
        } else if (document.getBalanceDue().signum() > 0) {
            document.setStatus(BillingDocumentStatus.PARTIALLY_PAID);
        }
        return documentRepository.save(document);
    }

    @Override
    public BillingDocumentResponse cancelAndArchiveDocument(String documentNumber, String reason) {
        return mapper.toResponse(cancelAndArchive(documentNumber, reason));
    }

    @Override
    public BillingDocument cancelAndArchive(String documentNumber, String reason) {
        BillingDocument document = serviceByNumber(documentNumber);

        if (document.getDocumentType() == BillingDocumentType.CREDIT_NOTE) {
            // Un avoir ne s'annule jamais par un avoir · il faut d'abord defaire son imputation.
            releaseCreditNoteBeforeCancellation(document);
        } else if (requiresCreditNoteToCancel(document)) {
            return cancelLockedDocumentViaCreditNote(document, reason);
        }

        // Annulation directe
        if (document.getStatus() != BillingDocumentStatus.CANCELLED) {
            document.setStatus(BillingDocumentStatus.CANCELLED);
            if (document.getCancelledAt() == null) {
                document.setCancelledAt(Instant.now());
            }
            document.setBalanceDue(BigDecimal.ZERO);
            if (document.getPaidAmount() == null || document.getPaidAmount().signum() == 0) {
                document.setPaidAt(null);
            }
            document = documentRepository.save(document);
            eventWriter.write(document, "BILLING_DOCUMENT_CANCELLED", billingActionDetails(reason, true));
        }
        if (document.getArchivedAt() == null) {
            document.setArchivedAt(Instant.now());
            document = documentRepository.save(document);
            eventWriter.write(document, "BILLING_DOCUMENT_ARCHIVED", billingActionDetails(reason, null));
        }
        return document;
    }

    /**
     * Un avoir est-il nécessaire pour annuler ce document ?
     *
     * <p>Non, sauf si de l'argent a réellement été encaissé. Un avoir sert à constater une
     * créance en faveur du client ; sur une facture dont rien n'a été perçu, il n'y a rien à
     * créditer et l'annulation directe suffit · elle est datée, tracée et sort le document des
     * agrégats comptables.
     *
     * <p>Non plus pour un devis, un avoir ou une note de débit : émettre un avoir d'avoir n'a
     * aucun sens, et c'est ce que le code faisait, avec pour seul effet un refus
     * « Payments can only be allocated to invoices » venu de {@code ensureCanPay}.
     */
    private boolean requiresCreditNoteToCancel(BillingDocument document) {
        if (!Boolean.TRUE.equals(document.getLocked())) {
            return false;
        }
        if (document.getDocumentType() != BillingDocumentType.INVOICE
                && document.getDocumentType() != BillingDocumentType.PROFORMA_INVOICE
                && document.getDocumentType() != BillingDocumentType.CORRECTIVE_INVOICE) {
            return false;
        }
        BigDecimal paid = document.getPaidAmount();
        return paid != null && paid.signum() > 0;
    }

    /**
     * Défait l'imputation d'un avoir avant de l'annuler.
     *
     * <p>Un avoir déjà consommé a produit un effet ailleurs : soit il a soldé une facture, soit
     * il a crédité un portefeuille. L'annuler sans défaire cet effet laisserait la contrepartie
     * en place, donc un document annulé qui continue de peser sur un solde.
     */
    private void releaseCreditNoteBeforeCancellation(BillingDocument creditNote) {
        if (creditNote.getStatus() != BillingDocumentStatus.ISSUED) {
            // Avoir non consommé · rien à défaire.
            return;
        }

        // Reversé au portefeuille : le solde a pu être dépensé depuis, on ne le reprend pas
        // silencieusement. C'est une décision de gestion, pas une écriture technique.
        boolean refundedToWallet = walletLedgerEntryRepository
                .findByIdempotencyKey("CREDIT_NOTE_REFUND:" + creditNote.getDocumentNumber())
                .isPresent();
        if (refundedToWallet) {
            throw new BadRequestException("L'avoir " + creditNote.getDocumentNumber()
                    + " a ete reverse au portefeuille du client · le solde a pu etre utilise depuis. "
                    + "Debiter le portefeuille du montant correspondant avant d'annuler l'avoir.");
        }

        // Imputé sur une facture · retirer l'imputation, sauf si la facture est elle-même
        // annulée : elle ne doit pas ressortir du néant avec un solde rouvert.
        String target = creditNote.getSourceCode();
        if (StringUtils.hasText(target) && "INVOICE".equalsIgnoreCase(creditNote.getSourceType())) {
            documentRepository.findByDocumentNumber(target)
                    .filter(invoice -> invoice.getStatus() != BillingDocumentStatus.CANCELLED
                            && invoice.getStatus() != BillingDocumentStatus.VOIDED)
                    .ifPresent(invoice -> reversePayment(target, creditNote.getTotalAmount()));
        }
    }

    /**
     * Annulation d'un document fiscalement scellé (locked=true).
     * Émet un avoir pour le solde restant, le valide et l'applique.
     * Les montants de la facture originale ne sont jamais mutés (invariant I3) : seuls les
     * champs de suivi (statut, dates d'annulation et d'archivage, solde dû) évoluent, et le
     * trigger {@code trg_billing_document_immutable} ne les protège pas.
     */
    private BillingDocument cancelLockedDocumentViaCreditNote(BillingDocument document, String reason) {
        BigDecimal creditAmount = document.getBalanceDue() != null && document.getBalanceDue().signum() > 0
                ? document.getBalanceDue()
                : document.getTotalAmount();

        if (creditAmount.signum() > 0) {
            String creditReason = StringUtils.hasText(reason) ? reason : "Annulation de " + document.getDocumentNumber();
            createCreditNote(document.getDocumentNumber(), new CreateCreditNoteRequest(
                    creditAmount,
                    creditReason,
                    true,   // applyImmediately
                    true,   // validateImmediately
                    null    // lignes auto-générées
            ));
        }

        // L'avoir est appliqué via applyPayment, qui solde la facture et la laisse donc en PAID.
        // Une facture annulee ressortait ainsi comme encaissee : elle restait comptee dans le
        // chiffre d'affaires, puisque les agregats comptables excluent CANCELLED et VOIDED mais
        // pas PAID. Le statut est donc repositionne apres l'application de l'avoir.
        BillingDocument cancelled = markCancelledAndArchived(document.getDocumentNumber());
        eventWriter.write(cancelled, "BILLING_DOCUMENT_CANCELLED_VIA_CREDIT_NOTE",
                billingActionDetails(reason, true));
        return cancelled;
    }

    /**
     * Bascule un document en {@code CANCELLED} et l'archive, sans toucher aux montants.
     * {@code paidAmount} est conservé tel quel : il fait partie de l'historique fiscal, et
     * l'exclusion du document des agrégats comptables se fait sur le statut.
     */
    private BillingDocument markCancelledAndArchived(String documentNumber) {
        BillingDocument document = serviceByNumber(documentNumber);
        if (document.getStatus() != BillingDocumentStatus.CANCELLED) {
            document.setStatus(BillingDocumentStatus.CANCELLED);
            if (document.getCancelledAt() == null) {
                document.setCancelledAt(Instant.now());
            }
        }
        document.setBalanceDue(BigDecimal.ZERO);
        if (document.getArchivedAt() == null) {
            document.setArchivedAt(Instant.now());
        }
        return documentRepository.save(document);
    }

    @Override
    public BillingDocumentResponse applyCreditNoteToInvoice(String creditNoteNumber, String targetInvoiceNumber) {
        BillingDocument creditNote = serviceByNumber(creditNoteNumber);
        BillingDocument targetInvoice = serviceByNumber(targetInvoiceNumber);

        if (creditNote.getDocumentType() != BillingDocumentType.CREDIT_NOTE) {
            throw new BadRequestException("Le document " + creditNoteNumber + " n'est pas un avoir");
        }
        if (!Boolean.TRUE.equals(creditNote.getLocked())) {
            throw new BadRequestException("L'avoir doit être validé (SEFC) avant d'être appliqué à une autre facture");
        }
        if (creditNote.getStatus() == BillingDocumentStatus.ISSUED) {
            throw new BadRequestException("L'avoir " + creditNoteNumber + " a déjà été consommé");
        }
        if (!creditNote.getCustomerCode().equals(targetInvoice.getCustomerCode())) {
            throw new BadRequestException("L'avoir et la facture cible doivent appartenir au même client");
        }
        if (targetInvoice.getDocumentType() != BillingDocumentType.INVOICE
                && targetInvoice.getDocumentType() != BillingDocumentType.PROFORMA_INVOICE) {
            throw new BadRequestException("La cible doit être une facture ou une facture proforma");
        }

        applyPayment(targetInvoiceNumber, creditNote.getTotalAmount());

        creditNote.setStatus(BillingDocumentStatus.ISSUED);
        BillingDocument savedCreditNote = documentRepository.save(creditNote);

        fiscalAuditService.log(FiscalAuditService.CREDIT_NOTE_APPLIED,
                "BILLING_DOCUMENT", creditNoteNumber,
                creditNote.getOriginalDocumentNumber(),
                "applied_to=" + targetInvoiceNumber + "|amount=" + creditNote.getTotalAmount().toPlainString());
        eventWriter.write(savedCreditNote, "CREDIT_NOTE_APPLIED_TO_INVOICE",
                java.util.Map.of("targetInvoiceNumber", targetInvoiceNumber,
                                 "amount", creditNote.getTotalAmount().toPlainString()));

        return mapper.toResponse(savedCreditNote);
    }

    @Override
    public BillingDocumentResponse createCorrectiveInvoice(String originalInvoiceNumber,
                                                           CreateManualBillingDocumentRequest request) {
        BillingDocument original = serviceByNumber(originalInvoiceNumber);

        if (original.getDocumentType() != BillingDocumentType.INVOICE
                && original.getDocumentType() != BillingDocumentType.PROFORMA_INVOICE) {
            throw new BadRequestException("Seules les factures peuvent faire l'objet d'une facture rectificative");
        }
        if (!Boolean.TRUE.equals(original.getLocked())) {
            throw new BadRequestException(
                    "La facture d'origine doit être validée (SEFC) avant d'émettre une rectificative. " +
                    "Pour un brouillon, utilisez directement update().");
        }

        // Une rectificative corrige une facture precise · son destinataire est celui de la
        // facture d'origine, il n'a pas a etre resaisi. Sans cette reprise, l'appel echouait sur
        // « Customer name is required when customer cannot be resolved » alors que le client
        // etait parfaitement determine. Une valeur explicitement fournie reste prioritaire.
        BillingDocumentResponse corrective = create(
                inheritCustomer(toDocumentRequest(BillingDocumentType.CORRECTIVE_INVOICE, request), original));

        BillingDocument correctiveDoc = serviceByNumber(corrective.documentNumber());
        correctiveDoc.setOriginalDocumentNumber(originalInvoiceNumber);
        correctiveDoc.setOriginalDocumentType(original.getDocumentType().name());
        correctiveDoc.setCreditNoteReason(StringUtils.hasText(request.description())
                ? request.description()
                : "Rectification de " + originalInvoiceNumber);
        documentRepository.save(correctiveDoc);

        fiscalAuditService.log(FiscalAuditService.CORRECTIVE_INVOICE_CREATED,
                "BILLING_DOCUMENT", corrective.documentNumber(), originalInvoiceNumber, null);

        return get(corrective.documentNumber());
    }

    /** Rend {@code null} une valeur absente ou reduite a des espaces, pour que le gabarit masque la ligne. */
    private String emptyToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    /** Reprend la valeur transmise, ou en tire une de la serie dediee quand rien n'est fourni. */
    private String valueOrGenerated(String value, Supplier<String> generator) {
        String trimmed = emptyToNull(value);
        return trimmed != null ? trimmed : generator.get();
    }

    private BillingDocument buildDocument(CreateBillingDocumentRequest request,
                                          BillingCustomerSnapshotResolver.CustomerSnapshot customer,
                                          BillingCalculationService.CalculatedDocument calculation) {
        String documentNumber = numberingSupport.nextDocumentNumber(request.documentType(), customer.customerType());
        // Le bon de commande et la reference client sont des documents du client vers nous : le
        // premier est la commande qu'il nous passe, la seconde la reference sous laquelle il range
        // notre facture. Les generer reviendrait a affirmer sur chaque facture une commande qu'il
        // n'a jamais passee, et a entrer en conflit le jour ou il en transmet une vraie. Faute de
        // valeur fournie, la ligne ne figure pas : le gabarit la masque quand le champ est vide.
        //
        // L'affaire est notre reference a nous, elle tire donc sa propre serie annuelle. Elle etait,
        // comme les deux autres, remplie en prefixant le numero de document
        // (« PRJ-INV-MAN-20260904-00000018 ») : elle redisait un numero deja imprime deux fois sur
        // la page. Le lien avec la facture n'a pas a etre recopie dans le numero, la ligne du
        // document rangeant l'affaire a cote de son propre numero.
        String customerReference = emptyToNull(request.customerReference());
        String poNumber = emptyToNull(request.poNumber());
        String projectCode = valueOrGenerated(request.projectCode(),
                numberingSupport::nextProjectCode);
        // Le commercial reste vide quand personne n'est rattache. « SYSTEM » s'imprimait tel quel
        // sur la facture du client, en face du libelle « Commercial ».
        String salespersonCode = emptyToNull(request.salespersonCode());
        return BillingDocument.builder()
                .documentNumber(documentNumber)
                .documentType(request.documentType())
                .status(BillingDocumentStatus.DRAFT)
                .customerType(customer.customerType())
                .customerCode(customer.customerCode())
                .customerName(customer.customerName())
                .customerEmail(customer.customerEmail())
                .customerPhone(customer.customerPhone())
                .billingAddressJson(customer.billingAddressJson())
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .title(trim(request.title()))
                .description(trim(request.description()))
                .terms(trim(request.terms()))
                .currency(request.currency().trim().toUpperCase())
                .subtotalAmount(calculation.subtotalAmount())
                .discountAmount(calculation.discountAmount())
                .taxableAmount(calculation.taxableAmount())
                .vatAmount(calculation.vatAmount())
                .additionalCentAmount(calculation.additionalCentAmount())
                .taxAmount(calculation.taxAmount())
                .totalAmount(calculation.totalAmount())
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(calculation.totalAmount())
                .issueDate(request.issueDate() == null ? LocalDate.now() : request.issueDate())
                .dueDate(request.dueDate())
                .customerReference(customerReference)
                .poNumber(poNumber)
                .projectCode(projectCode)
                .salespersonCode(salespersonCode)
                .deliveryAddressJson(trim(request.deliveryAddressJson()))
                .language(trim(request.language()) == null ? "fr" : trim(request.language()))
                .exchangeRate(request.exchangeRate())
                .paymentReference(trim(request.paymentReference()))
                .paymentInstructions(trim(request.paymentInstructions()))
                .bankDetailsJson(trim(request.bankDetailsJson()))
                .metadataJson(trim(request.metadataJson()))
                .build();
    }

    private CreateBillingDocumentAdvanceRequest toAdvanceRequest(BillingDocumentAdvance advance, String quoteNumber) {
        return new CreateBillingDocumentAdvanceRequest(
                BillingAdvanceType.FIXED_AMOUNT,
                advance.getComputedAmount(),
                parseOrderString(advance.getIncludedLineOrders()),
                parseOrderString(advance.getExcludedLineOrders()),
                advance.getPaymentReference(),
                advance.getReferenceLabel() != null
                        ? advance.getReferenceLabel()
                        : "Acompte reçu · Devis " + quoteNumber,
                null,
                advance.getNotes()
        );
    }

    private List<Integer> parseOrderString(String orders) {
        if (orders == null || orders.isBlank()) return null;
        return Arrays.stream(orders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    private List<BillableItem> resolveBillableItems(List<String> billableNumbers) {
        List<BillableItem> items = new ArrayList<>();
        String currency = null;
        for (String number : billableNumbers) {
            BillableItem item = billableItemRepository.findByBillableNumber(number)
                    .orElseThrow(() -> new ResourceNotFoundException("Billable item not found: " + number));
            if (item.getStatus() != BillableItemStatus.PENDING && item.getStatus() != BillableItemStatus.SENT_TO_INVOICE) {
                throw new BadRequestException("Billable item is not invoiceable: " + number);
            }
            if (currency == null) {
                currency = item.getCurrency();
            } else if (!currency.equalsIgnoreCase(item.getCurrency())) {
                throw new BadRequestException("All billable items must use the same currency");
            }
            items.add(item);
        }
        return items;
    }

    private List<CreateBillingDocumentClauseRequest> defaultInvoiceClauses() {
        return List.of(new CreateBillingDocumentClauseRequest(
                "PAYMENT_TERMS",
                "Conditions de paiement",
                "La facture est payable selon les modalites convenues. Tout retard peut entrainer des restrictions de service.",
                1
        ));
    }

    private CreateBillingDocumentRequest toDocumentRequest(BillingDocumentType type, CreateManualBillingDocumentRequest request) {
        return new CreateBillingDocumentRequest(
                type,
                request.customerType(),
                request.customerCode(),
                request.customerName(),
                request.customerEmail(),
                request.customerPhone(),
                request.billingAddressJson(),
                request.sourceType(),
                request.sourceCode(),
                request.title(),
                request.description(),
                request.terms(),
                request.currency(),
                request.issueDate(),
                request.dueDate(),
                request.metadataJson(),
                request.lines(),
                request.discounts(),
                request.clauses(),
                request.paymentReference(),
                request.paymentInstructions(),
                request.bankDetailsJson(),
                request.advance(),
                request.customerReference(),
                request.poNumber(),
                request.projectCode(),
                request.salespersonCode(),
                request.deliveryAddressJson(),
                request.language(),
                request.exchangeRate(),
                request.earlyPaymentDiscount()
        );
    }

    /**
     * Reprend le destinataire d'un document de reference lorsque la requete ne le precise pas.
     *
     * <p>Une rectificative corrige une facture donnee : son destinataire est celui de cette
     * facture, et le resaisir n'apporte rien qu'un risque de divergence. Une valeur explicitement
     * fournie reste prioritaire · corriger les coordonnees imprimees fait justement partie des
     * motifs d'emission d'une rectificative.
     */
    private CreateBillingDocumentRequest inheritCustomer(CreateBillingDocumentRequest request,
                                                         BillingDocument source) {
        if (StringUtils.hasText(request.customerCode()) || StringUtils.hasText(request.customerName())) {
            return request;
        }
        return new CreateBillingDocumentRequest(
                request.documentType(),
                firstNonBlank(request.customerType(), source.getCustomerType()),
                firstNonBlank(request.customerCode(), source.getCustomerCode()),
                firstNonBlank(request.customerName(), source.getCustomerName()),
                firstNonBlank(request.customerEmail(), source.getCustomerEmail()),
                firstNonBlank(request.customerPhone(), source.getCustomerPhone()),
                firstNonBlank(request.billingAddressJson(), source.getBillingAddressJson()),
                request.sourceType(),
                request.sourceCode(),
                request.title(),
                request.description(),
                request.terms(),
                firstNonBlank(request.currency(), source.getCurrency()),
                request.issueDate(),
                request.dueDate(),
                request.metadataJson(),
                request.lines(),
                request.discounts(),
                request.clauses(),
                request.paymentReference(),
                request.paymentInstructions(),
                request.bankDetailsJson(),
                request.advance(),
                request.customerReference(),
                request.poNumber(),
                request.projectCode(),
                request.salespersonCode(),
                request.deliveryAddressJson(),
                request.language(),
                request.exchangeRate(),
                request.earlyPaymentDiscount()
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private BillingLineType lineTypeFromBillable(BillableItem item) {
        String sourceType = item.getSourceType() == null ? "" : item.getSourceType().toUpperCase();
        if (sourceType.contains("BOOKING")) {
            return BillingLineType.BOOKING;
        }
        if (sourceType.contains("PASS")) {
            return BillingLineType.PASS;
        }
        if (sourceType.contains("ADDON")) {
            return BillingLineType.ADDON;
        }
        if (sourceType.contains("SUBSCRIPTION")) {
            return BillingLineType.SUBSCRIPTION;
        }
        if (sourceType.contains("OVERAGE")) {
            return BillingLineType.OVERAGE;
        }
        if (sourceType.contains("INVENTORY") || sourceType.contains("ITEM") || sourceType.contains("PRODUCT")) {
            return BillingLineType.PRODUCT;
        }
        return BillingLineType.SERVICE;
    }

    private BillingDocument quote(String quoteNumber) {
        BillingDocument quote = serviceByNumber(quoteNumber);
        if (quote.getDocumentType() != BillingDocumentType.QUOTE) {
            throw new BadRequestException("Billing document is not a quote");
        }
        return quote;
    }

    /**
     * Part <b>fixe</b> de la remise d'une ligne deja calculee.
     *
     * <p>Le moteur additionne la remise exprimee en taux et celle exprimee en montant, puis
     * conserve sur la ligne le taux fourni <i>et</i> le montant resolu. Recopier les deux tels
     * quels · a la conversion d'un devis, a la duplication, a chaque modification · fait
     * recalculer le taux et l'ajouter a un montant qui le contenait deja : la remise double, et
     * se cumule a chaque nouvelle copie.
     *
     * <p>On ne renvoie donc que ce que le taux ne reproduit pas. Le taux, lui, est recopie tel
     * quel : il reste affiche sur le document et sera recalcule a l'identique.
     */
    private BigDecimal residualFixedDiscount(BigDecimal subtotal, BigDecimal discountRate,
                                             BigDecimal discountAmount) {
        BigDecimal amount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (discountRate == null || discountRate.signum() <= 0
                || subtotal == null || subtotal.signum() <= 0) {
            return amount;
        }
        // Meme arrondi que BillingCalculationService.percentage · un ecart de rounding ici
        // reapparaitrait comme un residu de quelques centimes a chaque copie.
        BigDecimal fromRate = subtotal.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal residual = amount.subtract(fromRate);
        return residual.signum() > 0 ? residual : BigDecimal.ZERO;
    }

    private CreateBillingDocumentLineRequest toCreateLineRequest(BillingDocumentLineResponse line) {
        return new CreateBillingDocumentLineRequest(
                line.lineOrder(),
                line.lineType(),
                line.itemCode(),
                line.description(),
                line.detailedDescription(),
                line.quantity(),
                line.unitPrice(),
                line.discountRate(),
                residualFixedDiscount(line.subtotalAmount(), line.discountRate(), line.discountAmount()),
                line.taxable(),
                line.taxIncluded(),
                line.vatRate(),
                line.additionalCentRate(),
                line.sourceType(),
                line.sourceCode(),
                line.unit(),
                line.externalReference(),
                line.notes(),
                Boolean.TRUE.equals(line.optional()) ? Boolean.TRUE : null,
                line.category()
        );
    }

    private CreateBillingDocumentLineRequest toCreateLineRequest(BillingDocumentLine line) {
        return new CreateBillingDocumentLineRequest(
                line.getLineOrder(),
                line.getLineType(),
                line.getItemCode(),
                line.getDescription(),
                line.getDetailedDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getDiscountRate(),
                residualFixedDiscount(line.getSubtotalAmount(), line.getDiscountRate(), line.getDiscountAmount()),
                line.getTaxable(),
                line.getTaxIncluded(),
                line.getVatRate(),
                line.getAdditionalCentRate(),
                line.getSourceType(),
                line.getSourceCode(),
                line.getUnit(),
                line.getExternalReference(),
                line.getNotes(),
                Boolean.TRUE.equals(line.getOptional()) ? Boolean.TRUE : null,
                line.getCategory()
        );
    }

    private CreateBillingDocumentDiscountRequest toCreateDiscountRequest(BillingDocumentDiscountResponse discount) {
        return new CreateBillingDocumentDiscountRequest(
                discount.discountCode(),
                discount.description(),
                discount.discountType(),
                discount.value()
        );
    }

    private CreateBillingDocumentClauseRequest toCreateClauseRequest(BillingDocumentClauseResponse clause) {
        return new CreateBillingDocumentClauseRequest(
                clause.clauseCode(),
                clause.title(),
                clause.body(),
                clause.displayOrder()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCustomerType(String customerType, String customerCode) {
        String normalized = trim(customerType);
        if (normalized == null) {
            throw new BadRequestException("Customer type is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (normalized.equals("CLIENT") || normalized.equals("CUSTOMER")) {
            String inferred = inferCustomerTypeFromCode(customerCode);
            if (inferred != null) {
                return inferred;
            }
        }
        return switch (normalized) {
            case "MEMBRE" -> "MEMBER";
            case "CLIENT" -> "CUSTOMER";
            case "BUSINESS", "COMPANY", "ENTREPRISE" -> "BUSINESS_ENTITY";
            default -> normalized;
        };
    }

    private String requiredCustomerCode(String customerCode) {
        String normalized = trim(customerCode);
        if (normalized == null) {
            throw new BadRequestException("Customer code is required");
        }
        return normalized;
    }

    private String inferCustomerTypeFromCode(String customerCode) {
        if (!StringUtils.hasText(customerCode)) {
            return null;
        }
        String normalizedCode = customerCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.startsWith("MBR-")) {
            return "MEMBER";
        }
        if (normalizedCode.startsWith("CUS-")) {
            return "CUSTOMER";
        }
        if (normalizedCode.startsWith("BUS-") || normalizedCode.startsWith("BIZ-")) {
            return "BUSINESS_ENTITY";
        }
        return null;
    }

    private java.util.Map<String, Object> billingActionDetails(String reason, Boolean archived) {
        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        if (StringUtils.hasText(reason)) {
            details.put("reason", reason.trim());
        }
        if (archived != null) {
            details.put("archived", archived);
        }
        return details;
    }

    /**
     * Valide fiscalement un document SEFC · opération atomique en 12 étapes.
     * Toutes les écritures se font dans la même transaction (@Transactional de classe).
     *
     * Étapes :
     *  1.  Vérification du statut (ensureCanValidate)
     *  2.  Date fiscale = aujourd'hui
     *  3.  Numéro fiscal SEFC définitif (DocumentSequenceService, SELECT FOR UPDATE)
     *  4.  Hash précédent (findLastValidatedHash, SELECT FOR UPDATE SKIP LOCKED)
     *  5.  Hash courant SHA-256 (FiscalHashService)
     *  6.  Signature HMAC-SHA256 (FiscalSignatureService)
     *  7.  Verrouillage et statut VALIDATED
     *  8.  Persistance
     *  9.  Journal d'audit fiscal
     * 10.  Événement métier
     */
    @Override
    public BillingDocumentResponse validate(String documentNumber) {
        // 1 · garde
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanValidate(document);

        // 2 · date fiscale
        LocalDate fiscalDate = LocalDate.now();

        // 3 · numéro fiscal SEFC (séquence annuelle, SELECT FOR UPDATE)
        String fiscalNumber = documentSequenceService.nextFiscalNumber(document.getDocumentType(), fiscalDate);

        document.setFiscalDate(fiscalDate);
        document.setFiscalNumber(fiscalNumber);

        // 4 · hash précédent (chaînage, SELECT FOR UPDATE SKIP LOCKED)
        String previousHash = documentRepository
                .findLastValidatedHash(document.getDocumentType().name())
                .orElse(null);

        // 5 · hash courant SHA-256
        String currentHash = fiscalHashService.compute(document, previousHash);

        // 6 · signature HMAC-SHA256
        String signature = fiscalSignatureService.sign(currentHash);

        // 7 · verrouillage
        document.setPreviousHash(previousHash);
        document.setCurrentHash(currentHash);
        document.setHashAlgorithm(FiscalHashService.ALGORITHM);
        document.setFiscalSignature(signature);
        document.setSignatureAlgorithm(FiscalSignatureService.ALGORITHM);
        document.setSignedAt(Instant.now());
        document.setLocked(true);
        document.setValidatedAt(Instant.now());
        document.setStatus(BillingDocumentStatus.VALIDATED);

        // 8 · persistance (une seule écriture atomique)
        BillingDocument saved = documentRepository.save(document);

        // 9 · journal d'audit
        fiscalAuditService.log(
                FiscalAuditService.INVOICE_VALIDATED,
                "BILLING_DOCUMENT",
                document.getDocumentNumber(),
                null,
                fiscalNumber + "|hash=" + currentHash.substring(0, 12)
        );

        // 10 · événement métier
        eventWriter.write(saved, "BILLING_DOCUMENT_VALIDATED", null);
        return mapper.toResponse(saved);
    }
}
