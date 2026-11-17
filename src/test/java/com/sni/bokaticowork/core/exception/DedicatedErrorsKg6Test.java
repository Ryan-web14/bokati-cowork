package com.sni.bokaticowork.core.exception;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** Lot KG6 : erreurs dediees au lieu du repli en 500. */
class DedicatedErrorsKg6Test {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorResponse());
    private final HttpServletRequest request = new MockHttpServletRequest("POST", "/sni/api/v1/documents/upload");

    record Body(DocumentSpace space) {
    }

    @Test
    void aTooLargeFileIs413() {
        ResponseEntity<Object> response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L * 1024 * 1024), request);
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(((ApiError) response.getBody()).getMessage()).contains("10 Mo");
    }

    @Test
    void anInvalidEnumParameterNamesTheParameterAndTheAllowedValues() throws Exception {
        MethodParameter parameter = new MethodParameter(DedicatedErrorsKg6Test.class.getDeclaredMethod("dummy", DocumentSpace.class), 0);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("KYC", DocumentSpace.class, "space", parameter,
                new IllegalArgumentException("No enum constant"));

        ResponseEntity<Object> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ApiError body = (ApiError) response.getBody();
        assertThat(body.getMessage()).contains("space").contains("KYC_SPACE").contains("GENERIC");
    }

    @Test
    void anInvalidEnumInTheBodyNamesTheFieldAndTheAllowedValues() {
        Throwable cause = catchThrowable(() -> JsonMapper.builder().build().readValue("{\"space\":\"KYC\"}", Body.class));
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad", cause, new MockHttpInputMessage(new byte[0]));

        ApiError body = (ApiError) handler.handleMessageNotReadable(ex, request).getBody();

        assertThat(body.getMessage()).contains("space").contains("KYC_SPACE");
    }

    @Test
    void aRolledBackTransactionIsAConflict() {
        ResponseEntity<Object> response = handler.handleRolledBack(new UnexpectedRollbackException("rollback-only"), request);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @SuppressWarnings("unused")
    private void dummy(DocumentSpace space) {
    }
}
