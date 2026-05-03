package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/payment-links")
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    @GetMapping("/{token}")
    public ResponseEntity<PaymentIntentResponse> resolve(@PathVariable String token) {
        return ResponseEntity.ok(paymentLinkService.resolve(token));
    }
}
