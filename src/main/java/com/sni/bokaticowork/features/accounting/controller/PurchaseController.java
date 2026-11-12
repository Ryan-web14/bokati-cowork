package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccPurchaseOrder;
import com.sni.bokaticowork.features.accounting.model.AccSupplier;
import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoice;
import com.sni.bokaticowork.features.accounting.service.implementation.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Achats : fournisseurs, commandes, factures et reglements fournisseurs (lot F13). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchases;

    public record FromNeedRequest(String needNumber, String supplierCode, List<Integer> lineNos) {
    }

    public record Reason(String reason) {
    }

    // ------------------------------------------------------------------ fournisseurs

    @GetMapping("/suppliers")
    public ResponseEntity<List<AccSupplier>> suppliers() {
        return ResponseEntity.ok(purchases.suppliers());
    }

    @Audited(module = "ACCOUNTING", action = "SUPPLIER_SAVE", ressource = "acc_supplier")
    @PostMapping("/suppliers")
    public ResponseEntity<AccSupplier> saveSupplier(@RequestBody AccSupplier supplier) {
        return ResponseEntity.ok(purchases.saveSupplier(supplier));
    }

    /** Fournisseur comptable d'une fiche fournisseur d'inventaire : retrouve, ou cree depuis la fiche. */
    @Audited(module = "ACCOUNTING", action = "SUPPLIER_FROM_INVENTORY", ressource = "acc_supplier")
    @PostMapping("/suppliers/from-inventory/{inventoryCode}")
    public ResponseEntity<AccSupplier> supplierFromInventory(@PathVariable String inventoryCode) {
        return ResponseEntity.ok(purchases.supplierFromInventory(inventoryCode));
    }

    @GetMapping("/suppliers/{code}/statement")
    public ResponseEntity<PurchaseService.Statement> statement(@PathVariable String code,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(purchases.statement(code, from, to));
    }

    // ------------------------------------------------------------------ commandes

    @GetMapping("/orders")
    public ResponseEntity<List<AccPurchaseOrder>> orders() {
        return ResponseEntity.ok(purchases.orders());
    }

    @GetMapping("/orders/{number}")
    public ResponseEntity<PurchaseService.OrderView> order(@PathVariable String number) {
        return ResponseEntity.ok(purchases.order(number));
    }

    @Audited(module = "ACCOUNTING", action = "PURCHASE_ORDER_CREATE", ressource = "acc_purchase_order")
    @PostMapping("/orders")
    public ResponseEntity<PurchaseService.OrderView> createOrder(@RequestBody PurchaseService.OrderRequest request) {
        return ResponseEntity.ok(purchases.createOrder(request));
    }

    @Audited(module = "ACCOUNTING", action = "PURCHASE_ORDER_FROM_NEED", ressource = "acc_purchase_order")
    @PostMapping("/orders/from-need")
    public ResponseEntity<PurchaseService.OrderView> fromNeed(@RequestBody FromNeedRequest request) {
        return ResponseEntity.ok(purchases.orderFromNeed(request.needNumber(), request.supplierCode(), request.lineNos()));
    }

    @Audited(module = "ACCOUNTING", action = "PURCHASE_ORDER_VALIDATE", ressource = "acc_purchase_order")
    @PatchMapping("/orders/{number}/validate")
    public ResponseEntity<PurchaseService.OrderView> validate(@PathVariable String number, @RequestBody(required = false) Reason override) {
        return ResponseEntity.ok(purchases.validateOrder(number, override == null ? null : override.reason()));
    }

    @Audited(module = "ACCOUNTING", action = "PURCHASE_ORDER_CANCEL", ressource = "acc_purchase_order")
    @PatchMapping("/orders/{number}/cancel")
    public ResponseEntity<PurchaseService.OrderView> cancel(@PathVariable String number, @RequestBody Reason reason) {
        return ResponseEntity.ok(purchases.cancelOrder(number, reason.reason()));
    }

    @GetMapping("/orders/{number}/pdf")
    public ResponseEntity<byte[]> orderPdf(@PathVariable String number) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + number + ".pdf\"").body(purchases.orderPdf(number));
    }

    // ------------------------------------------------------------------ factures

    @GetMapping("/invoices")
    public ResponseEntity<List<AccSupplierInvoice>> invoices() {
        return ResponseEntity.ok(purchases.invoices());
    }

    @GetMapping("/invoices/{number}")
    public ResponseEntity<PurchaseService.InvoiceView> invoice(@PathVariable String number) {
        return ResponseEntity.ok(purchases.invoice(number));
    }

    @Audited(module = "ACCOUNTING", action = "SUPPLIER_INVOICE_CREATE", ressource = "acc_supplier_invoice")
    @PostMapping("/invoices")
    public ResponseEntity<PurchaseService.InvoiceView> createInvoice(@RequestBody PurchaseService.InvoiceRequest request) {
        return ResponseEntity.ok(purchases.createInvoice(request));
    }

    @Audited(module = "ACCOUNTING", action = "SUPPLIER_INVOICE_POST", ressource = "acc_supplier_invoice")
    @PostMapping("/invoices/{number}/post")
    public ResponseEntity<PurchaseService.InvoiceView> post(@PathVariable String number) {
        return ResponseEntity.ok(purchases.postInvoice(number));
    }

    @Audited(module = "ACCOUNTING", action = "SUPPLIER_INVOICE_DELETE", ressource = "acc_supplier_invoice")
    @DeleteMapping("/invoices/{number}")
    public ResponseEntity<Void> delete(@PathVariable String number) {
        purchases.deleteDraft(number);
        return ResponseEntity.noContent().build();
    }

    @Audited(module = "ACCOUNTING", action = "SUPPLIER_PAYMENT", ressource = "acc_supplier_payment")
    @PostMapping("/invoices/{number}/pay")
    public ResponseEntity<PurchaseService.InvoiceView> pay(@PathVariable String number, @RequestBody PurchaseService.PaymentRequest request) {
        return ResponseEntity.ok(purchases.pay(number, request));
    }

    // ------------------------------------------------------------------ etats

    @GetMapping("/payables")
    public ResponseEntity<List<PurchaseService.Payable>> payables(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until) {
        return ResponseEntity.ok(purchases.payables(until));
    }

    @GetMapping("/withholdings")
    public ResponseEntity<PurchaseService.WithholdingStatement> withholdings(@RequestParam String period) {
        return ResponseEntity.ok(purchases.withholdings(period));
    }

    @GetMapping("/withholdings/export")
    public ResponseEntity<byte[]> withholdingsCsv(@RequestParam String period) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"retenues-" + period + ".csv\"").body(purchases.withholdingsCsv(period));
    }
}
