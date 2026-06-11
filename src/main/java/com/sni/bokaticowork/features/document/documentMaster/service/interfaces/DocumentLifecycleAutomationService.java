package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

public interface DocumentLifecycleAutomationService {

    int expireDocuments();

    int notifyPreExpiry();
}
