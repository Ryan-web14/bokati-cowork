package com.sni.bokaticowork.core.validation.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidationError {

    private final String field;
    private final String code;
    private final String message;
}
