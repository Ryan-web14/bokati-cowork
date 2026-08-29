package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.config.BillingDiscountGuardProperties;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Controle des remises · limites par article du catalogue et seuil global du document.
 *
 * <h2>Deux temps distincts</h2>
 * {@link #evaluate} constate et n'interrompt rien : c'est ce qui permet a un commercial de
 * construire son offre librement et de voir en direct ce qui coince. {@link #enforce} refuse, et
 * n'est appele qu'a l'emission — le moment ou le document engage.
 *
 * <h2>Le contournement</h2>
 * Il passe par la permission {@code BILLING:DISCOUNT_OVERRIDE}, pas par un role code en dur. Un
 * role en dur cree un droit invisible de l'ecran des roles : impossible a accorder, a retirer ou
 * a auditer. Et il exige un motif : un depassement silencieux n'a aucune valeur de controle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingDiscountGuard {

    public static final String OVERRIDE_PERMISSION = "BILLING:DISCOUNT_OVERRIDE";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ServiceCatalogItemRepository catalogItemRepository;
    private final BillingDiscountGuardProperties properties;

    /**
     * Constate les depassements sans rien interrompre.
     *
     * @param subtotal      sous-total avant toute remise
     * @param totalDiscount remise totale, remises de ligne comprises
     */
    public List<DiscountViolation> evaluate(List<BillingDocumentLine> lines,
                                            BigDecimal subtotal,
                                            BigDecimal totalDiscount) {
        List<DiscountViolation> violations = new ArrayList<>();
        if (lines != null) {
            lines.forEach(line -> evaluateLine(line, violations));
        }
        evaluateDocument(subtotal, totalDiscount, violations);
        return violations;
    }

    /**
     * Refuse si au moins un depassement est bloquant, sauf derogation valide.
     *
     * @param overrideReason motif de derogation · obligatoire pour passer outre
     * @return le motif retenu si une derogation a bien ete exercee, {@code null} sinon
     */
    public String enforce(List<DiscountViolation> violations, String overrideReason) {
        List<DiscountViolation> blocking = violations.stream()
                .filter(violation -> Severity.BLOCK.name().equals(violation.severity()))
                .toList();
        if (blocking.isEmpty()) {
            return null;
        }

        String summary = blocking.stream().map(DiscountViolation::message).reduce((a, b) -> a + " · " + b).orElse("");

        if (!StringUtils.hasText(overrideReason)) {
            throw new BadRequestException("Remise hors limites : " + summary
                    + ". Un motif de derogation est requis pour emettre ce document.");
        }
        if (!hasOverridePermission()) {
            throw new BadRequestException("Remise hors limites : " + summary
                    + ". La derogation exige la permission " + OVERRIDE_PERMISSION + ".");
        }

        log.warn("Derogation de remise · {} depassement(s) · motif={} · utilisateur={}",
                blocking.size(), overrideReason, currentUser());
        return overrideReason.trim();
    }

    // =================================================================================
    // Controles
    // =================================================================================

    private void evaluateLine(BillingDocumentLine line, List<DiscountViolation> violations) {
        if (line == null || !StringUtils.hasText(line.getItemCode())) {
            return;
        }
        Optional<ServiceCatalogItem> found = catalogItemRepository.findByItemCode(line.getItemCode().trim());
        if (found.isEmpty()) {
            return;
        }
        ServiceCatalogItem item = found.get();

        Severity severity = severityOf(item.getDiscountPolicy());
        if (severity == null) {
            return;
        }

        BigDecimal gross = orZero(line.getSubtotalAmount());
        BigDecimal discount = orZero(line.getDiscountAmount());
        BigDecimal quantity = orZero(line.getQuantity());

        // Prix unitaire reellement consenti, remise deduite · c'est lui que le plancher borne,
        // et non le prix catalogue, sans quoi une remise suffirait a passer dessous.
        if (quantity.signum() > 0) {
            BigDecimal netUnitPrice = gross.subtract(discount).divide(quantity, 4, RoundingMode.HALF_UP);
            BigDecimal floor = item.effectiveFloorPrice();
            if (floor != null && netUnitPrice.compareTo(floor) < 0) {
                violations.add(new DiscountViolation(
                        ViolationType.FLOOR_PRICE.name(), severity.name(),
                        item.getItemCode(), line.getDescription(), netUnitPrice, floor,
                        "« " + item.getName() + " » descend a " + plain(netUnitPrice)
                                + " alors que son plancher est " + plain(floor)));
            }
        }

        // Le taux effectif est recalcule depuis les montants : une remise saisie en valeur doit
        // etre bornee comme une remise saisie en taux.
        if (item.getMaxDiscountRate() != null && gross.signum() > 0) {
            BigDecimal effectiveRate = discount.multiply(HUNDRED).divide(gross, 4, RoundingMode.HALF_UP);
            if (effectiveRate.compareTo(item.getMaxDiscountRate()) > 0) {
                violations.add(new DiscountViolation(
                        ViolationType.MAX_DISCOUNT_RATE.name(), severity.name(),
                        item.getItemCode(), line.getDescription(), effectiveRate, item.getMaxDiscountRate(),
                        "« " + item.getName() + " » remise a " + plain(effectiveRate)
                                + " % alors que le maximum est " + plain(item.getMaxDiscountRate()) + " %"));
            }
        }
    }

    private void evaluateDocument(BigDecimal subtotal, BigDecimal totalDiscount,
                                  List<DiscountViolation> violations) {
        if (!properties.isEnabled()) {
            return;
        }
        BigDecimal gross = orZero(subtotal);
        BigDecimal discount = orZero(totalDiscount);

        BigDecimal maxRate = properties.getMaxDocumentDiscountRate();
        if (maxRate != null && gross.signum() > 0) {
            BigDecimal rate = discount.multiply(HUNDRED).divide(gross, 4, RoundingMode.HALF_UP);
            if (rate.compareTo(maxRate) > 0) {
                violations.add(new DiscountViolation(
                        ViolationType.DOCUMENT_DISCOUNT_RATE.name(), Severity.BLOCK.name(),
                        null, null, rate, maxRate,
                        "Remise globale de " + plain(rate) + " % alors que le maximum est "
                                + plain(maxRate) + " %"));
            }
        }

        BigDecimal maxAmount = properties.getMaxDocumentDiscountAmount();
        if (maxAmount != null && discount.compareTo(maxAmount) > 0) {
            violations.add(new DiscountViolation(
                    ViolationType.DOCUMENT_DISCOUNT_AMOUNT.name(), Severity.BLOCK.name(),
                    null, null, discount, maxAmount,
                    "Remise globale de " + plain(discount) + " alors que le plafond est "
                            + plain(maxAmount)));
        }
    }

    // =================================================================================
    // Utilitaires
    // =================================================================================

    /** {@code null} lorsque l'article n'impose aucun controle. */
    private Severity severityOf(String policy) {
        if (policy == null) {
            return null;
        }
        return switch (policy.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "WARN" -> Severity.WARN;
            case "BLOCK" -> Severity.BLOCK;
            default -> null;
        };
    }

    private boolean hasOverridePermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        // Les autorites portent les deux ecritures, nom de permission et MODULE:ACTION · on
        // accepte les deux, comme AdminApiAuthorizationManager.
        return authorities.stream().anyMatch(authority ->
                OVERRIDE_PERMISSION.equals(authority.getAuthority())
                        || OVERRIDE_PERMISSION.replace(':', '_').equals(authority.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "inconnu" : authentication.getName();
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public enum Severity { WARN, BLOCK }

    public enum ViolationType { FLOOR_PRICE, MAX_DISCOUNT_RATE, DOCUMENT_DISCOUNT_RATE, DOCUMENT_DISCOUNT_AMOUNT }

    /**
     * @param observed valeur constatee · taux en % ou montant selon le type
     * @param limit    limite applicable, dans la meme unite que {@code observed}
     */
    public record DiscountViolation(String type, String severity, String itemCode, String lineDescription,
                                    BigDecimal observed, BigDecimal limit, String message) {}
}
