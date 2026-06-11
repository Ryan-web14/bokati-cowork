package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import java.util.Map;

public interface DocumentMetadataService {
    Map<String, String> setMetadata(String documentCode, Map<String, String> metadata);
    Map<String, String> getMetadata(String documentCode);
    void deleteKey(String documentCode, String key);
}
