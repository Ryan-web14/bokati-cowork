package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDocumentDetailsRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.service.implementation.KycDocumentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/kyc/documents/admin")
public class KycDocumentAdminController {

    private final KycDocumentUploadService uploadService;

    /**
     * Upload a KYC document on behalf of any owner (admin only).
     *
     * Multipart form fields:
     *  - ownerType       : MEMBER | CUSTOMER | BUSINESS
     *  - ownerCode       : the owner's public code
     *  - documentTypeCode: e.g. CNI, PASSEPORT, NIU
     *  - side            : FRONT (default) | BACK
     *  - documentNumber  : (optional · enforced per type)
     *  - issueDate       : yyyy-MM-dd (optional)
     *  - expiryDate      : yyyy-MM-dd (optional)
     *  - file            : the document file
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycDocumentResponse> upload(
            @RequestParam DocumentOwnerType ownerType,
            @RequestParam String ownerCode,
            @RequestParam String documentTypeCode,
            @RequestParam(defaultValue = "FRONT") String side,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam("file") MultipartFile file) {
        KycDocumentResponse response = uploadService.adminUpload(
                ownerType, ownerCode, documentTypeCode, side,
                documentNumber, issueDate, expiryDate, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Enrich an existing KYC document with metadata (documentNumber, dates).
     * Useful when the document was uploaded first, then details are added after review.
     */
    @PatchMapping("/{kycDocumentId}/details")
    public ResponseEntity<KycDocumentResponse> updateDetails(
            @PathVariable Long kycDocumentId,
            @RequestBody KycDocumentDetailsRequest request) {
        KycDocumentResponse response = uploadService.updateDetails(
                kycDocumentId,
                request.getDocumentNumber(),
                request.getIssueDate(),
                request.getExpiryDate());
        return ResponseEntity.ok(response);
    }
}
