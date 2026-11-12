package com.sni.bokaticowork.core.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Une contrainte d'entite refusee au commit rend un 400 lisible, pas un 500. */
class EntityValidationErrorTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ErrorResponse());

    record Sample(@Size(min = 1, max = 150) String billingCompanyName) {
    }

    @Test
    @DisplayName("Contrainte violee au commit : 400 VALIDATION_FAILED avec le champ")
    void constraintViolationAtCommitIsBadRequest() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<Sample>> violations = validator.validate(new Sample(""));
            TransactionSystemException ex = new TransactionSystemException("Could not commit JPA transaction",
                    new RuntimeException("rollback", new ConstraintViolationException(violations)));

            ResponseEntity<Object> response = handler.handleTransactionSystem(ex, new MockHttpServletRequest("PATCH", "/x"));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(String.valueOf(response.getBody())).contains("billingCompanyName");
        }
    }

    @Test
    @DisplayName("Autre echec de transaction : reste une erreur serveur")
    void otherFailureStaysServerError() {
        ResponseEntity<Object> response = handler.handleTransactionSystem(
                new TransactionSystemException("boom"), new MockHttpServletRequest("PATCH", "/x"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}
