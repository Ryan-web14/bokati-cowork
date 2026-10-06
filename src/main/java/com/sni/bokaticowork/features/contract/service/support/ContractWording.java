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

    /** Espace insecable · un montant ne doit jamais etre coupe par un retour a la ligne. */
    private static final String NBSP = " ";

    private static final java.text.DecimalFormat GROUPED = grouped();

    /**
     * Locutions adverbiales et non adjectifs. « La periodicite convenue est mensuel » etait
     * faux, periodicite etant feminin. Une locution se place dans toute phrase sans accord.
     */
    private static final Map<String, String> BILLING_CYCLES = Map.of(
            "ONE_TIME",  "en une seule fois",
            "DAILY",     "tous les jours",
            "WEEKLY",    "toutes les semaines",
            "MONTHLY",   "tous les mois",
            "QUARTERLY", "tous les trois mois",
            "YEARLY",    "tous les ans");

    private static final Map<String, String> PASS_TYPES = Map.of(
            "DAY_PASS",            "pass journalier",
            "TIME_PACK",           "forfait horaire",
            "VISITOR_PASS",        "pass visiteur",
            "MEETING_ROOM_PACK",   "forfait salle de reunion",
            "PROMOTIONAL_PASS",    "pass promotionnel",
            "SUBSCRIPTION_PASS",   "pass d abonnement",
            "COMPANY_SHARED_PASS", "pass partage d entreprise",
            "CUSTOM",              "pass sur mesure");

    private static final Map<String, String> OBJECTS = Map.of(
            "SUBSCRIPTION",       "abonnement",
            "PASS",               "pass",
            "SUBSCRIPTION_ADDON", "option d abonnement");

    private static java.text.DecimalFormat grouped() {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(NBSP.charAt(0));
        java.text.DecimalFormat format = new java.text.DecimalFormat("#,##0.##", symbols);
        format.setGroupingUsed(true);
        return format;
    }

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

    /**
     * Libelles francais des valeurs d'enumeration portees par les variables.
     *
     * <p>Les gabarits recevaient {@code MONTHLY}, {@code DAY_PASS}, {@code SUBSCRIPTION_ADDON} ·
     * des identifiants techniques anglais, affiches tels quels dans une piece juridique francaise.
     * La traduction appartient au code et non au gabarit : elle doit etre la meme sur tous les
     * documents, et un gabarit qui traduit finit par traduire differemment.</p>
     *
     * <p>Une valeur inconnue est laissee telle quelle · mieux vaut un libelle brut qu'un blanc, et
     * une enumeration peut gagner une valeur sans que ce code soit repris.</p>
     */
    public void frenchifyLabels(Map<String, String> variables) {
        putLabel(variables, "billingCycle", "billingCycleLabel", BILLING_CYCLES);
        putLabel(variables, "passType", "passTypeLabel", PASS_TYPES);
        putLabel(variables, "sourceType", "objectLabel", OBJECTS);
        String amount = money(variables.get("totalAmount"), variables.get("currency"));
        if (amount != null) {
            variables.put("amountLabel", amount);
        }
        String unit = money(variables.get("unitPrice"), variables.get("currency"));
        if (unit != null) {
            variables.put("unitPriceLabel", unit);
        }
    }

    /**
     * Montant lisible · separateur de milliers francais et devise nommee.
     *
     * <p>Les montants arrivent en {@code toPlainString()}, donc {@code 180000}. Sur un contrat, le
     * montant est la clause que le client relit : il s'ecrit {@code 180 000 FCFA}. L'espace est
     * insecable, pour qu'un retour a la ligne ne coupe jamais un nombre en deux.</p>
     */
    public String money(String rawAmount, String currency) {
        if (!StringUtils.hasText(rawAmount)) {
            return null;
        }
        String grouped;
        try {
            synchronized (GROUPED) {
                grouped = GROUPED.format(new java.math.BigDecimal(rawAmount.trim()));
            }
        } catch (NumberFormatException ex) {
            // Valeur libre · transmise telle quelle plutot que perdue.
            grouped = rawAmount.trim();
        }
        String label = currencyLabel(currency);
        return StringUtils.hasText(label) ? grouped + NBSP + label : grouped;
    }

    /** {@code XAF} est un code ISO · sur un contrat congolais, la devise se nomme FCFA. */
    private String currencyLabel(String currency) {
        if (!StringUtils.hasText(currency)) {
            return null;
        }
        String code = currency.trim().toUpperCase(Locale.ROOT);
        return "XAF".equals(code) || "XOF".equals(code) ? "FCFA" : code;
    }

    private void putLabel(Map<String, String> variables, String sourceKey, String targetKey,
                          Map<String, String> dictionary) {
        String raw = variables.get(sourceKey);
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        variables.put(targetKey, dictionary.getOrDefault(key, raw.trim()));
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
