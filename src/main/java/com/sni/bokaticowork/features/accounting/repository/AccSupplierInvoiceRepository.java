package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccSupplierInvoiceRepository extends JpaRepository<AccSupplierInvoice, Long> {

    java.util.Optional<AccSupplierInvoice> findByNumber(String number);

    java.util.List<AccSupplierInvoice> findTop200ByOrderByCreatedAtDesc();

    java.util.List<AccSupplierInvoice> findAllBySupplierCodeOrderByInvoiceDateAsc(String supplierCode);

    java.util.List<AccSupplierInvoice> findAllByStatusInOrderByDueDateAsc(java.util.Collection<String> statuses);

    java.util.List<AccSupplierInvoice> findAllByOrderNumber(String orderNumber);

    java.util.Optional<AccSupplierInvoice> findBySupplierCodeAndKindAndSupplierReference(String supplierCode, String kind, String supplierReference);

    long countByNumberStartingWith(String prefix);
}
