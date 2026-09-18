package com.sni.bokaticowork.core.utils.phone;

import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.repository.CountryRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Met un numero en forme internationale, en devinant l'indicatif quand il manque.
 *
 * <p>Trois sources, dans cet ordre : le numero lui-meme s'il porte deja un {@code +} ; le pays
 * fourni avec le numero, sous forme de code ISO ({@code CG}) ou d'indicatif ({@code 242},
 * {@code +242}) ; a defaut, le pays de l'etablissement. Un numero sans indicatif n'est donc jamais
 * refuse pour cette seule raison · il est lu comme un numero d'ici, ce qu'il est presque toujours.</p>
 *
 * <p>Ce qui est refuse, c'est un numero qui ne peut pas en etre un : trop court, trop long, un
 * pays inconnu. Et le refus dit lequel.</p>
 */
@Service
@RequiredArgsConstructor
public class PhoneNumberService {

    private final CountryRepository countryRepository;

    @Value("${app.phone.default-country:CG}")
    private String defaultCountry;

    @Value("${app.phone.default-dial-code:+242}")
    private String defaultDialCode;

    /**
     * Pays dont le zero de tete fait partie du numero national et se garde en forme
     * internationale · Congo, Gabon, Cote d'Ivoire depuis 2021, Italie. Partout ailleurs c'est un
     * prefixe d'acces qui tombe.
     */
    @Value("${app.phone.keep-trunk-zero:CG,GA,CI,IT}")
    private String keepTrunkZeroCountries;

    /** Indicatif et regle du zero, resolus ensemble · ils vont par pays. */
    private record DialPlan(String dialCode, boolean keepTrunkZero) {
    }

    /**
     * Forme internationale du numero, ou une explication.
     *
     * @param country code ISO ou indicatif · ignore si le numero porte deja son {@code +}
     * @throws BadRequestException si le numero ne peut pas en etre un, ou si le pays est inconnu
     */
    public String normalize(String raw, String country) {
        if (!StringUtils.hasText(raw)) {
            throw new BadRequestException("Le numéro de téléphone est requis");
        }
        if (!PhoneNumbers.looksValid(raw)) {
            throw new BadRequestException("Le numéro de téléphone « " + raw.trim() + " » n'est pas lisible ·"
                    + " attendu : chiffres, avec ou sans indicatif, espaces et + acceptés");
        }
        if (PhoneNumbers.isInternational(raw)) {
            return PhoneNumbers.toE164(raw, null, false)
                    .orElseThrow(() -> new BadRequestException("Le numéro de téléphone « " + raw.trim()
                            + " » n'a pas une longueur valable"));
        }
        DialPlan plan = planFor(country);
        return PhoneNumbers.toE164(raw, plan.dialCode(), plan.keepTrunkZero())
                .orElseThrow(() -> new BadRequestException("Le numéro de téléphone « " + raw.trim()
                        + " » n'a pas une longueur valable une fois l'indicatif " + plan.dialCode() + " ajouté"));
    }

    /** Comme {@link #normalize}, mais un numero absent reste absent · pour les champs facultatifs. */
    public String normalizeOptional(String raw, String country) {
        return StringUtils.hasText(raw) ? normalize(raw, country) : null;
    }

    /**
     * Forme internationale si on y arrive, sinon le numero nettoye · pour les recherches.
     *
     * <p>Une recherche ne doit pas echouer parce que la saisie est approximative : on cherche avec
     * ce qu'on a de mieux, et on ne trouve pas, c'est tout.</p>
     */
    public String normalizeForLookup(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        if (PhoneNumbers.isInternational(raw)) {
            return PhoneNumbers.toE164(raw, null, false).orElse(PhoneNumbers.clean(raw));
        }
        DialPlan plan = planForOrNull(null);
        return plan == null
                ? PhoneNumbers.clean(raw)
                : PhoneNumbers.toE164(raw, plan.dialCode(), plan.keepTrunkZero()).orElse(PhoneNumbers.clean(raw));
    }

    // -----------------------------------------------------------------------------------------

    private DialPlan planFor(String country) {
        DialPlan plan = planForOrNull(country);
        if (plan == null) {
            throw new BadRequestException("Pays « " + country + " » inconnu · indiquez un code ISO (CG, FR) ou un indicatif (+242)");
        }
        return plan;
    }

    private DialPlan planForOrNull(String country) {
        String value = StringUtils.hasText(country) ? country.trim() : defaultCountry;
        if (!StringUtils.hasText(value)) {
            return new DialPlan(defaultDialCode, keepsTrunkZero(defaultCountry));
        }
        // Un indicatif donne tel quel : « 242 », « +242 », « 00242 ».
        String bare = value.replaceAll("[\\s+]", "").replaceFirst("^00", "");
        if (!bare.isEmpty() && bare.chars().allMatch(Character::isDigit)) {
            String dial = "+" + bare;
            return new DialPlan(dial, keepsTrunkZero(isoForDial(dial)));
        }
        String iso = value.toUpperCase(Locale.ROOT);
        Optional<Country> found = countryRepository.findByCountryCode(iso);
        if (found.isPresent() && StringUtils.hasText(found.get().getPhoneCode())) {
            return new DialPlan("+" + found.get().getPhoneCode().replaceAll("[^0-9]", ""), keepsTrunkZero(iso));
        }
        // La table peut ne pas etre encore alimentee sur un environnement neuf · le pays de
        // l'etablissement ne doit pas en dependre.
        return iso.equalsIgnoreCase(defaultCountry) ? new DialPlan(defaultDialCode, keepsTrunkZero(iso)) : null;
    }

    private String isoForDial(String dial) {
        if (dial.equals(normalizeDial(defaultDialCode))) {
            return defaultCountry;
        }
        return countryRepository.findCountryByPhoneCode(dial).map(Country::getCountryCode).orElse("");
    }

    private String normalizeDial(String dial) {
        return "+" + (dial == null ? "" : dial.replaceAll("[^0-9]", ""));
    }

    private boolean keepsTrunkZero(String iso) {
        if (!StringUtils.hasText(iso) || !StringUtils.hasText(keepTrunkZeroCountries)) {
            return false;
        }
        for (String code : keepTrunkZeroCountries.split(",")) {
            if (code.trim().equalsIgnoreCase(iso.trim())) {
                return true;
            }
        }
        return false;
    }
}
