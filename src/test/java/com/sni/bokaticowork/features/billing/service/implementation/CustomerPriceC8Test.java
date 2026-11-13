package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.CustomerPriceDtos;
import com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse;
import com.sni.bokaticowork.features.billing.model.CustomerPrice;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.CustomerPriceRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogPriceRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot C8 : le client sous contrat obtient son prix (source CONTRACT) ; un taux negocie s'applique
 * au prix de base ; a l'expiration du contrat, le prix du segment ou de la fiche revient ; un
 * nouveau tarif arrete le precedent la veille.
 */
class CustomerPriceC8Test {

    private final CustomerPriceRepository repository = mock(CustomerPriceRepository.class);
    private final ServiceCatalogItemRepository items = mock(ServiceCatalogItemRepository.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final CustomerPriceService service = new CustomerPriceService(repository, items, staff);
    private final ServiceCatalogPriceRepository catalogPrices = mock(ServiceCatalogPriceRepository.class);
    private final CatalogPriceService prices = new CatalogPriceService(items, catalogPrices, mock(InventoryItemRepository.class));
    private final List<CustomerPrice> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(prices, "customerPrices", service);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("commercial@elleaose.com", "Commercial", null));
        when(items.findByItemCode("SRV-BUREAU")).thenReturn(Optional.of(ServiceCatalogItem.builder().itemCode("SRV-BUREAU").name("Bureau privatif")
                .unitPrice(new BigDecimal("500000")).currency("XAF").build()));
        when(catalogPrices.findByItemCodeAndSegmentOrderByValidFromDescIdDesc(anyString(), anyString())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(invocation -> {
            CustomerPrice price = invocation.getArgument(0);
            if (!stored.contains(price)) {
                stored.add(0, price);
            }
            return price;
        });
        when(repository.findByCustomerTypeAndCustomerCodeAndItemCodeOrderByValidFromDesc("COMPANY", "ORG-5", "SRV-BUREAU"))
                .thenAnswer(invocation -> List.copyOf(stored));
    }

    @Test
    void shouldGiveTheContractPriceFirst() {
        service.create("COMPANY", "ORG-5", new CustomerPriceDtos.PriceRequest("SRV-BUREAU", new BigDecimal("420000"), null,
                LocalDate.now().minusDays(10), null, "CTR-2026-03", "Contrat 3 ans"));

        CatalogPriceResponse price = prices.price("SRV-BUREAU", "COMPANY", null, "COMPANY", "ORG-5");
        assertThat(price.source()).isEqualTo("CONTRACT");
        assertThat(price.unitPrice()).isEqualByComparingTo("420000");
        assertThat(prices.price("SRV-BUREAU", "COMPANY", null, "COMPANY", "ORG-6").source()).isEqualTo("CATALOG");
    }

    @Test
    void shouldApplyANegotiatedRateAndStopThePreviousPrice() {
        service.create("COMPANY", "ORG-5", new CustomerPriceDtos.PriceRequest("SRV-BUREAU", new BigDecimal("420000"), null,
                LocalDate.now().minusMonths(6), null, null, null));
        service.create("COMPANY", "ORG-5", new CustomerPriceDtos.PriceRequest("SRV-BUREAU", null, new BigDecimal("10"),
                LocalDate.now(), null, "CTR-2026-09", null));

        assertThat(stored.get(1).getValidUntil()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(prices.price("SRV-BUREAU", null, null, "COMPANY", "ORG-5").unitPrice()).isEqualByComparingTo("450000");
    }

    @Test
    void shouldReturnToTheCatalogPriceOnceTheContractHasEnded() {
        service.create("COMPANY", "ORG-5", new CustomerPriceDtos.PriceRequest("SRV-BUREAU", new BigDecimal("420000"), null,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(1), null, null));

        assertThat(prices.price("SRV-BUREAU", null, null, "COMPANY", "ORG-5").source()).isEqualTo("CATALOG");
    }

    @Test
    void shouldAskForEitherAPriceOrARate() {
        assertThatThrownBy(() -> service.create("COMPANY", "ORG-5", new CustomerPriceDtos.PriceRequest("SRV-BUREAU", new BigDecimal("1"),
                new BigDecimal("5"), LocalDate.now(), null, null, null)))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("TARIF_INVALIDE"));
    }
}
