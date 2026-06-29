package com.sni.bokaticowork.features.contract.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.contract.dto.request.RequestContractSigningRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractSigningResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractSigningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContractSigningController {

    private final ContractSigningService signingService;

    @Audited(module = "CONTRACT", action = "REQUEST_SIGNING", ressource = "contract")
    @PostMapping(ApiPath.V1 + "/contracts/{contractCode}/request-signing")
    public ResponseEntity<ContractSigningResponse> requestSigning(
            @PathVariable String contractCode,
            @Valid @RequestBody RequestContractSigningRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(signingService.requestSigning(contractCode, request));
    }

    @GetMapping(ApiPath.V1 + "/public/contracts/sign/{token}")
    public ResponseEntity<ContractSigningResponse> getSigningDetails(@PathVariable UUID token) {
        return ResponseEntity.ok(signingService.getSigningDetails(token));
    }

    @PostMapping(ApiPath.V1 + "/public/contracts/sign/{token}")
    public ResponseEntity<ContractSigningResponse> sign(
            @PathVariable UUID token,
            @Valid @RequestBody SignContractRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(signingService.sign(token, request, ipAddress, userAgent));
    }
}
