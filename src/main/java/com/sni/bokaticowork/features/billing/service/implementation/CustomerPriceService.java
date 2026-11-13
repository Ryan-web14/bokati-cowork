package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.CustomerPriceDtos;
import com.sni.bokaticowork.features.billing.model.CustomerPrice;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.CustomerPriceRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lot C8 : tarifs negocies par client (contrat-cadre). Prix fixe ou taux de remise, date, propre a
 * une organisation ou un membre. Ordre des prix (decision CD8) : contrat du client, puis prix du
 * segment date, puis prix de la fiche ; une remise manuelle s'ajoute ensuite dans les limites de
 * l'article. Un nouveau tarif du meme article arrete le precedent la veille : rien n'est efface.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerPriceService {

    private final CustomerPriceRepository prices;
    private final ServiceCatalogItemRepository items;
    private final StaffProfileService staffProfiles;

    @Transactional(readOnly = true)
    public List<CustomerPriceDtos.PriceView> list(String customerType, String customerCode, boolean currentOnly) {
        LocalDate today = LocalDate.now();
        return prices.findByCustomerTypeAndCustomerCodeOrderByItemCodeAscValidFromDesc(customerType, customerCode).stream()
                .filter(price -> !currentOnly || price.validOn(today))
                .map(this::view).toList();
    }

    public CustomerPriceDtos.PriceView create(String customerType, String customerCode, CustomerPriceDtos.PriceRequest request) {
        if ((request.unitPrice() == null) == (request.discountRate() == null)) {
            throw BadRequestException.coded("TARIF_INVALIDE", "Indiquez soit un prix (unitPrice), soit un taux de remise (discountRate).");
        }
        if (request.validUntil() != null && request.validUntil().isBefore(request.validFrom())) {
            throw BadRequestException.coded("TARIF_INVALIDE", "La fin de validité précède le début.");
        }
        ServiceCatalogItem item = item(request.itemCode());
        // Le tarif en cours (ou futur) du meme article s'arrete la veille ; un tarif qui commencerait
        // le meme jour serait remplace : il prend fin la veille de son propre debut, donc on le refuse.
        for (CustomerPrice previous : prices.findByCustomerTypeAndCustomerCodeAndItemCodeOrderByValidFromDesc(customerType, customerCode,
                item.getItemCode())) {
            boolean overlaps = previous.getValidUntil() == null || !previous.getValidUntil().isBefore(request.validFrom());
            if (!overlaps) {
                continue;
            }
            if (!previous.getValidFrom().isBefore(request.validFrom())) {
                throw BadRequestException.coded("TARIF_INVALIDE", "Un tarif de " + item.getItemCode() + " commence déjà le "
                        + previous.getValidFrom() + " : arrêtez-le ou choisissez une date postérieure.");
            }
            previous.setValidUntil(request.validFrom().minusDays(1));
            prices.save(previous);
        }
        CustomerPrice saved = prices.save(CustomerPrice.builder().customerType(customerType).customerCode(customerCode)
                .itemCode(item.getItemCode()).unitPrice(request.unitPrice()).discountRate(request.discountRate())
                .currency(item.getCurrency() == null ? "XAF" : item.getCurrency())
                .validFrom(request.validFrom()).validUntil(request.validUntil())
                .contractReference(trim(request.contractReference())).reason(trim(request.reason()))
                .createdBy(staffProfiles.currentAuthor().login()).build());
        return view(saved);
    }

    public CustomerPriceDtos.PriceView end(Long id, LocalDate validUntil) {
        CustomerPrice price = prices.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarif introuvable : " + id));
        if (validUntil.isBefore(price.getValidFrom())) {
            throw BadRequestException.coded("TARIF_INVALIDE", "La fin de validité précède le début (" + price.getValidFrom() + ").");
        }
        price.setValidUntil(validUntil);
        return view(prices.save(price));
    }

    /**
     * Prix du contrat d'un client pour un article a une date : prix fixe, ou prix de base (segment
     * ou fiche) diminue du taux negocie. Vide sans contrat en vigueur.
     */
    @Transactional(readOnly = true)
    public Optional<Contract> contractPrice(String customerType, String customerCode, String itemCode, LocalDate date, BigDecimal basePrice) {
        if (!StringUtils.hasText(customerType) || !StringUtils.hasText(customerCode) || !StringUtils.hasText(itemCode)) {
            return Optional.empty();
        }
        LocalDate day = date == null ? LocalDate.now() : date;
        return prices.findByCustomerTypeAndCustomerCodeAndItemCodeOrderByValidFromDesc(customerType.trim(), customerCode.trim(), itemCode.trim())
                .stream().filter(price -> price.validOn(day)).findFirst()
                .map(price -> new Contract(price, price.getUnitPrice() != null ? price.getUnitPrice()
                        : basePrice == null ? null : basePrice.multiply(BigDecimal.valueOf(100).subtract(price.getDiscountRate()))
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
    }

    /** Tarif retenu et prix qui en resulte. */
    public record Contract(CustomerPrice price, BigDecimal unitPrice) {
    }

    private ServiceCatalogItem item(String itemCode) {
        return items.findByItemCode(itemCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Article du catalogue introuvable : " + itemCode));
    }

    private CustomerPriceDtos.PriceView view(CustomerPrice price) {
        String name = items.findByItemCode(price.getItemCode()).map(ServiceCatalogItem::getName).orElse(null);
        return new CustomerPriceDtos.PriceView(price.getId(), price.getCustomerType(), price.getCustomerCode(), price.getItemCode(), name,
                price.getUnitPrice(), price.getDiscountRate(), price.getCurrency(), price.getValidFrom(), price.getValidUntil(),
                price.validOn(LocalDate.now()), price.getContractReference(), price.getReason(), price.getCreatedBy(), price.getCreatedAt());
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
