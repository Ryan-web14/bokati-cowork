package com.sni.bokaticowork.security.service.passwordResetService.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Six caractères suffisent · mais la longueur n'est pas la seule garde.
 *
 * <p>Abaisser le minimum ne doit pas laisser passer les mots de passe que tout le monde essaie
 * en premier : il faut toujours une lettre, un chiffre, de la variété, et ne pas figurer dans la
 * liste des plus courants.</p>
 */
class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void sixCharactersAreEnough() {
        assertEquals(6, PasswordPolicyValidator.MIN_LENGTH);
        assertTrue(validator.validate("bok4ti").isEmpty(), "six caractères, une lettre, un chiffre");
        assertTrue(validator.validate("User@123456789").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bok4t", "a1", ""})
    void belowTheMinimumIsRefusedWithItsLength(String tooShort) {
        Optional<String> violation = validator.validate(tooShort);
        assertTrue(violation.isPresent());
        assertTrue(violation.get().contains("6 caractères"), violation.orElse(""));
    }

    @Test
    void whatIsShortMustStillBeAPassword() {
        assertTrue(validator.validate(null).isPresent());
        assertTrue(validator.validate("abcdef").isPresent(), "sans chiffre");
        assertTrue(validator.validate("123456").isPresent(), "sans lettre, et trop courant");
        assertTrue(validator.validate("aaa111").isPresent(), "deux caractères distincts seulement");
        assertTrue(validator.validate("aab112").isEmpty(), "quatre caractères distincts, cela passe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"azerty", "qwerty", "000000", "111111", "abc123", "admin1", "password", "motdepasse"})
    void theMostCommonOnesStayRefusedNowThatTheyAreLongEnough(String common) {
        assertTrue(validator.validate(common).isPresent(), common + " doit rester refusé");
    }
}
