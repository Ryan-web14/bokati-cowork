package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.kyc.model.KycDocument;

public interface KycOcrService {
    void process(KycDocument kycDocument);
}
