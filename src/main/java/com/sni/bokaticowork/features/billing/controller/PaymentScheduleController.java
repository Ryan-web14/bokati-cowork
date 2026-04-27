package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.request.CreatePaymentScheduleRequest;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class PaymentScheduleController {

    private final PaymentScheduleService paymentScheduleService;

    @PostMapping("/invoices/{documentNumber}/schedule")
    public ResponseEntity<PaymentScheduleResponse> createSchedule(
            @PathVariable String documentNumber,
            @Valid @RequestBody CreatePaymentScheduleRequest request) {
        return ResponseEntity.ok(paymentScheduleService.createSchedule(documentNumber, request));
    }

    @GetMapping("/invoices/{documentNumber}/schedule")
    public ResponseEntity<PaymentScheduleResponse> getScheduleByDocument(@PathVariable String documentNumber) {
        return ResponseEntity.ok(paymentScheduleService.getScheduleByDocument(documentNumber));
    }

    @GetMapping("/schedules/{scheduleNumber}")
    public ResponseEntity<PaymentScheduleResponse> getSchedule(@PathVariable String scheduleNumber) {
        return ResponseEntity.ok(paymentScheduleService.getSchedule(scheduleNumber));
    }

    @PostMapping("/schedules/installments/{installmentNumber}/pay")
    public ResponseEntity<PaymentScheduleResponse> payInstallment(
            @PathVariable String installmentNumber,
            @Valid @RequestBody PayInvoiceRequest request) {
        return ResponseEntity.ok(paymentScheduleService.payInstallment(installmentNumber, request));
    }

    @DeleteMapping("/schedules/{scheduleNumber}")
    public ResponseEntity<PaymentScheduleResponse> cancelSchedule(@PathVariable String scheduleNumber) {
        return ResponseEntity.ok(paymentScheduleService.cancelSchedule(scheduleNumber));
    }
}