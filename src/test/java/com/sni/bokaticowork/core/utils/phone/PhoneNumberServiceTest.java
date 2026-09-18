package com.sni.bokaticowork.core.utils.phone;

import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.repository.CountryRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Les numéros de téléphone, sous toutes les formes que les gens tapent.
 *
 * <p>Aucune de ces formes n'est une erreur : +, 00, espaces, points, tirets, parenthèses sont des
 * habitudes d'écriture. Ce qui est gardé est la forme internationale, une seule par numéro.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneNumberServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private PhoneNumberService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultCountry", "CG");
        ReflectionTestUtils.setField(service, "defaultDialCode", "+242");
        ReflectionTestUtils.setField(service, "keepTrunkZeroCountries", "CG,GA,CI,IT");
        when(countryRepository.findCountryByPhoneCode(anyString())).thenReturn(Optional.empty());
        when(countryRepository.findCountryByPhoneCode("+33")).thenReturn(Optional.of(country("FR", "+33")));
        when(countryRepository.findByCountryCode(anyString())).thenReturn(Optional.empty());
        when(countryRepository.findByCountryCode("FR")).thenReturn(Optional.of(country("FR", "+33")));
        when(countryRepository.findByCountryCode("CM")).thenReturn(Optional.of(country("CM", "+237")));
    }

    private Country country(String iso, String dial) {
        Country country = new Country();
        country.setCountryCode(iso);
        country.setPhoneCode(dial);
        return country;
    }

    // ---------------------------------------------------------------------------------------

    @Test
    void auCongoLeZeroDeTeteFaitPartieDuNumero() {
        // +242 06 123 45 67 · le zero n'est pas un prefixe d'accès, il se garde.
        assertEquals("+242061234567", service.normalize("061234567", "CG"));
        assertEquals("+242061234567", service.normalize("061234567", "+242"));
    }

    @Test
    void enFranceLeZeroDeTeteTombe() {
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "FR"));
    }

    @Test
    void laFormeLocaleCongolaiseDevientInternationale() {
        assertEquals("+242061234567", service.normalize("061234567", null));
        assertEquals("+242061234567", service.normalize("06 123 45 67", null));
        assertEquals("+242061234567", service.normalize("06.123.45.67", null));
    }

    @Test
    void lePlusEtLesEspacesNeGenentPas() {
        assertEquals("+242061234567", service.normalize("+242 06 123 45 67", null));
        assertEquals("+242061234567", service.normalize("+242061234567", "FR"),
                "Un numéro qui porte son indicatif n'écoute pas le pays qu'on lui donne");
    }

    @Test
    void leDoubleZeroVautLePlus() {
        assertEquals("+33612345678", service.normalize("0033 6 12 34 56 78", null));
    }

    @Test
    void lePaysCompleteLIndicatifManquant() {
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "FR"));
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "fr"));
        assertEquals("+237699001122", service.normalize("699 00 11 22", "CM"));
    }

    @Test
    void lePaysPeutEtreDonneCommeIndicatif() {
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "+33"));
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "33"));
        assertEquals("+33612345678", service.normalize("06 12 34 56 78", "0033"));
    }

    @Test
    void lIndicatifDejaTapeSansLePlusNEstPasDouble() {
        assertEquals("+242061234567", service.normalize("242 06 123 45 67", null));
    }

    @Test
    void lesParenthesesEtTiretsSontLus() {
        assertEquals("+12125551234", service.normalize("+1 (212) 555-1234", null));
    }

    @Test
    void unPaysInconnuEstRefuseEnLeNommant() {
        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> service.normalize("06 12 34 56 78", "ZZ"));
        assertTrue(thrown.getMessage().contains("ZZ"));
    }

    @Test
    void unNumeroIllisibleEstRefuseAvecLaRaison() {
        assertThrows(BadRequestException.class, () -> service.normalize("abc", null));
        assertThrows(BadRequestException.class, () -> service.normalize("12", null));
        assertThrows(BadRequestException.class, () -> service.normalize("+242 06 12 34 56 78 90 12 34", null),
                "Plus de quinze chiffres n'est un numéro nulle part");
    }

    @Test
    void lePaysParDefautTientSansLaTable() {
        // Sur un environnement neuf la table des pays peut être vide · le pays de l'établissement
        // ne doit pas en dépendre.
        when(countryRepository.findByCountryCode("CG")).thenReturn(Optional.empty());

        assertEquals("+242061234567", service.normalize("061234567", "CG"));
    }

    @Test
    void laRechercheNEchouePasSurUneSaisieApproximative() {
        assertEquals("+242061234567", service.normalizeForLookup("06 123 45 67"));
        assertEquals("+242061234567", service.normalizeForLookup("+242061234567"));
    }

    @Test
    void laFormeSeuleSeJugeSansPays() {
        assertTrue(PhoneNumbers.looksValid("+242 06 123 45 67"));
        assertTrue(PhoneNumbers.looksValid("06 123 45 67"));
        assertTrue(PhoneNumbers.looksValid("(212) 555-1234"));
        assertFalse(PhoneNumbers.looksValid("06 12 34 abc"));
        assertFalse(PhoneNumbers.looksValid(""));
        assertFalse(PhoneNumbers.looksValid("+"));
    }
}
