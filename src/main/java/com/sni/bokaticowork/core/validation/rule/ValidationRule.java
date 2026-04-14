package com.sni.bokaticowork.core.validation.rule;

import com.sni.bokaticowork.core.validation.model.ValidationError;

import java.util.List;

public interface ValidationRule<T> {

    List<ValidationError> validate(T target);
}
