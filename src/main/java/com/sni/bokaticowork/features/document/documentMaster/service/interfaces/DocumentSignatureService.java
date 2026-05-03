package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateDocumentSignatureRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SignDocumentRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentSignatureResponse;

import java.util.List;

public interface DocumentSignatureService {
    DocumentSignatureResponse requestSignature(String documentCode, CreateDocumentSignatureRequest request);
    DocumentSignatureResponse sign(String documentCode, Long signatureId, SignDocumentRequest request, String ipAddress, String userAgent);
    List<DocumentSignatureResponse> list(String documentCode);
}
