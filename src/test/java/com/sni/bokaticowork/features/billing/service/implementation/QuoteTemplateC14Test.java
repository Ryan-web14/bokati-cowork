package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.QuoteTemplateDtos;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.CatalogComponent;
import com.sni.bokaticowork.features.billing.model.QuoteTemplate;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.CatalogComponentRepository;
import com.sni.bokaticowork.features.billing.repository.QuoteTemplateRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C14 : « Seminaire journee » cree un devis de quatre lignes aux prix du catalogue ; un article
 * compose ventile son prix entre ses composants au prorata de leur valeur.
 */
class QuoteTemplateC14Test {

    private final QuoteTemplateRepository templates = mock(QuoteTemplateRepository.class);
    private final CatalogComponentRepository components = mock(CatalogComponentRepository.class);
    private final ServiceCatalogItemRepository catalog = mock(ServiceCatalogItemRepository.class);
    private final InventoryItemRepository inventory = mock(InventoryItemRepository.class);
    private final BillingDocumentService billing = mock(BillingDocumentService.class);
    private QuoteTemplateService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingDocumentService> billingProvider = mock(ObjectProvider.class);
        when(billingProvider.getObject()).thenReturn(billing);
        service = new QuoteTemplateService(templates, components, catalog, inventory, new ObjectMapper(), mock(StaffProfileService.class),
                billingProvider, mock(ObjectProvider.class));
        item("SRV-SALLE", "150000");
        item("SRV-PAUSE", "5000");
        item("SRV-DEJ", "12000");
        item("SRV-VIDEO", "30000");
        item("SRV-PACK", "100000");
    }

    @Test
    void shouldCreateAQuoteFromTheSeminarTemplate() {
        when(templates.findByCode("SEMINAIRE")).thenReturn(Optional.of(QuoteTemplate.builder().code("SEMINAIRE").name("Séminaire journée")
                .validityDays(15).active(true).linesJson("""
                        [{"itemCode":"SRV-SALLE","quantity":1},{"itemCode":"SRV-PAUSE","quantity":40},
                         {"itemCode":"SRV-DEJ","quantity":20},{"itemCode":"SRV-VIDEO","quantity":1}]""").build()));
        when(billing.create(org.mockito.ArgumentMatchers.any())).thenReturn(mock(BillingDocumentResponse.class));

        service.quoteFromTemplate("seminaire", new QuoteTemplateDtos.FromTemplateRequest("COMPANY", "ORG-1", "Société Mbote", null, null, null, null));

        ArgumentCaptor<CreateBillingDocumentRequest> request = ArgumentCaptor.forClass(CreateBillingDocumentRequest.class);
        verify(billing).create(request.capture());
        assertThat(request.getValue().documentType()).isEqualTo(BillingDocumentType.QUOTE);
        assertThat(request.getValue().lines()).hasSize(4);
        assertThat(request.getValue().lines().get(1).quantity()).isEqualByComparingTo("40");
        assertThat(request.getValue().lines().get(1).unitPrice()).isEqualByComparingTo("5000");
        assertThat(request.getValue().title()).isEqualTo("Séminaire journée");
    }

    @Test
    void shouldSplitABundleBetweenItsComponents() {
        InventoryItem kit = InventoryItem.builder().itemCode("ART-KIT").name("Kit").salePrice(25000L).build();
        when(inventory.findByItemCode("ART-KIT")).thenReturn(Optional.of(kit));
        when(components.findByParentItemCodeOrderByDisplayOrderAsc("SRV-PACK")).thenReturn(List.of(
                CatalogComponent.builder().componentCode("SRV-SALLE").componentSource("SERVICE_CATALOG").quantity(BigDecimal.ONE).build(),
                CatalogComponent.builder().componentCode("ART-KIT").componentSource("INVENTORY_ITEM").quantity(new BigDecimal("2")).build()));

        List<QuoteTemplateService.Share> shares = service.split("SRV-PACK", new BigDecimal("100000"));

        // Valeurs : 150 000 et 2 x 25 000 = 50 000, soit 75 % et 25 %.
        assertThat(shares).extracting(share -> share.amount().toPlainString()).containsExactly("75000.00", "25000.00");
        assertThat(service.split("SRV-SALLE", new BigDecimal("1"))).isEmpty();
    }

    private void item(String code, String price) {
        when(catalog.findByItemCode(code)).thenReturn(Optional.of(ServiceCatalogItem.builder().itemCode(code).name(code)
                .unitPrice(new BigDecimal(price)).currency("XAF").build()));
    }
}
