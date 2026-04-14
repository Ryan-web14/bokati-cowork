package com.sni.bokaticowork.core.idempotency.service.interfaces;

import com.sni.bokaticowork.core.idempotency.service.model.IdempotencyExecutionResult;

import java.util.function.Supplier;

public interface IdempotencyService {

    <T> IdempotencyExecutionResult<T> execute(String operation,
                                              String idempotencyKey,
                                              Object requestPayload,
                                              Class<T> responseType,
                                              Supplier<T> callback);
}
