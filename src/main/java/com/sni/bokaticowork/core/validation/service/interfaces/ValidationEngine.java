package com.sni.bokaticowork.core.validation.service.interfaces;

import com.sni.bokaticowork.core.validation.model.ValidationResult;
import com.sni.bokaticowork.core.validation.rule.ValidationRule;

import java.util.List;

public interface ValidationEngine {

    <T> ValidationResult validate(T target, List<ValidationRule<T>> rules);

    <T> void validateAndThrow(T target, String message, List<ValidationRule<T>> rules);
}
