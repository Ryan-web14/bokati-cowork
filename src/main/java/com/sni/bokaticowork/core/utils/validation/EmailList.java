package com.sni.bokaticowork.core.utils.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Une ou plusieurs adresses de courriel separees par des virgules (ou des points-virgules).
 * Vide ou absent : accepte.
 */
@Documented
@Constraint(validatedBy = EmailList.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailList {

    String message() default "une ou plusieurs adresses de courriel valides, séparées par des virgules";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<EmailList, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || EmailAddresses.isValidList(value);
        }
    }
}
