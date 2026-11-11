package com.sni.bokaticowork.core.utils.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAddressesAndNiuTest {

    @Test
    @DisplayName("Plusieurs courriels de facturation separes par des virgules : decoupes, sans doublon")
    void splitsACommaSeparatedList() {
        assertThat(EmailAddresses.split(" compta@acme.cg, DG@acme.cg ;finance@acme.cg,, compta@ACME.cg "))
                .containsExactly("compta@acme.cg", "DG@acme.cg", "finance@acme.cg");
        assertThat(EmailAddresses.split("  ")).isEmpty();
        assertThat(EmailAddresses.split(null)).isEmpty();
    }

    @Test
    @DisplayName("Liste valide si toutes les adresses le sont ; une seule adresse reste acceptee")
    void validatesEveryAddress() {
        assertThat(EmailAddresses.isValidList("compta@acme.cg")).isTrue();
        assertThat(EmailAddresses.isValidList("compta@acme.cg, dg@acme.cg")).isTrue();
        assertThat(EmailAddresses.isValidList("compta@acme.cg, pas-une-adresse")).isFalse();
        assertThat(EmailAddresses.isValidList(",")).isFalse();
        assertThat(EmailAddresses.isValidList("a@b.cg," + "x".repeat(600))).isFalse();
    }

    @Test
    @DisplayName("Normalisee en minuscules, separee par « , »")
    void normalizes() {
        assertThat(EmailAddresses.normalize("Compta@Acme.cg;dg@acme.cg ")).isEqualTo("compta@acme.cg, dg@acme.cg");
        assertThat(EmailAddresses.normalize(" ")).isNull();
        assertThat(EmailAddresses.first("compta@acme.cg, dg@acme.cg")).isEqualTo("compta@acme.cg");
    }

    @Test
    @DisplayName("La contrainte @EmailList accepte vide, une ou plusieurs adresses, refuse une adresse invalide")
    void constraint() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new Holder(null))).isEmpty();
        assertThat(validator.validate(new Holder("compta@acme.cg, dg@acme.cg"))).isEmpty();
        assertThat(validator.validate(new Holder("compta@acme.cg, dg"))).hasSize(1);
    }

    record Holder(@EmailList String billingEmail) {
    }

    @Test
    @DisplayName("NIU : P, E ou M suivi de 15 chiffres")
    void niuAcceptsM() {
        assertThat(ValidationUtils.validateNiu("M012345678901234")).isTrue();
        assertThat(ValidationUtils.validateNiu("P012345678901234")).isTrue();
        assertThat(ValidationUtils.validateNiu("E012345678901234")).isTrue();
        assertThat(ValidationUtils.validateNiu("X012345678901234")).isFalse();
        assertThat(ValidationUtils.validateNiu("M01234567890123")).isFalse();
    }
}
