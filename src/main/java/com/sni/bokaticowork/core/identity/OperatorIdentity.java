package com.sni.bokaticowork.core.identity;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Identite de l'exploitant telle qu'elle s'imprime sur les pieces.
 *
 * <p>Une seule source · la fiche exploitant ({@code business}, {@code operator = true}). Chaque
 * champ absent ou vide de la fiche retombe sur la valeur qui etait jusqu'ici ecrite en dur dans
 * les gabarits : une piece ne doit jamais sortir sans adresse ni RCCM parce que la fiche est
 * incomplete.</p>
 *
 * <p>Les methodes d'affichage ({@link #contactLine()}, {@link #rccmLine()}...) servent aux
 * fragments communs {@code fragments/document-brand.html} · elles evitent de repeter les
 * conditions de presence dans chaque gabarit.</p>
 */
public record OperatorIdentity(
        String name,
        String legalForm,
        String niu,
        String rccm,
        String address,
        String city,
        String phone,
        String email,
        String representativeName,
        String representativeTitle) {

    public static final String DEFAULT_NAME = "ELLE A OSE";
    public static final String DEFAULT_ADDRESS = "84 Boulevard du Général Charles de Gaulle, centre-ville, Pointe-Noire";
    public static final String DEFAULT_CITY = "Pointe-Noire";
    public static final String DEFAULT_RCCM = "CG-PNR-01-2017-A11-00472";
    public static final String DEFAULT_EMAIL = "coworkspace@elleaose.com";
    public static final String DEFAULT_PHONE = "05 204 2552";

    /** Valeurs historiques des gabarits · utilisees quand la fiche manque ou est incomplete. */
    public static OperatorIdentity defaults() {
        return new OperatorIdentity(DEFAULT_NAME, null, null, DEFAULT_RCCM, DEFAULT_ADDRESS, DEFAULT_CITY,
                DEFAULT_PHONE, DEFAULT_EMAIL, null, null);
    }

    /** Complete chaque champ vide par la valeur par defaut correspondante. */
    public OperatorIdentity withDefaults() {
        OperatorIdentity d = defaults();
        return new OperatorIdentity(
                or(name, d.name), clean(legalForm), clean(niu), or(rccm, d.rccm), or(address, d.address),
                or(city, d.city), or(phone, d.phone), or(email, d.email),
                clean(representativeName), clean(representativeTitle));
    }

    /** « RCCM CG-PNR-... » ou {@code null}. */
    public String rccmLine() {
        return StringUtils.hasText(rccm) ? "RCCM " + rccm : null;
    }

    /** « NIU ... » ou {@code null}. */
    public String niuLine() {
        return StringUtils.hasText(niu) ? "NIU " + niu : null;
    }

    /** RCCM et NIU sur une ligne, separes par un point median. */
    public String registrationLine() {
        return join(" · ", rccmLine(), niuLine());
    }

    /** Courriel et telephone sur une ligne. */
    public String contactLine() {
        return join(" · ", email, phone);
    }

    /** Raison sociale suivie de la forme juridique quand elle est connue. */
    public String fullName() {
        return StringUtils.hasText(legalForm) ? name + " (" + legalForm + ")" : name;
    }

    /** Ligne de pied · nom, adresse, RCCM. */
    public String footerLine() {
        return join(" · ", name, address, rccmLine());
    }

    private static String join(String separator, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                kept.add(part.trim());
            }
        }
        return kept.isEmpty() ? null : String.join(separator, kept);
    }

    private static String or(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned != null ? cleaned : fallback;
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
