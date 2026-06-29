package com.sni.bokaticowork.features.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.service.implementation.PaymentReceiptServiceImpl;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final BillingDocumentService billingDocumentService;
    private final PaymentReceiptService paymentReceiptService;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PawapayDepositRepository pawapayDepositRepository;
    private final ObjectMapper objectMapper;
    private final Locale appLocale;

    @GetMapping("/doc/{documentNumber}")
    public String verifyDocument(@PathVariable String documentNumber, Model model) {
        BillingDocumentResponse document;
        try {
            document = billingDocumentService.get(documentNumber);
        } catch (Exception ex) {
            model.addAttribute("ref", documentNumber);
            model.addAttribute("type", "document");
            return "verify/not-found";
        }
        model.addAttribute("document", document);
        model.addAttribute("fmt", new BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter(document.currency(), objectMapper, appLocale));
        model.addAttribute("generatedAt", LocalDate.now());
        try {
            model.addAttribute("payments", buildPaymentInfos(documentNumber));
        } catch (Exception ignored) {
            model.addAttribute("payments", List.of());
        }
        return "verify/document";
    }

    private List<BillingDocumentPdfServiceImpl.PaymentInfo> buildPaymentInfos(String documentNumber) {
        return paymentAllocationRepository.findAllByBillingDocumentNumberFetchTransaction(documentNumber)
                .stream()
                .map(allocation -> {
                    var tx = allocation.getPaymentTransaction();
                    String depositId = null;
                    String payerPhone = null;
                    if (tx.getPaymentMethod() == PaymentMethod.MOBILE_MONEY) {
                        PawapayDeposit deposit = pawapayDepositRepository
                                .findByTransactionNumber(tx.getTransactionNumber())
                                .orElse(null);
                        if (deposit != null) {
                            depositId = deposit.getDepositId();
                            payerPhone = deposit.getPhoneNumber();
                        }
                    }
                    return new BillingDocumentPdfServiceImpl.PaymentInfo(
                            tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null,
                            tx.getProvider(),
                            tx.getProviderReference(),
                            depositId,
                            payerPhone,
                            allocation.getAllocatedAmount(),
                            tx.getCurrency(),
                            tx.getPaidAt()
                    );
                })
                .toList();
    }

    @GetMapping("/receipt/{receiptNumber}")
    public String verifyReceipt(@PathVariable String receiptNumber, Model model) {
        try {
            PaymentReceiptResponse receipt = paymentReceiptService.getByReceiptNumber(receiptNumber);
            model.addAttribute("receipt", receipt);
            model.addAttribute("fmt", new PaymentReceiptServiceImpl.PaymentReceiptTemplateFormatter(receipt.currency(), objectMapper, appLocale));
            model.addAttribute("generatedAt", LocalDate.now());
            return "verify/receipt";
        } catch (Exception ex) {
            model.addAttribute("ref", receiptNumber);
            model.addAttribute("type", "reçu");
            return "verify/not-found";
        }
    }
}