package com.sni.bokaticowork.core.validation.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ValidationResult {

    private final List<ValidationError> errors;

    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }
}
