package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.marketplace.dto.MarketplaceDtos;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOffer;
import com.sni.bokaticowork.features.marketplace.repository.MarketplaceOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Le catalogue de la marketplace · ce que l'administration met en vente, a quel prix, et ce qu'il
 * en reste.
 */
@Service
@RequiredArgsConstructor
public class MarketplaceCatalogService {

    private final MarketplaceOfferRepository offerRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryLocationRepository locationRepository;
    private final StockLevelRepository stockLevelRepository;
    private final ServiceCatalogItemRepository serviceItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OfferView> list(Boolean published, String category) {
        return offerRepository.search(published, trim(category)).stream().map(this::view).toList();
    }

    /** Le portail ne voit que ce qui est publie et vendable. */
    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OfferView> storefront(String category) {
        return offerRepository.search(Boolean.TRUE, trim(category)).stream()
                .filter(this::sellable)
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceDtos.OfferView get(Long id) {
        return view(offer(id));
    }

    @Transactional
    public MarketplaceDtos.OfferView create(MarketplaceDtos.OfferRequest request, String actor) {
        MarketplaceOffer.Kind kind = request.kind();
        MarketplaceOffer offer = MarketplaceOffer.builder()
                .offerCode(sequenceGenerator.next("marketplace_offer"))
                .kind(kind)
                .description(trim(request.description()))
                .imageUrl(trim(request.imageUrl()))
                .category(trim(request.category()))
                .price(request.price())
                .maxPerOrder(request.maxPerOrder())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .published(Boolean.FALSE)
                .createdBy(actor)
                .build();
        String defaultTitle;
        String defaultCurrency;
        if (kind == MarketplaceOffer.Kind.PRODUCT) {
            if (!StringUtils.hasText(request.inventoryItemCode()) || !StringUtils.hasText(request.locationCode())) {
                throw new BadRequestException("Un article se vend depuis un emplacement : précisez l'article et l'emplacement.");
            }
            InventoryItem item = item(request.inventoryItemCode().trim());
            InventoryLocation location = location(request.locationCode().trim());
            offer.setInventoryItemCode(item.getItemCode());
            offer.setLocationCode(location.getLocationCode());
            defaultTitle = item.getName();
            defaultCurrency = "XAF";
        } else {
            if (!StringUtils.hasText(request.serviceItemCode())) {
                throw new BadRequestException("Précisez la prestation du catalogue que l'offre vend.");
            }
            ServiceCatalogItem service = serviceItem(request.serviceItemCode().trim());
            offer.setServiceItemCode(service.getItemCode());
            defaultTitle = service.getName();
            defaultCurrency = service.getCurrency();
            if (offer.getDescription() == null) {
                offer.setDescription(service.getDescription());
            }
            if (offer.getImageUrl() == null) {
                offer.setImageUrl(service.getImageUrl());
            }
            if (offer.getCategory() == null) {
                offer.setCategory(service.getCategory());
            }
        }
        offer.setTitle(StringUtils.hasText(request.title()) ? request.title().trim() : defaultTitle);
        String currency = StringUtils.hasText(request.currency()) ? request.currency() : defaultCurrency;
        offer.setCurrency((StringUtils.hasText(currency) ? currency : "XAF").trim().toUpperCase(Locale.ROOT));
        return view(offerRepository.saveAndFlush(offer));
    }

    @Transactional
    public MarketplaceDtos.OfferView update(Long id, MarketplaceDtos.OfferUpdateRequest request) {
        MarketplaceOffer offer = offer(id);
        if (StringUtils.hasText(request.title())) {
            offer.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            offer.setDescription(trim(request.description()));
        }
        if (request.imageUrl() != null) {
            offer.setImageUrl(trim(request.imageUrl()));
        }
        if (request.category() != null) {
            offer.setCategory(trim(request.category()));
        }
        if (Boolean.TRUE.equals(request.resetPrice())) {
            offer.setPrice(null);
        } else if (request.price() != null) {
            offer.setPrice(request.price());
        }
        if (Boolean.TRUE.equals(request.clearMaxPerOrder())) {
            offer.setMaxPerOrder(null);
        } else if (request.maxPerOrder() != null) {
            offer.setMaxPerOrder(request.maxPerOrder());
        }
        if (request.displayOrder() != null) {
            offer.setDisplayOrder(request.displayOrder());
        }
        if (Boolean.TRUE.equals(offer.getPublished())) {
            ensurePublishable(offer);
        }
        return view(offerRepository.save(offer));
    }

    @Transactional
    public MarketplaceDtos.OfferView publish(Long id, boolean published) {
        MarketplaceOffer offer = offer(id);
        if (published) {
            ensurePublishable(offer);
        }
        offer.setPublished(published);
        return view(offerRepository.save(offer));
    }

    /** Le prix demande · celui de l'offre, sinon celui de l'article ou de la prestation. */
    public Optional<BigDecimal> priceOf(MarketplaceOffer offer) {
        if (offer.getPrice() != null) {
            return Optional.of(offer.getPrice());
        }
        if (offer.getKind() == MarketplaceOffer.Kind.PRODUCT) {
            return itemRepository.findByItemCode(offer.getInventoryItemCode())
                    .map(InventoryItem::getSalePrice)
                    .map(BigDecimal::valueOf);
        }
        return serviceItemRepository.findByItemCode(offer.getServiceItemCode())
                .map(ServiceCatalogItem::getUnitPrice);
    }

    /** Ce qui reste a vendre a l'emplacement · vide pour une prestation, qui ne s'epuise pas. */
    public BigDecimal availableOf(MarketplaceOffer offer) {
        if (offer.getKind() != MarketplaceOffer.Kind.PRODUCT) {
            return null;
        }
        Optional<InventoryItem> item = itemRepository.findByItemCode(offer.getInventoryItemCode());
        Optional<InventoryLocation> location = locationRepository.findByLocationCode(offer.getLocationCode());
        if (item.isEmpty() || location.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return stockLevelRepository.findByItemAndLocationReadOnly(item.get(), location.get())
                .map(StockLevel::getQuantityAvailable)
                .orElse(BigDecimal.ZERO);
    }

    /** Publiee, avec un prix, et ce qu'elle vend toujours actif. */
    public boolean sellable(MarketplaceOffer offer) {
        if (!Boolean.TRUE.equals(offer.getPublished())) {
            return false;
        }
        if (priceOf(offer).filter(price -> price.signum() > 0).isEmpty()) {
            return false;
        }
        if (offer.getKind() == MarketplaceOffer.Kind.PRODUCT) {
            return itemRepository.findByItemCode(offer.getInventoryItemCode())
                    .map(item -> !Boolean.FALSE.equals(item.getActive())).orElse(false);
        }
        return serviceItemRepository.findByItemCode(offer.getServiceItemCode())
                .map(service -> !Boolean.FALSE.equals(service.getActive())).orElse(false);
    }

    public MarketplaceOffer offer(Long id) {
        return offerRepository.findByShortId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + id));
    }

