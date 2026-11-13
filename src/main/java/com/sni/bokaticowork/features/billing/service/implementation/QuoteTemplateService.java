package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.QuoteTemplateDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.CatalogComponent;
import com.sni.bokaticowork.features.billing.model.QuoteTemplate;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.CatalogComponentRepository;
import com.sni.bokaticowork.features.billing.repository.QuoteTemplateRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lot C14 : devis types et articles composes.
 *
 * <p>Un modele de devis liste des articles du catalogue et leurs quantites ; un devis en est tire
 * pour un client, aux prix qui s'appliquent a lui (contrat, segment, fiche), pret a ajuster.</p>
 *
 * <p>Un article compose (lot d'articles) se vend a prix global ; ses composants (prestations du
 * catalogue ou articles d'inventaire) servent a ventiler le produit entre leurs comptes et a sortir
 * le stock composant par composant.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class QuoteTemplateService {

    static final String SERVICE_CATALOG = "SERVICE_CATALOG";
    static final String INVENTORY_ITEM = "INVENTORY_ITEM";

    private final QuoteTemplateRepository templates;
    private final CatalogComponentRepository components;
    private final ServiceCatalogItemRepository catalog;
    private final InventoryItemRepository inventory;
    private final ObjectMapper objectMapper;
    private final StaffProfileService staffProfiles;
    private final ObjectProvider<BillingDocumentService> billingServices;
    private final ObjectProvider<CatalogPriceService> catalogPrices;

    // ================================================================== modeles

    @Transactional(readOnly = true)
    public List<QuoteTemplateDtos.TemplateView> list(boolean includeInactive) {
        return templates.findAllByOrderByNameAsc().stream().filter(template -> includeInactive || Boolean.TRUE.equals(template.getActive()))
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public QuoteTemplateDtos.TemplateView get(String code) {
        return view(template(code));
    }

    /** Cree ou remplace un modele (meme code). */
    public QuoteTemplateDtos.TemplateView save(QuoteTemplateDtos.TemplateRequest request) {
        for (QuoteTemplateDtos.TemplateLine line : request.lines()) {
            catalogItem(line.itemCode());
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        QuoteTemplate template = templates.findByCode(code).orElseGet(() -> QuoteTemplate.builder().code(code)
                .createdBy(staffProfiles.currentAuthor().login()).build());
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setValidityDays(request.validityDays() == null ? 30 : request.validityDays());
        template.setTerms(request.terms());
        template.setLinesJson(write(request.lines()));
        if (request.active() != null) {
            template.setActive(request.active());
        }
        return view(templates.save(template));
    }

    /** Devis en brouillon tire d'un modele, aux prix du client. */
    public BillingDocumentResponse quoteFromTemplate(String code, QuoteTemplateDtos.FromTemplateRequest request) {
        QuoteTemplate template = template(code);
        if (!Boolean.TRUE.equals(template.getActive())) {
            throw BadRequestException.coded("MODELE_INACTIF", "Le modèle " + template.getCode() + " est désactivé.");
        }
        CatalogPriceService prices = catalogPrices.getIfAvailable();
        List<CreateBillingDocumentLineRequest> lines = new ArrayList<>();
        String currency = null;
        int order = 1;
        for (QuoteTemplateDtos.TemplateLine line : read(template.getLinesJson())) {
            ServiceCatalogItem item = catalogItem(line.itemCode());
            currency = currency == null ? item.getCurrency() : currency;
            BigDecimal price = prices == null ? item.getUnitPrice()
                    : prices.price(item.getItemCode(), request.segment(), LocalDate.now(), request.customerType(), request.customerCode()).unitPrice();
            lines.add(new CreateBillingDocumentLineRequest(order++, BillingLineType.SERVICE, item.getItemCode(),
                    StringUtils.hasText(line.description()) ? line.description() : item.getName(), item.getDetailedDescription(), line.quantity(),
                    price, BigDecimal.ZERO, BigDecimal.ZERO, null, false, null, null, SERVICE_CATALOG, item.getItemCode(), item.getUnit(), null, null,
                    null, null));
        }
        CreateBillingDocumentRequest quote = new CreateBillingDocumentRequest(BillingDocumentType.QUOTE, request.customerType(), request.customerCode(),
                request.customerName(), request.customerEmail(), request.customerPhone(), null, "QUOTE_TEMPLATE", template.getCode(),
                StringUtils.hasText(request.title()) ? request.title().trim() : template.getName(), template.getDescription(), template.getTerms(),
                currency == null ? "XAF" : currency, LocalDate.now(), LocalDate.now().plusDays(template.getValidityDays()), null, lines, List.of(), List.of(), null, null, null,
                null, null, null, null, null, null, null, null, null);
        return billingServices.getObject().create(quote);
    }

    // ================================================================== articles composes

    @Transactional(readOnly = true)
    public List<QuoteTemplateDtos.Component> components(String itemCode) {
        return components.findByParentItemCodeOrderByDisplayOrderAsc(catalogItem(itemCode).getItemCode()).stream()
                .map(component -> new QuoteTemplateDtos.Component(component.getComponentCode(), component.getComponentSource(), component.getQuantity()))
                .toList();
    }

    /** Remplace les composants d'un article ; une liste vide en fait un article simple. */
    public List<QuoteTemplateDtos.Component> setComponents(String itemCode, List<QuoteTemplateDtos.Component> wanted) {
        ServiceCatalogItem parent = catalogItem(itemCode);
        components.deleteByParent(parent.getItemCode());
        int order = 0;
        for (QuoteTemplateDtos.Component component : wanted) {
            String source = component.componentSource().trim().toUpperCase(Locale.ROOT);
            String code = component.componentCode().trim();
            if (SERVICE_CATALOG.equals(source)) {
                if (code.equalsIgnoreCase(parent.getItemCode())) {
                    throw BadRequestException.coded("COMPOSANT_INVALIDE", "Un article ne se compose pas de lui-même.");
                }
                catalogItem(code);
            } else if (INVENTORY_ITEM.equals(source)) {
                inventory.findByItemCode(code).orElseThrow(() -> new ResourceNotFoundException("Article d'inventaire introuvable : " + code));
            } else {
                throw BadRequestException.coded("COMPOSANT_INVALIDE", "componentSource vaut SERVICE_CATALOG ou INVENTORY_ITEM.");
            }
            components.save(CatalogComponent.builder().parentItemCode(parent.getItemCode()).componentCode(code).componentSource(source)
                    .quantity(component.quantity()).displayOrder(order++).build());
        }
        return components(parent.getItemCode());
    }

    /**
     * Ventilation d'un montant d'article compose entre ses composants, au prorata de leur valeur
     * (prix du catalogue ou prix de vente d'inventaire, fois la quantite). L'arrondi va au dernier.
     * Vide si l'article n'est pas compose ou si ses composants n'ont pas de valeur.
     */
    @Transactional(readOnly = true)
    public List<Share> split(String itemCode, BigDecimal amount) {
        if (!StringUtils.hasText(itemCode)) {
            return List.of();
        }
        List<CatalogComponent> parts = components.findByParentItemCodeOrderByDisplayOrderAsc(itemCode.trim());
        if (parts.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> weights = parts.stream().map(part -> value(part).multiply(part.getQuantity())).toList();
        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return List.of();
        }
        List<Share> shares = new ArrayList<>();
        BigDecimal left = amount;
        for (int index = 0; index < parts.size(); index++) {
            BigDecimal share = index == parts.size() - 1 ? left
                    : amount.multiply(weights.get(index)).divide(total, 2, RoundingMode.HALF_UP);
            left = left.subtract(share);
            shares.add(new Share(parts.get(index).getComponentCode(), parts.get(index).getComponentSource(), share));
        }
        return shares;
    }

    /** Part d'un composant dans le montant d'un article compose. */
    public record Share(String componentCode, String componentSource, BigDecimal amount) {
    }

    // ================================================================== outils

    private BigDecimal value(CatalogComponent part) {
        if (SERVICE_CATALOG.equals(part.getComponentSource())) {
            return catalog.findByItemCode(part.getComponentCode()).map(ServiceCatalogItem::getUnitPrice).orElse(BigDecimal.ZERO);
        }
        return inventory.findByItemCode(part.getComponentCode())
                .map(item -> item.getSalePrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getSalePrice())).orElse(BigDecimal.ZERO);
    }

    private ServiceCatalogItem catalogItem(String code) {
        return catalog.findByItemCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Article du catalogue introuvable : " + code));
    }

    private QuoteTemplate template(String code) {
        return templates.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Modèle de devis introuvable : " + code));
    }

    private String write(List<QuoteTemplateDtos.TemplateLine> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Lignes illisibles : " + exception.getOriginalMessage());
        }
    }

    private List<QuoteTemplateDtos.TemplateLine> read(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuoteTemplateDtos.TemplateLine>>() {});
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Modèle illisible : " + exception.getOriginalMessage());
        }
    }

    private QuoteTemplateDtos.TemplateView view(QuoteTemplate template) {
        return new QuoteTemplateDtos.TemplateView(template.getCode(), template.getName(), template.getDescription(), template.getValidityDays(),
                template.getTerms(), read(template.getLinesJson()), Boolean.TRUE.equals(template.getActive()), template.getCreatedBy(),
                template.getUpdatedAt());
    }
}
