package com.sni.bokaticowork.features.document.documentMaster.dto.response;

public  record  DocumentFileResult(byte[] content, String mimeType, String fileName) {}