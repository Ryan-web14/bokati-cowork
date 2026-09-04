package com.sni.bokaticowork.features.contract.service.support;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Formulations d'un contrat · lieu de signature et dates en toutes lettres.
 *
 * <p>Les gabarits portaient « Fait a ____________, le ____ / ____ / ________ » en dur, jamais
 * rempli, alors que la date de generation et l'adresse de l'exploitant sont connues. Un contrat
 * remis avec des blancs a completer a la main n'a pas la meme valeur qu'un contrat complet : il
 * laisse croire que la date reste a negocier, et deux exemplaires peuvent finir dates differemment.
 *
 * <p>Les dates etaient par ailleurs injectees en {@code toString()}, donc au format ISO
 * {@code 2026-09-03}. Une piece juridique s'ecrit « 3 septembre 2026 » : la forme ISO se lit mal et
 * se prete a confusion pour un lecteur non technique.
 */
@Component
public class ContractWording {

    private static final DateTimeFormatter LONG_FRENCH =
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.FRENCH);

    /**
     * Ville ou le contrat est repute signe.
     *
     * <p>Refus explicite quand elle manque, plutot qu'un blanc ou une ville par defaut. Le lieu de
     * signature determine la juridiction competente : l'inventer serait une erreur juridique, et le
     * laisser vide rendrait le contrat incomplet. La donnee se corrige en une fois sur la fiche de
     * l'entite, et le message dit laquelle.
     */
    public String placeOfSigning(BusinessEntity business) {
        if (business == null) {
            throw new BadRequestException("Le lieu de signature du contrat est inconnu : aucune"
                    + " entite exploitante n'est rattachee a la demande. Indiquez businessCode · "
                    + " le lieu determine la juridiction competente et ne peut etre laisse vide.");
        }
        String city = business.getAddress() == null ? null : business.getAddress().getCity();
        if (!StringUtils.hasText(city)) {
            String name = StringUtils.hasText(business.getName()) ? business.getName() : business.getCode();
            throw new BadRequestException("Le lieu de signature du contrat est inconnu : "
                    + name + " n'a pas de ville dans son adresse. Renseignez-la avant de generer"
                    + " le contrat · elle determine la juridiction competente et ne peut etre laissee vide.");
        }
        return city.trim();
    }

    /** {@code 3 septembre 2026} · forme attendue dans une piece juridique. */
    public String inWords(LocalDate date) {
        return date == null ? null : date.format(LONG_FRENCH);
    }

    /**
     * Reecrit en toutes lettres les dates deja presentes dans les variables.
     *
     * <p>Elles y arrivent en ISO parce qu'elles proviennent d'un {@code LocalDate.toString()}. Une
     * valeur non reconnue est laissee intacte : elle peut avoir ete saisie a la main par un
     * administrateur, et la corriger au jugé serait pire que de la transmettre telle quelle.
     */
    public void frenchifyDates(Map<String, String> variables, String... keys) {
        for (String key : keys) {
            String raw = variables.get(key);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            try {
                variables.put(key, inWords(LocalDate.parse(raw.trim())));
            } catch (RuntimeException ignored) {
                // Valeur libre · on ne touche pas a ce qu'on ne sait pas lire.
            }
        }
    }

    /** Adresse sur une ligne, pour l'en-tete et le pied de page. */
    public String oneLine(Address address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, address.getStreetNumber(), " ");
        appendIfPresent(sb, address.getStreetName(), ", ");
        appendIfPresent(sb, address.getDistrict(), ", ");
        appendIfPresent(sb, address.getCity(), "");
        String result = sb.toString().replaceAll("[,\\s]+$", "").trim();
        return result.isEmpty() ? null : result;
    }

    private void appendIfPresent(StringBuilder sb, String value, String separator) {
        if (StringUtils.hasText(value)) {
            sb.append(value.trim()).append(separator);
        }
    }
}
