package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccSupplierInvoiceLineRepository extends JpaRepository<AccSupplierInvoiceLine, Long> {

    java.util.List<AccSupplierInvoiceLine> findAllByInvoiceIdOrderByLineNoAsc(Long invoiceId);

    void deleteAllByInvoiceId(Long invoiceId);
}
