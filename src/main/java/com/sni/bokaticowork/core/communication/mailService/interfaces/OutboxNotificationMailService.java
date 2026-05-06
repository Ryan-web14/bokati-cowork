package com.sni.bokaticowork.core.communication.mailService.interfaces;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface OutboxNotificationMailService {

    CompletableFuture<Boolean> sendDocumentNotification(String to, Map<String, Object> variables);

    CompletableFuture<Boolean> sendKycNotification(String to, Map<String, Object> variables);

    CompletableFuture<Boolean> sendContractNotification(String to, Map<String, Object> variables);

    CompletableFuture<Boolean> sendKycDocumentNotification(String to, Map<String, Object> variables);
}
