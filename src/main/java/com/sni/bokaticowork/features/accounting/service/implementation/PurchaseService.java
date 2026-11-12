package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.accounting.BudgetControl;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.enums.FundNeedStatus;
import com.sni.bokaticowork.features.fund.model.FundNeedLine;
import com.sni.bokaticowork.features.fund.model.FundNeedStatement;
import com.sni.bokaticowork.features.fund.repository.FundNeedLineRepository;
import com.sni.bokaticowork.features.fund.repository.FundNeedStatementRepository;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.LinkedSupplier;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Achats (lot F13) : fournisseurs et leur retenue a la source, commandes (saisies ou issues d'un
 * etat de besoin valide) validees par une autre personne et engagees au budget, factures et avoirs
 * fournisseurs ecrits au journal des achats, reglements lettres sur le compte fournisseur,
 * echeancier, releve fournisseur, etat des retenues a la source.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseService {

    public static final String ORDER_SOURCE = "PURCHASE_ORDER";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final AccSupplierRepository suppliers;
    private final AccPurchaseOrderRepository orders;
    private final AccPurchaseOrderLineRepository orderLines;
    private final AccSupplierInvoiceRepository invoices;
    private final AccSupplierInvoiceLineRepository invoiceLines;
    private final AccSupplierPaymentRepository payments;
    private final FundNeedStatementRepository needs;
    private final FundNeedLineRepository needLines;
    private final CashOperationNatureRepository natures;
    private final AccountingEntryService entries;
    private final AccountingLetteringService lettering;
    private final AccountingSettings settings;
    private final VoucherService vouchers;
    private final CashActorResolver actors;
    private final ObjectProvider<BudgetControl> budgetControl;
    private final DataSource dataSource;
    private final EntityManager entityManager;
    /** Fiches fournisseurs d'inventaire (reference des fournisseurs, suggestion). */
    private final SupplierRepository inventorySuppliers;

    // ------------------------------------------------------------------ fournisseurs

    @Transactional(readOnly = true)
    public List<AccSupplier> suppliers() {
        return suppliers.findAllByOrderByNameAsc();
    }

    public AccSupplier saveSupplier(AccSupplier request) {
        require("SUPPLIER:MANAGE");
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Nom du fournisseur obligatoire");
        }
        String code = StringUtils.hasText(request.getCode()) ? request.getCode().trim().toUpperCase(Locale.ROOT) : "FRN-" + (suppliers.count() + 1);
        AccSupplier supplier = suppliers.findByCode(code).orElseGet(() -> AccSupplier.builder().code(code).createdBy(actors.resolve("SYSTEM")).build());
        supplier.setName(request.getName().trim());
        supplier.setNiu(request.getNiu());
        supplier.setRccm(request.getRccm());
        supplier.setAddress(request.getAddress());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setBankDetails(request.getBankDetails());
        supplier.setPaymentTermsDays(request.getPaymentTermsDays() == null ? 30 : request.getPaymentTermsDays());
        supplier.setAccountNumber(StringUtils.hasText(request.getAccountNumber()) ? request.getAccountNumber().trim()
                : settings.text("accounting.purchase.supplier-account", "401"));
        supplier.setDefaultAccount(request.getDefaultAccount());
        supplier.setVatExempt(Boolean.TRUE.equals(request.getVatExempt()));
        BigDecimal rate = request.getWithholdingRate() == null ? BigDecimal.ZERO : request.getWithholdingRate();
        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) >= 0) {
            throw new BadRequestException("Taux de retenue entre 0 et 100");
        }
        supplier.setWithholdingRate(rate);
        supplier.setWithholdingAccount(StringUtils.hasText(request.getWithholdingAccount()) ? request.getWithholdingAccount().trim()
                : settings.text("accounting.purchase.withholding-account", "447"));
        supplier.setWithholdingBase("TTC".equalsIgnoreCase(request.getWithholdingBase()) ? "TTC" : "HT");
        supplier.setWithholdingLabel(request.getWithholdingLabel());
        supplier.setStatus("INACTIVE".equalsIgnoreCase(request.getStatus()) ? "INACTIVE" : "ACTIVE");
        if (request.getInventorySupplierId() != null && !request.getInventorySupplierId().equals(supplier.getInventorySupplierId())) {
            Supplier inventory = inventorySuppliers.findById(request.getInventorySupplierId())
                    .orElseThrow(() -> new BadRequestException("Fiche fournisseur d'inventaire inconnue : " + request.getInventorySupplierId()));
            suppliers.findByInventorySupplierId(inventory.getId()).filter(other -> !other.getCode().equals(supplier.getCode())).ifPresent(other -> {
                throw new ConflictException("La fiche " + inventory.getSupplierCode() + " est deja reliee au fournisseur comptable " + other.getCode());
            });
            supplier.setInventorySupplierId(inventory.getId());
        }
        return suppliers.save(supplier);
    }

    /**
     * Fournisseur comptable d'une fiche fournisseur d'inventaire : celui qui lui est relie, sinon
     * un nouveau, cree depuis la fiche (nom, NIU, adresse, telephone, courriel ; compte 401 et
     * retenue a la source par defaut).
     */
    public AccSupplier supplierFromInventory(String inventoryCode) {
        require("SUPPLIER:MANAGE");
        return linkedSupplier(inventorySupplier(inventoryCode));
    }

    private Supplier inventorySupplier(String inventoryCode) {
        Supplier inventory = LinkedSupplier.find(inventorySuppliers, inventoryCode)
                .orElseThrow(() -> new BadRequestException("Fournisseur d'inventaire inconnu : " + inventoryCode));
        if (inventory.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException("Le fournisseur " + inventory.getName() + " n'est pas actif");
        }
        return inventory;
    }

    private AccSupplier linkedSupplier(Supplier inventory) {
        Optional<AccSupplier> linked = suppliers.findByInventorySupplierId(inventory.getId());
        if (linked.isPresent()) {
            return linked.get();
        }
        String code = inventory.getSupplierCode().trim().toUpperCase(Locale.ROOT);
        if (code.length() > 40 || suppliers.existsByCode(code)) {
            long next = suppliers.count() + 1;
            code = "FRN-" + next;
            while (suppliers.existsByCode(code)) {
                next++;
                code = "FRN-" + next;
            }
        }
        return suppliers.save(AccSupplier.builder().code(code).name(cut(inventory.getName(), 200)).niu(cut(inventory.getTaxId(), 60))
                .address(cut(inventory.getAddress(), 500)).phone(cut(inventory.getPhone(), 40)).email(cut(inventory.getEmail(), 180))
                .accountNumber(settings.text("accounting.purchase.supplier-account", "401"))
                .withholdingAccount(settings.text("accounting.purchase.withholding-account", "447"))
                .inventorySupplierId(inventory.getId()).createdBy(actors.resolve("SYSTEM")).build());
    }

    private static String cut(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    // ------------------------------------------------------------------ commandes

    public record LineInput(String designation, BigDecimal quantity, BigDecimal unitPrice, String natureCode, String accountNumber, BigDecimal vatRate,
                            String sectionCode, Integer orderLineNo) {
    }

    public record OrderRequest(String supplierCode, String subject, LocalDate orderDate, LocalDate expectedDate, String sectionCode, List<LineInput> lines) {
    }

    public record OrderView(AccPurchaseOrder order, List<AccPurchaseOrderLine> lines, AccSupplier supplier, List<AccSupplierInvoice> invoices) {
    }

    @Transactional(readOnly = true)
    public List<AccPurchaseOrder> orders() {
        return orders.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public OrderView order(String number) {
        AccPurchaseOrder order = orderOf(number);
        return new OrderView(order, orderLines.findAllByOrderIdOrderByLineNoAsc(order.getId()), suppliers.findByCode(order.getSupplierCode()).orElse(null),
                invoices.findAllByOrderNumber(order.getNumber()));
    }

    public OrderView createOrder(OrderRequest request) {
        require("PURCHASE_ORDER:MANAGE");
        AccSupplier supplier = activeSupplier(request.supplierCode());
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BadRequestException("Une commande a au moins une ligne");
        }
        LocalDate date = request.orderDate() == null ? LocalDate.now() : request.orderDate();
        AccPurchaseOrder order = orders.save(AccPurchaseOrder.builder().number(nextNumber("BC", date, true)).supplierCode(supplier.getCode())
                .subject(StringUtils.hasText(request.subject()) ? request.subject() : "Commande " + supplier.getName()).orderDate(date)
                .expectedDate(request.expectedDate()).sectionCode(request.sectionCode()).createdBy(actors.resolve("SYSTEM")).build());
        saveOrderLines(order, supplier, request.lines(), null);
        return order(order.getNumber());
    }

    /**
     * Commande issue d'un etat de besoin valide (toutes ses lignes chiffrees restantes, ou celles
     * choisies), sans ressaisie ; les lignes sont marquees transformees.
     */
    public OrderView orderFromNeed(String needNumber, String supplierCode, List<Integer> lineNos) {
        require("PURCHASE_ORDER:MANAGE");
        FundNeedStatement need = needs.findByNumber(needNumber).orElseThrow(() -> new ResourceNotFoundException("Etat de besoin introuvable : " + needNumber));
        if (need.getStatus() != FundNeedStatus.VALIDATED && need.getStatus() != FundNeedStatus.PARTIALLY_TRANSFORMED) {
            throw new BadRequestException("Seul un etat de besoin valide se transforme en commande");
        }
        List<FundNeedLine> lines = needLines.findByStatementIdOrderByLineNoAsc(need.getId()).stream()
                .filter(line -> line.getTransformedRequestNumber() == null && line.getTotalAmount() != null && line.getTotalAmount().signum() > 0)
                .filter(line -> lineNos == null || lineNos.isEmpty() || lineNos.contains(line.getLineNo())).toList();
        if (lines.isEmpty()) {
            throw new BadRequestException("Aucune ligne chiffree restant a transformer");
        }
        AccSupplier supplier = needSupplier(supplierCode, lines);
        AccPurchaseOrder order = orders.save(AccPurchaseOrder.builder().number(nextNumber("BC", LocalDate.now(), true)).supplierCode(supplier.getCode())
                .needStatementNumber(need.getNumber()).subject(need.getSubject()).orderDate(LocalDate.now()).expectedDate(need.getWantedDate())
                .sectionCode(need.getCostCenter()).createdBy(actors.resolve("SYSTEM")).build());
        List<LineInput> inputs = lines.stream().map(line -> new LineInput(line.getDesignation(), line.getQuantity() == null ? BigDecimal.ONE : line.getQuantity(),
                line.getUnitPrice() != null ? line.getUnitPrice() : line.getTotalAmount(), line.getNatureCode(), null, null, need.getCostCenter(), line.getLineNo())).toList();
        saveOrderLines(order, supplier, inputs, true);
        lines.forEach(line -> {
            line.setTransformedRequestNumber(order.getNumber());
            needLines.save(line);
        });
        boolean all = needLines.findByStatementIdOrderByLineNoAsc(need.getId()).stream()
                .allMatch(line -> line.getTransformedRequestNumber() != null || line.getTotalAmount() == null || line.getTotalAmount().signum() == 0);
        need.setStatus(all ? FundNeedStatus.TRANSFORMED : FundNeedStatus.PARTIALLY_TRANSFORMED);
        needs.save(need);
        return order(order.getNumber());
    }

    private void saveOrderLines(AccPurchaseOrder order, AccSupplier supplier, List<LineInput> inputs, Boolean fromNeed) {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        int no = 1;
        for (LineInput input : inputs) {
            if (!StringUtils.hasText(input.designation()) || input.unitPrice() == null || input.unitPrice().signum() <= 0) {
                throw new BadRequestException("Ligne " + no + " : designation et prix unitaire positifs obligatoires");
            }
            BigDecimal quantity = input.quantity() == null || input.quantity().signum() <= 0 ? BigDecimal.ONE : input.quantity();
            BigDecimal amount = money(input.unitPrice().multiply(quantity));
            BigDecimal rate = Boolean.TRUE.equals(supplier.getVatExempt()) ? BigDecimal.ZERO : (input.vatRate() == null ? defaultVatRate() : input.vatRate());
            BigDecimal lineVat = money(amount.multiply(rate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            orderLines.save(AccPurchaseOrderLine.builder().orderId(order.getId()).lineNo(no).designation(input.designation()).quantity(quantity)
                    .unitPrice(input.unitPrice()).natureCode(input.natureCode()).accountNumber(account(input.natureCode(), input.accountNumber(), supplier))
                    .vatRate(rate).amountHt(amount).vatAmount(lineVat).sectionCode(StringUtils.hasText(input.sectionCode()) ? input.sectionCode() : order.getSectionCode())
                    .needLineNo(Boolean.TRUE.equals(fromNeed) ? input.orderLineNo() : null).build());
            ht = ht.add(amount);
            vat = vat.add(lineVat);
            no++;
        }
        order.setTotalHt(ht);
        order.setTotalVat(vat);
        order.setTotalTtc(ht.add(vat));
        orders.save(order);
    }

    /**
     * Validation par une autre personne que le preparateur : controle budgetaire, puis engagement
     * des montants hors taxe. Un depassement bloquant est refuse, sauf derogation tracee
     * (droit BUDGET:OVERRIDE, motif obligatoire).
     */
    public OrderView validateOrder(String number, String overrideReason) {
        require("PURCHASE_ORDER:VALIDATE");
        AccPurchaseOrder order = orderOf(number);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BadRequestException("La commande " + number + " n'est pas en brouillon");
        }
        String actor = actors.resolve("SYSTEM");
        if (CashActorResolver.samePerson(actor, order.getCreatedBy())) {
            throw new ForbiddenException("Une commande se valide par une autre personne que celle qui l'a preparee");
        }
        BudgetControl control = budgetControl.getIfAvailable();
        if (control != null) {
            List<BudgetControl.Spend> spends = spends(order);
            BudgetControl.Impact impact = control.check(spends, order.getOrderDate());
            if (impact.blocked()) {
                if (!actors.hasAuthority("BUDGET:OVERRIDE") || !StringUtils.hasText(overrideReason)) {
                    throw new ConflictException("budget", "la commande depasse le budget : " + String.join(" ; ", impact.messages())
                            + " (derogation : droit BUDGET:OVERRIDE et motif)");
                }
                control.recordOverride(order.getNumber(), impact, overrideReason, actor);
            }
            control.commit(ORDER_SOURCE, order.getNumber(), spends, order.getOrderDate(), null, false, null, null, actor);
            order.setBudgetCommitted(true);
        }
        order.setStatus("VALIDATED");
        order.setValidatedBy(actor);
        order.setValidatedAt(Instant.now());
        orders.save(order);
        return order(number);
    }

    /** Annulation d'une commande pas encore facturee : l'engagement budgetaire est libere. */
    public OrderView cancelOrder(String number, String reason) {
        require("PURCHASE_ORDER:VALIDATE");
        AccPurchaseOrder order = orderOf(number);
        if (!"DRAFT".equals(order.getStatus()) && !"VALIDATED".equals(order.getStatus())) {
            throw new BadRequestException("Une commande deja facturee ne s'annule pas (" + order.getStatus() + ")");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif de l'annulation");
        }
        if (Boolean.TRUE.equals(order.getBudgetCommitted())) {
            BudgetControl control = budgetControl.getIfAvailable();
            if (control != null) {
                control.release(ORDER_SOURCE, order.getNumber(), false);
            }
        }
        order.setStatus("CANCELLED");
        order.setCancelledBy(actors.resolve("SYSTEM"));
        order.setCancelReason(reason);
        orders.save(order);
        return order(number);
    }

    /** Bon de commande imprime (piece numerotee BC du lot F7). */
    public byte[] orderPdf(String number) {
        OrderView view = order(number);
        AccPurchaseOrder order = view.order();
        if ("DRAFT".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BadRequestException("Seule une commande validee s'imprime");
        }
        VoucherService.Issued issued = vouchers.issueAndPrint("BC", ORDER_SOURCE, order.getNumber(), order.getTotalTtc(), order.getSubject(),
                actors.resolve("SYSTEM"));
        AccSupplier supplier = view.supplier();
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Fournisseur").pair("Fournisseur", supplier == null ? order.getSupplierCode() : supplier.getName())
                .pair("NIU", supplier == null ? null : supplier.getNiu()).pair("Adresse", supplier == null ? null : supplier.getAddress())
                .pair("Commande", order.getNumber()).pair("Objet", order.getSubject()).pair("Date", order.getOrderDate())
                .pair("Livraison souhaitee", order.getExpectedDate()).pair("Etat de besoin", order.getNeedStatementNumber()));
        VoucherService.Section lines = new VoucherService.Section("Lignes").columns(List.of("N", "Designation", "Qte", "PU HT", "HT", "TVA %", "TVA"),
                List.of(true, false, true, true, true, true, true));
        view.lines().forEach(line -> lines.row(line.getLineNo(), line.getDesignation(), line.getQuantity().stripTrailingZeros().toPlainString(),
                VoucherService.money(line.getUnitPrice()), VoucherService.money(line.getAmountHt()), line.getVatRate().stripTrailingZeros().toPlainString(),
                VoucherService.money(line.getVatAmount())));
        lines.row("", "Total", "", "", VoucherService.money(order.getTotalHt()), "", VoucherService.money(order.getTotalVat()));
        sections.add(lines);
        return vouchers.render("Bon de commande", issued, sections, order.getTotalTtc(), null, List.of(
                new VoucherService.Signature("Prepare par", order.getCreatedBy(), VoucherService.date(order.getCreatedAt())),
                new VoucherService.Signature("Valide par", order.getValidatedBy(), order.getValidatedAt() == null ? null : VoucherService.date(order.getValidatedAt()))));
    }

    // ------------------------------------------------------------------ factures

    public record InvoiceRequest(String supplierCode, String supplierReference, String kind, LocalDate invoiceDate, LocalDate dueDate, String orderNumber,
                                 String originalNumber, String subject, String documentRef, Boolean closeOrder, BigDecimal withholdingAmount,
                                 List<LineInput> lines) {
    }

    public record InvoiceView(AccSupplierInvoice invoice, List<AccSupplierInvoiceLine> lines, List<AccSupplierPayment> payments, AccSupplier supplier) {
    }

    @Transactional(readOnly = true)
    public List<AccSupplierInvoice> invoices() {
        return invoices.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public InvoiceView invoice(String number) {
        AccSupplierInvoice invoice = invoiceOf(number);
        return new InvoiceView(invoice, invoiceLines.findAllByInvoiceIdOrderByLineNoAsc(invoice.getId()),
                payments.findAllByInvoiceNumberOrderByPaymentDateAsc(invoice.getNumber()), suppliers.findByCode(invoice.getSupplierCode()).orElse(null));
    }

    /**
     * Saisie d'une facture (ou d'un avoir) fournisseur en brouillon. Une facture sur commande sans
     * lignes reprend le reste a facturer de la commande. Le meme numero de facture d'un meme
     * fournisseur est refuse (doublon).
     */
    public InvoiceView createInvoice(InvoiceRequest request) {
        require("SUPPLIER_INVOICE:MANAGE");
        AccSupplier supplier = activeSupplier(request.supplierCode());
        String kind = "CREDIT_NOTE".equalsIgnoreCase(request.kind()) ? "CREDIT_NOTE" : "INVOICE";
        if (!StringUtils.hasText(request.supplierReference())) {
            throw new BadRequestException("Numero de la facture du fournisseur obligatoire");
        }
        String reference = request.supplierReference().trim();
        if (invoices.findBySupplierCodeAndKindAndSupplierReference(supplier.getCode(), kind, reference).isPresent()) {
            throw new ConflictException("supplier_invoice", "la " + ("CREDIT_NOTE".equals(kind) ? "facture d'avoir" : "facture") + " " + reference
                    + " du fournisseur " + supplier.getName() + " est deja saisie");
        }
        AccPurchaseOrder order = null;
        if (StringUtils.hasText(request.orderNumber())) {
            order = orderOf(request.orderNumber().trim());
            if (!order.getSupplierCode().equals(supplier.getCode())) {
                throw new BadRequestException("La commande " + order.getNumber() + " est d'un autre fournisseur");
            }
            if (!"VALIDATED".equals(order.getStatus()) && !"PARTIALLY_INVOICED".equals(order.getStatus())) {
                throw new BadRequestException("La commande " + order.getNumber() + " n'est pas validee ou est deja facturee (" + order.getStatus() + ")");
            }
        }
        AccSupplierInvoice original = null;
        if ("CREDIT_NOTE".equals(kind)) {
            if (!StringUtils.hasText(request.originalNumber())) {
                throw new BadRequestException("Un avoir indique la facture qu'il corrige");
            }
            original = invoiceOf(request.originalNumber().trim());
        }
        List<LineInput> inputs = request.lines();
        if ((inputs == null || inputs.isEmpty()) && order != null) {
            inputs = orderLines.findAllByOrderIdOrderByLineNoAsc(order.getId()).stream()
                    .filter(line -> line.getAmountHt().compareTo(line.getInvoicedHt()) > 0)
                    .map(line -> new LineInput(line.getDesignation(), BigDecimal.ONE, line.getAmountHt().subtract(line.getInvoicedHt()), line.getNatureCode(),
                            line.getAccountNumber(), line.getVatRate(), line.getSectionCode(), line.getLineNo())).toList();
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new BadRequestException("Une facture a au moins une ligne");
        }
        LocalDate date = request.invoiceDate() == null ? LocalDate.now() : request.invoiceDate();
        AccSupplierInvoice invoice = invoices.save(AccSupplierInvoice.builder().number(nextNumber("CREDIT_NOTE".equals(kind) ? "AF" : "FF", date, false))
                .kind(kind).supplierCode(supplier.getCode()).supplierReference(reference).invoiceDate(date)
                .dueDate(request.dueDate() != null ? request.dueDate() : date.plusDays(supplier.getPaymentTermsDays()))
                .orderNumber(order == null ? null : order.getNumber()).originalNumber(original == null ? null : original.getNumber())
                .subject(request.subject()).documentRef(request.documentRef()).closeOrder(Boolean.TRUE.equals(request.closeOrder()))
                .createdBy(actors.resolve("SYSTEM")).build());
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        int no = 1;
        for (LineInput input : inputs) {
            if (!StringUtils.hasText(input.designation()) || input.unitPrice() == null || input.unitPrice().signum() <= 0) {
                throw new BadRequestException("Ligne " + no + " : designation et montant positifs obligatoires");
            }
            BigDecimal quantity = input.quantity() == null || input.quantity().signum() <= 0 ? BigDecimal.ONE : input.quantity();
            BigDecimal amount = money(input.unitPrice().multiply(quantity));
            BigDecimal rate = Boolean.TRUE.equals(supplier.getVatExempt()) ? BigDecimal.ZERO : (input.vatRate() == null ? defaultVatRate() : input.vatRate());
            BigDecimal lineVat = money(amount.multiply(rate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            String account = account(input.natureCode(), input.accountNumber(), supplier);
            invoiceLines.save(AccSupplierInvoiceLine.builder().invoiceId(invoice.getId()).lineNo(no).designation(input.designation())
                    .natureCode(input.natureCode()).accountNumber(account).amountHt(amount).vatRate(rate).vatAmount(lineVat)
                    .vatAccount(vatAccount(account)).sectionCode(input.sectionCode()).orderLineNo(input.orderLineNo()).build());
            ht = ht.add(amount);
            vat = vat.add(lineVat);
            no++;
        }
        BigDecimal ttc = ht.add(vat);
        BigDecimal withholding = request.withholdingAmount() != null ? money(request.withholdingAmount())
                : money(("TTC".equals(supplier.getWithholdingBase()) ? ttc : ht).multiply(supplier.getWithholdingRate()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        if (withholding.signum() < 0 || withholding.compareTo(ttc) > 0) {
            throw new BadRequestException("La retenue a la source est comprise entre 0 et le total TTC");
        }
        invoice.setTotalHt(ht);
        invoice.setTotalVat(vat);
        invoice.setTotalTtc(ttc);
        invoice.setWithholdingAmount(withholding);
        invoice.setNetToPay(ttc.subtract(withholding));
        invoices.save(invoice);
        return invoice(invoice.getNumber());
    }

    /**
     * Comptabilisation au journal des achats : D charges HT (par section) ; D TVA recuperable ;
     * C fournisseur (net a payer, au nom du fournisseur, echeance) ; C retenue a la source. L'avoir
     * ecrit l'inverse et se lettre avec sa facture. Une facture sur commande realise l'engagement
     * budgetaire quand la commande est entierement facturee (ou close).
     */
    public InvoiceView postInvoice(String number) {
        require("SUPPLIER_INVOICE:MANAGE");
        AccSupplierInvoice invoice = invoiceOf(number);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new BadRequestException("La facture " + number + " est deja comptabilisee");
        }
        AccSupplier supplier = suppliers.findByCode(invoice.getSupplierCode()).orElseThrow();
        List<AccSupplierInvoiceLine> lines = invoiceLines.findAllByInvoiceIdOrderByLineNoAsc(invoice.getId());
        boolean credit = "CREDIT_NOTE".equals(invoice.getKind());
        String label = (credit ? "Avoir " : "Facture ") + supplier.getName() + " " + invoice.getSupplierReference();
        String letteringReference = credit && invoice.getOriginalNumber() != null ? invoice.getOriginalNumber() : invoice.getNumber();
        List<AccountingRequests.EntryLine> entry = new ArrayList<>();
        for (AccSupplierInvoiceLine line : lines) {
            entry.add(line(line.getAccountNumber(), null, line.getDesignation(), line.getAmountHt(), !credit, invoice.getNumber(), null, null, line.getSectionCode(), null));
            if (line.getVatAmount().signum() > 0) {
                entry.add(line(line.getVatAccount(), null, "TVA " + line.getDesignation(), line.getVatAmount(), !credit, invoice.getNumber(),
                        "TVA" + line.getVatRate().stripTrailingZeros().toPlainString(), line.getAmountHt(), null, null));
            }
        }
        entry.add(line(supplier.getAccountNumber(), supplier.getCode(), label, invoice.getNetToPay(), credit, letteringReference, null, null, null,
                credit ? null : invoice.getDueDate()));
        if (invoice.getWithholdingAmount().signum() > 0) {
            entry.add(line(supplier.getWithholdingAccount(), null, (supplier.getWithholdingLabel() == null ? "Retenue a la source" : supplier.getWithholdingLabel())
                    + " " + supplier.getName(), invoice.getWithholdingAmount(), credit, invoice.getNumber(), "RAS", credit ? null : invoice.getTotalHt(), null, null));
        }
        AccEntry posted = entries.post(settings.text("accounting.purchase.journal", "AC"), invoice.getInvoiceDate(), label, EntryOrigin.PLATFORM,
                credit ? "SUPPLIER_CREDIT_NOTE" : "SUPPLIER_INVOICE", invoice.getNumber(), entry, actors.resolve("SYSTEM"));
        if (posted.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture de la facture n'a pas pu etre validee : " + posted.getPendingDetail());
        }
        invoice.setEntryNumber(posted.getEntryNumber());
        invoice.setStatus("POSTED");
        invoice.setPostedBy(actors.resolve("SYSTEM"));
        invoice.setPostedAt(Instant.now());
        invoices.save(invoice);
        if (credit) {
            // L'avoir se lettre avec sa facture ; la facture d'origine voit son reste diminuer.
            lettering.autoLetterByReference(supplier.getAccountNumber(), supplier.getCode(), letteringReference);
            invoices.findByNumber(letteringReference).ifPresent(this::refreshPaidStatus);
            refreshPaidStatus(invoice);
        } else if (invoice.getOrderNumber() != null) {
            invoiceOrder(invoice, lines);
        }
        return invoice(number);
    }

    private void invoiceOrder(AccSupplierInvoice invoice, List<AccSupplierInvoiceLine> lines) {
        AccPurchaseOrder order = orderOf(invoice.getOrderNumber());
        List<AccPurchaseOrderLine> ordered = orderLines.findAllByOrderIdOrderByLineNoAsc(order.getId());
        for (AccSupplierInvoiceLine line : lines) {
            if (line.getOrderLineNo() == null) {
                continue;
            }
            ordered.stream().filter(item -> item.getLineNo().equals(line.getOrderLineNo())).findFirst().ifPresent(item -> {
                item.setInvoicedHt(item.getInvoicedHt().add(line.getAmountHt()));
                orderLines.save(item);
            });
        }
        order.setInvoicedHt(order.getInvoicedHt().add(invoice.getTotalHt()));
        boolean complete = Boolean.TRUE.equals(invoice.getCloseOrder()) || order.getInvoicedHt().compareTo(order.getTotalHt()) >= 0;
        order.setStatus(complete ? "INVOICED" : "PARTIALLY_INVOICED");
        orders.save(order);
        if (complete && Boolean.TRUE.equals(order.getBudgetCommitted())) {
            BudgetControl control = budgetControl.getIfAvailable();
            if (control != null) {
                control.settle(ORDER_SOURCE, order.getNumber(), order.getInvoicedHt());
            }
        }
    }

    /** Un brouillon se supprime ; une facture comptabilisee se corrige par un avoir. */
    public void deleteDraft(String number) {
        require("SUPPLIER_INVOICE:MANAGE");
        AccSupplierInvoice invoice = invoiceOf(number);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new BadRequestException("Seul un brouillon se supprime ; une facture comptabilisee se corrige par un avoir");
        }
        invoiceLines.deleteAllByInvoiceId(invoice.getId());
        invoices.delete(invoice);
    }

    // ------------------------------------------------------------------ reglements

    public record PaymentRequest(BigDecimal amount, LocalDate date, String treasuryAccount, String journalCode, String reference) {
    }

    /** Reglement : D fournisseur ; C tresorerie ; lettrage avec la facture des qu'elle est soldee. */
    public InvoiceView pay(String number, PaymentRequest request) {
        require("SUPPLIER_PAYMENT:MANAGE");
        AccSupplierInvoice invoice = invoiceOf(number);
        if (!"INVOICE".equals(invoice.getKind()) || (!"POSTED".equals(invoice.getStatus()) && !"PARTIALLY_PAID".equals(invoice.getStatus()))) {
            throw new BadRequestException("Seule une facture comptabilisee et non soldee se regle (" + invoice.getStatus() + ")");
        }
        if (request == null || !StringUtils.hasText(request.treasuryAccount()) || !StringUtils.hasText(request.journalCode())) {
            throw new BadRequestException("Compte et journal de tresorerie obligatoires");
        }
        AccSupplier supplier = suppliers.findByCode(invoice.getSupplierCode()).orElseThrow();
        BigDecimal remaining = remaining(invoice);
        BigDecimal amount = request.amount() == null ? remaining : money(request.amount());
        if (amount.signum() <= 0 || amount.compareTo(remaining) > 0) {
            throw new BadRequestException("Le reglement est positif et au plus le reste a payer (" + plain(remaining) + ")");
        }
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        String label = "Reglement " + supplier.getName() + " " + invoice.getSupplierReference();
        AccEntry posted = entries.post(request.journalCode().trim(), date, label, EntryOrigin.PLATFORM, "SUPPLIER_PAYMENT", invoice.getNumber(), List.of(
                line(supplier.getAccountNumber(), supplier.getCode(), label, amount, true, invoice.getNumber(), null, null, null, null),
                line(request.treasuryAccount().trim(), null, label, amount, false, StringUtils.hasText(request.reference()) ? request.reference() : invoice.getNumber(),
                        null, null, null, null)), actors.resolve("SYSTEM"));
        if (posted.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture du reglement n'a pas pu etre validee : " + posted.getPendingDetail());
        }
        payments.save(AccSupplierPayment.builder().invoiceNumber(invoice.getNumber()).supplierCode(supplier.getCode()).paymentDate(date).amount(amount)
                .treasuryAccount(request.treasuryAccount().trim()).journalCode(request.journalCode().trim()).reference(request.reference())
                .entryNumber(posted.getEntryNumber()).createdBy(actors.resolve("SYSTEM")).build());
        lettering.autoLetterByReference(supplier.getAccountNumber(), supplier.getCode(), invoice.getNumber());
        refreshPaidStatus(invoice);
        return invoice(number);
    }

    /** Reste a payer : net a payer moins reglements et avoirs comptabilises sur la facture. */
    BigDecimal remaining(AccSupplierInvoice invoice) {
        BigDecimal paid = payments.findAllByInvoiceNumberOrderByPaymentDateAsc(invoice.getNumber()).stream().map(AccSupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credited = invoices.findAllBySupplierCodeOrderByInvoiceDateAsc(invoice.getSupplierCode()).stream()
                .filter(other -> "CREDIT_NOTE".equals(other.getKind()) && invoice.getNumber().equals(other.getOriginalNumber()) && !"DRAFT".equals(other.getStatus()))
                .map(AccSupplierInvoice::getNetToPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        return invoice.getNetToPay().subtract(paid).subtract(credited).max(BigDecimal.ZERO);
    }

    private void refreshPaidStatus(AccSupplierInvoice invoice) {
        if ("DRAFT".equals(invoice.getStatus()) || "CANCELLED".equals(invoice.getStatus())) {
            return;
        }
        if ("CREDIT_NOTE".equals(invoice.getKind())) {
            invoice.setStatus("PAID");
        } else {
            BigDecimal remaining = remaining(invoice);
            invoice.setPaidAmount(invoice.getNetToPay().subtract(remaining));
            invoice.setStatus(remaining.signum() == 0 ? "PAID" : invoice.getPaidAmount().signum() > 0 ? "PARTIALLY_PAID" : "POSTED");
        }
        invoices.save(invoice);
    }

    // ------------------------------------------------------------------ etats

    public record Payable(String number, String supplierCode, String supplierName, String supplierReference, LocalDate invoiceDate, LocalDate dueDate,
                          BigDecimal netToPay, BigDecimal remaining, long daysLate) {
    }

    /** Echeancier des factures a payer, par date d'echeance (jusqu'a une date, ou toutes). */
    @Transactional(readOnly = true)
    public List<Payable> payables(LocalDate until) {
        LocalDate today = LocalDate.now();
        List<Payable> result = new ArrayList<>();
        for (AccSupplierInvoice invoice : invoices.findAllByStatusInOrderByDueDateAsc(List.of("POSTED", "PARTIALLY_PAID"))) {
            if (!"INVOICE".equals(invoice.getKind()) || (until != null && invoice.getDueDate().isAfter(until))) {
                continue;
            }
            BigDecimal remaining = remaining(invoice);
            if (remaining.signum() == 0) {
                continue;
            }
            String name = suppliers.findByCode(invoice.getSupplierCode()).map(AccSupplier::getName).orElse(invoice.getSupplierCode());
            result.add(new Payable(invoice.getNumber(), invoice.getSupplierCode(), name, invoice.getSupplierReference(), invoice.getInvoiceDate(),
                    invoice.getDueDate(), invoice.getNetToPay(), remaining, Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), today))));
        }
        return result;
    }

    public record StatementLine(LocalDate date, String entryNumber, String journal, String label, String reference, BigDecimal debit, BigDecimal credit,
                                BigDecimal balance) {
    }

    public record Statement(AccSupplier supplier, LocalDate from, LocalDate to, BigDecimal opening, List<StatementLine> lines, BigDecimal closing) {
    }

    /** Releve du fournisseur (grand livre auxiliaire) : solde crediteur = du au fournisseur. */
    @Transactional(readOnly = true)
    public Statement statement(String code, LocalDate from, LocalDate to) {
        AccSupplier supplier = suppliers.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + code));
        LocalDate start = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BigDecimal opening = Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date < ?", BigDecimal.class,
                supplier.getAccountNumber(), supplier.getCode(), Date.valueOf(start))).orElse(BigDecimal.ZERO);
        List<StatementLine> lines = new ArrayList<>();
        BigDecimal[] running = {opening};
        jdbc.query("SELECT e.entry_date, e.entry_number, e.journal_code, l.label, l.reference, l.debit, l.credit FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                + "ORDER BY e.entry_date, e.id, l.line_no", rs -> {
            running[0] = running[0].add(rs.getBigDecimal("credit")).subtract(rs.getBigDecimal("debit"));
            lines.add(new StatementLine(rs.getDate("entry_date").toLocalDate(), rs.getString("entry_number"), rs.getString("journal_code"), rs.getString("label"),
                    rs.getString("reference"), rs.getBigDecimal("debit"), rs.getBigDecimal("credit"), running[0]));
        }, supplier.getAccountNumber(), supplier.getCode(), Date.valueOf(start), Date.valueOf(end));
        return new Statement(supplier, start, end, opening, lines, running[0]);
    }

    public record Withholding(String invoiceNumber, String supplierCode, String supplierName, String niu, String supplierReference, LocalDate invoiceDate,
                              BigDecimal base, BigDecimal rate, BigDecimal amount, String account) {
    }

    public record WithholdingStatement(String period, List<Withholding> lines, BigDecimal total, Map<String, BigDecimal> accountMovements) {
    }

    /** Etat mensuel des retenues a la source operees sur les fournisseurs, a reverser a l'Etat. */
    @Transactional(readOnly = true)
    public WithholdingStatement withholdings(String period) {
        YearMonth month;
        try {
            month = YearMonth.parse(period.trim());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Periode attendue au format AAAA-MM");
        }
        List<Withholding> lines = new ArrayList<>();
        Set<String> accounts = new TreeSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AccSupplier supplier : suppliers.findAllByOrderByNameAsc()) {
            for (AccSupplierInvoice invoice : invoices.findAllBySupplierCodeOrderByInvoiceDateAsc(supplier.getCode())) {
                if ("DRAFT".equals(invoice.getStatus()) || invoice.getWithholdingAmount().signum() == 0
                        || !YearMonth.from(invoice.getInvoiceDate()).equals(month)) {
                    continue;
                }
                BigDecimal sign = "CREDIT_NOTE".equals(invoice.getKind()) ? BigDecimal.ONE.negate() : BigDecimal.ONE;
                BigDecimal base = "TTC".equals(supplier.getWithholdingBase()) ? invoice.getTotalTtc() : invoice.getTotalHt();
                lines.add(new Withholding(invoice.getNumber(), supplier.getCode(), supplier.getName(), supplier.getNiu(), invoice.getSupplierReference(),
                        invoice.getInvoiceDate(), base.multiply(sign), supplier.getWithholdingRate(), invoice.getWithholdingAmount().multiply(sign),
                        supplier.getWithholdingAccount()));
                total = total.add(invoice.getWithholdingAmount().multiply(sign));
                accounts.add(supplier.getWithholdingAccount());
            }
        }
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, BigDecimal> movements = new LinkedHashMap<>();
        for (String account : accounts) {
            movements.put(account, jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                    + "WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ?", BigDecimal.class,
                    account, Date.valueOf(month.atDay(1)), Date.valueOf(month.atEndOfMonth())));
        }
        return new WithholdingStatement(month.toString(), lines, total, movements);
    }

    @Transactional(readOnly = true)
    public byte[] withholdingsCsv(String period) {
        WithholdingStatement statement = withholdings(period);
        StringBuilder out = new StringBuilder("﻿Periode;Facture;Fournisseur;NIU;Reference fournisseur;Date;Base;Taux;Retenue;Compte\n");
        statement.lines().forEach(line -> out.append(String.join(";", statement.period(), line.invoiceNumber(), line.supplierName().replace(";", ","),
                line.niu() == null ? "" : line.niu(), line.supplierReference(), String.valueOf(line.invoiceDate()), plain(line.base()),
                line.rate().stripTrailingZeros().toPlainString(), plain(line.amount()), line.account())).append("\n"));
        out.append(";;;;;;;Total;").append(plain(statement.total())).append(";\n");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ outils

    private List<BudgetControl.Spend> spends(AccPurchaseOrder order) {
        return orderLines.findAllByOrderIdOrderByLineNoAsc(order.getId()).stream()
                .map(line -> new BudgetControl.Spend(line.getNatureCode(), line.getAccountNumber(), line.getSectionCode(), line.getAmountHt())).toList();
    }

    /** Compte de charge : celui de la nature d'operation, sinon celui saisi, sinon celui par defaut du fournisseur. */
    private String account(String natureCode, String accountNumber, AccSupplier supplier) {
        if (StringUtils.hasText(accountNumber)) {
            return accountNumber.trim();
        }
        if (StringUtils.hasText(natureCode)) {
            return natures.findByCode(natureCode.trim()).map(nature -> nature.getAccountNumber())
                    .filter(StringUtils::hasText).orElseThrow(() -> new BadRequestException("Nature d'operation sans compte : " + natureCode));
        }
        if (StringUtils.hasText(supplier.getDefaultAccount())) {
            return supplier.getDefaultAccount();
        }
        throw new BadRequestException("Indiquez la nature d'operation ou le compte de charge de chaque ligne");
    }

    /** TVA recuperable : 4454 sur les services exterieurs (62, 63), 4451 sur immobilisations (2), 4452 sinon. */
    private String vatAccount(String account) {
        if (account.startsWith("2")) {
            return settings.text("accounting.purchase.vat-account-fixed-assets", "4451");
        }
        if (account.startsWith("62") || account.startsWith("63")) {
            return settings.text("accounting.purchase.vat-account-services", "4454");
        }
        return settings.text("accounting.purchase.vat-account", "4452");
    }

    private BigDecimal defaultVatRate() {
        try {
            return new BigDecimal(settings.text("accounting.purchase.default-vat-rate", "18").trim());
        } catch (RuntimeException exception) {
            return new BigDecimal("18");
        }
    }

    private AccountingRequests.EntryLine line(String account, String thirdParty, String label, BigDecimal amount, boolean debit, String reference,
                                              String taxCode, BigDecimal taxBase, String section, LocalDate dueDate) {
        Map<String, String> analytic = null;
        String axis = settings.text("accounting.purchase.analytic-axis", "");
        if (StringUtils.hasText(section) && StringUtils.hasText(axis)) {
            analytic = Map.of(axis, section);
        }
        return new AccountingRequests.EntryLine(account, thirdParty, label, debit ? amount : null, debit ? null : amount, null, null, null, taxCode, taxBase,
                dueDate, reference, analytic);
    }

    private String nextNumber(String prefix, LocalDate date, boolean order) {
        String base = prefix + "-" + date.format(MONTH) + "-";
        long count = order ? orders.countByNumberStartingWith(base) : invoices.countByNumberStartingWith(base);
        return base + String.format("%04d", count + 1);
    }

    /**
     * Fournisseur d'une commande issue d'un etat de besoin : celui indique (code comptable, ou code
     * d'une fiche d'inventaire), sinon la fiche d'inventaire portee par les lignes retenues.
     */
    private AccSupplier needSupplier(String supplierCode, List<FundNeedLine> lines) {
        if (StringUtils.hasText(supplierCode)) {
            if (suppliers.findByCode(supplierCode.trim().toUpperCase(Locale.ROOT)).isEmpty()
                    && LinkedSupplier.find(inventorySuppliers, supplierCode).isPresent()) {
                return activeSupplier(linkedSupplier(inventorySupplier(supplierCode)).getCode());
            }
            return activeSupplier(supplierCode);
        }
        Set<String> codes = lines.stream().map(FundNeedLine::getSupplierCode).filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (codes.isEmpty()) {
            throw new BadRequestException("Indiquez le fournisseur : aucune ligne retenue n'est reliee a une fiche fournisseur");
        }
        if (codes.size() > 1 || lines.stream().anyMatch(line -> !StringUtils.hasText(line.getSupplierCode()))) {
            throw new BadRequestException("Les lignes retenues ne portent pas toutes le meme fournisseur (" + String.join(", ", codes)
                    + ") : indiquez le fournisseur ou choisissez les lignes");
        }
        return activeSupplier(linkedSupplier(inventorySupplier(codes.iterator().next())).getCode());
    }

    private AccSupplier activeSupplier(String code) {
        AccSupplier supplier = suppliers.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Fournisseur inconnu : " + code));
        if (!"ACTIVE".equals(supplier.getStatus())) {
            throw new BadRequestException("Le fournisseur " + supplier.getName() + " est inactif");
        }
        return supplier;
    }

    private AccPurchaseOrder orderOf(String number) {
        return orders.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + number));
    }

    private AccSupplierInvoice invoiceOf(String number) {
        return invoices.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Facture fournisseur introuvable : " + number));
    }

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Action reservee : " + authority);
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
