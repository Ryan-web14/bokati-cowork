package com.sni.bokaticowork.core.validation.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.validation.model.ValidationError;
import com.sni.bokaticowork.core.validation.model.ValidationResult;
import com.sni.bokaticowork.core.validation.rule.ValidationRule;
import com.sni.bokaticowork.core.validation.service.interfaces.ValidationEngine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultValidationEngine implements ValidationEngine {

    @Override
    public <T> ValidationResult validate(T target, List<ValidationRule<T>> rules) {
        List<ValidationError> errors = new ArrayList<>();

        if (rules != null) {
            for (ValidationRule<T> rule : rules) {
                if (rule == null) {
                    continue;
                }

                List<ValidationError> ruleErrors = rule.validate(target);
                if (ruleErrors != null && !ruleErrors.isEmpty()) {
                    errors.addAll(ruleErrors);
                }
            }
        }

        return ValidationResult.builder()
                .errors(errors)
                .build();
    }

    @Override
    public <T> void validateAndThrow(T target, String message, List<ValidationRule<T>> rules) {
        ValidationResult result = validate(target, rules);
        if (!result.isValid()) {
            throw new ValidationException(
                    message,
                    result.getErrors().stream().map(ValidationError::getMessage).toList()
            );
        }
    }
}
