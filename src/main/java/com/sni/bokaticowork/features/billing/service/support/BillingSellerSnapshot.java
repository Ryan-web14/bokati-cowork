package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantane du vendeur sur une piece de facturation (colonnes {@code seller_*}, V157).
 *
 * <p>Fige a l'emission l'identite de l'exploitant telle qu'elle est ce jour-la · une facture doit
 * pouvoir etre reimprimee a l'identique apres un demenagement ou un changement de raison sociale.
 * Une piece deja verrouillee (validee, scellee) ou deja dotee d'un instantane n'est jamais
 * reecrite.</p>
 *
 * <p>L'adresse est stockee en tableau JSON de lignes ({@code ["84 Boulevard ...", "Pointe-Noire"]})
 * · {@code fmt.addressLines} le lit sans libelle, et {@link #addressFromJson} le relit.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingSellerSnapshot {

    private final OperatorIdentityService operatorIdentityService;
    private final ObjectMapper objectMapper;

    /** Ecrit l'instantane si la piece n'en a pas et n'est pas verrouillee. */
    public void applyIfMissing(BillingDocument document) {
        if (document == null || Boolean.TRUE.equals(document.getLocked())
                || StringUtils.hasText(document.getSellerName())) {
            return;
        }
        apply(document, operatorIdentityService.current());
    }

    void apply(BillingDocument document, OperatorIdentity identity) {
        document.setSellerName(identity.name());
        document.setSellerNiu(identity.niu());
        document.setSellerAddressJson(addressJson(identity.address()));
        document.setSellerPhone(identity.phone());
        document.setSellerEmail(identity.email());
    }

    private String addressJson(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(List.of(address.trim()));
        } catch (Exception ex) {
            log.warn("Adresse du vendeur non serialisable : {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Identite figee d'une piece · seller_* completes par l'identite courante (RCCM, ville,
     * representant, qui ne sont pas figes).
     *
     * @return {@code null} si la piece n'a pas d'instantane
     */
    public static OperatorIdentity frozen(String sellerName, String sellerNiu, String sellerAddressJson,
                                          String sellerPhone, String sellerEmail,
                                          OperatorIdentity current, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(sellerName)) {
            return null;
        }
        OperatorIdentity base = current != null ? current : OperatorIdentity.defaults();
        return new OperatorIdentity(
                sellerName, base.legalForm(), sellerNiu != null ? sellerNiu : base.niu(), base.rccm(),
                addressFromJson(sellerAddressJson, objectMapper), base.city(),
                StringUtils.hasText(sellerPhone) ? sellerPhone : base.phone(),
                StringUtils.hasText(sellerEmail) ? sellerEmail : base.email(),
                base.representativeName(), base.representativeTitle()).withDefaults();
    }

    /** Relit l'adresse figee · tableau de lignes, objet ou texte brut. */
    public static String addressFromJson(String json, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> parts = new ArrayList<>();
            collect(root, parts);
            return parts.isEmpty() ? null : String.join(", ", parts);
        } catch (Exception ex) {
            return json.trim();
        }
    }

    private static void collect(JsonNode node, List<String> parts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            String text = node.asText();
            if (StringUtils.hasText(text)) {
                parts.add(text.trim());
            }
            return;
        }
        node.forEach(child -> collect(child, parts));
    }
}
