package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccSupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccSupplierPaymentRepository extends JpaRepository<AccSupplierPayment, Long> {

    java.util.List<AccSupplierPayment> findAllByInvoiceNumberOrderByPaymentDateAsc(String invoiceNumber);
}
