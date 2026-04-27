package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleResponse;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;

public interface PaymentScheduleService {
    PaymentScheduleResponse createSchedule(String documentNumber, CreatePaymentScheduleRequest request);
    PaymentScheduleResponse getSchedule(String scheduleNumber);
    PaymentScheduleResponse getScheduleByDocument(String documentNumber);
    PaymentScheduleResponse payInstallment(String installmentNumber, PayInvoiceRequest request);
    PaymentScheduleResponse cancelSchedule(String scheduleNumber);
    int markOverdueInstallments();
}