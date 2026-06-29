package com.sni.bokaticowork.features.portal.document.kyc.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycCompletionResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycDocumentResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycRequirementResponse;
import com.sni.bokaticowork.features.portal.document.kyc.dto.response.ClientKycStatusResponse;
import com.sni.bokaticowork.features.portal.document.kyc.service.ClientKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/client/documents/kyc")
@RequiredArgsConstructor
public class ClientKycController {

    private final ClientContextService clientContextService;
    private final ClientKycService clientKycService;

    @GetMapping
    public ResponseEntity<ClientKycStatusResponse> getMyCase() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.getMyCase(member));
    }

    @GetMapping("/requirements")
    public ResponseEntity<List<ClientKycRequirementResponse>> getRequirements() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.getRequirements(member));
    }

    @GetMapping("/completion")
    public ResponseEntity<ClientKycCompletionResponse> getCompletion() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.getCompletion(member));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<ClientKycDocumentResponse>> listDocuments() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.listDocuments(member));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientKycDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "side", defaultValue = "FRONT") String side) {
        Member member = clientContextService.getAuthenticatedMember();
        ClientKycDocumentResponse response = clientKycService.uploadDocument(
                member, documentType, documentNumber, issueDate, expiryDate, file, side);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientKycDocumentResponse> uploadDocumentJson() {
        throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                "Document upload requires Content-Type: multipart/form-data with a 'file' part. "
                        + "JSON body is not supported for file uploads.");
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<ClientKycDocumentResponse> getDocument(@PathVariable Long id) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.getDocument(member, id));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        Member member = clientContextService.getAuthenticatedMember();
        DocumentFileResult result = clientKycService.downloadDocument(member, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .body(result.content());
    }

    @PutMapping(value = "/documents/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientKycDocumentResponse> resubmitDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.resubmitDocument(member, id, file));
    }

    @PutMapping(value = "/documents/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientKycDocumentResponse> resubmitDocumentJson(@PathVariable Long id) {
        throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                "Document resubmission requires Content-Type: multipart/form-data with a 'file' part. "
                        + "JSON body is not supported for file uploads.");
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        Member member = clientContextService.getAuthenticatedMember();
        clientKycService.deleteDocument(member, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit")
    public ResponseEntity<ClientKycStatusResponse> submitCase() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientKycService.submitCase(member));
    }
}