    public MarketplaceDtos.OfferView view(MarketplaceOffer offer) {
        return new MarketplaceDtos.OfferView(
                offer.getShortId(),
                offer.getOfferCode(),
                offer.getKind(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getImageUrl(),
                offer.getCategory(),
                priceOf(offer).orElse(null),
                offer.getPrice(),
                offer.getCurrency(),
                availableOf(offer),
                offer.getMaxPerOrder(),
                Boolean.TRUE.equals(offer.getPublished()),
                offer.getDisplayOrder() == null ? 0 : offer.getDisplayOrder(),
                offer.getInventoryItemCode(),
                offer.getLocationCode(),
                offer.getServiceItemCode());
    }

    private void ensurePublishable(MarketplaceOffer offer) {
        if (priceOf(offer).filter(price -> price.signum() > 0).isEmpty()) {
            throw new BadRequestException("L'offre « " + offer.getTitle() + " » n'a pas de prix : donnez-lui le sien, "
                    + "ou un prix de vente à l'article ou à la prestation.");
        }
        if (offer.getKind() == MarketplaceOffer.Kind.PRODUCT) {
            InventoryItem item = item(offer.getInventoryItemCode());
            if (Boolean.FALSE.equals(item.getActive())) {
                throw new BadRequestException("L'article " + item.getItemCode() + " n'est plus actif.");
            }
        } else if (Boolean.FALSE.equals(serviceItem(offer.getServiceItemCode()).getActive())) {
            throw new BadRequestException("La prestation " + offer.getServiceItemCode() + " n'est plus active.");
        }
    }

    private InventoryItem item(String code) {
        return itemRepository.findByItemCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable : " + code));
    }

    private InventoryLocation location(String code) {
        return locationRepository.findByLocationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Emplacement introuvable : " + code));
    }

    private ServiceCatalogItem serviceItem(String code) {
        return serviceItemRepository.findByItemCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation introuvable : " + code));
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
