package com.sni.bokaticowork.features.portal.document.contract.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.document.contract.dto.request.ClientContractSignRequest;
import com.sni.bokaticowork.features.portal.document.contract.dto.response.ClientContractResponse;
import com.sni.bokaticowork.features.portal.document.contract.dto.response.ClientContractSummaryResponse;
import com.sni.bokaticowork.features.portal.document.contract.service.ClientContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/client/documents/contracts")
@RequiredArgsConstructor
public class ClientContractController {

    private final ClientContextService clientContextService;
    private final ClientContractService clientContractService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ClientContractSummaryResponse>> listContracts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientContractService.listContracts(member, pageable));
    }

    @GetMapping("/{contractCode}")
    public ResponseEntity<ClientContractResponse> getContract(@PathVariable String contractCode) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientContractService.getContract(member, contractCode));
    }

    @GetMapping("/{contractCode}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String contractCode) {
        Member member = clientContextService.getAuthenticatedMember();
        DocumentFileResult result = clientContractService.downloadPdf(member, contractCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .body(result.content());
    }

    @PostMapping("/{contractCode}/sign")
    public ResponseEntity<ClientContractResponse> signContract(
            @PathVariable String contractCode,
            @Valid @RequestBody ClientContractSignRequest request,
            HttpServletRequest httpRequest) {
        Member member = clientContextService.getAuthenticatedMember();
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(clientContractService.signContract(member, contractCode, request, ip, ua));
    }
}
