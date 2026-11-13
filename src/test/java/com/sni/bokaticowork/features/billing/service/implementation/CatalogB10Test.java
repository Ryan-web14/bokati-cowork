package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.dto.request.CatalogPriceRequest;
import com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogPrice;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogPriceRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B10 : prix par segment et dates avec historique, import CSV, codes d'erreur stables. */
class CatalogB10Test {

    private final ServiceCatalogItemRepository items = mock(ServiceCatalogItemRepository.class);
    private final ServiceCatalogPriceRepository prices = mock(ServiceCatalogPriceRepository.class);
    private final CatalogPriceService service = new CatalogPriceService(items, prices, mock(InventoryItemRepository.class));
    private final List<ServiceCatalogPrice> saved = new ArrayList<>();
    private final ServiceCatalogItem office = ServiceCatalogItem.builder().itemCode("BUR-1").name("Bureau").unitPrice(new BigDecimal("150000"))
            .costPrice(new BigDecimal("90000")).active(true).build();

    @BeforeEach
    void setUp() {
        when(items.findByItemCode("BUR-1")).thenReturn(Optional.of(office));
        when(items.save(any(ServiceCatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prices.save(any(ServiceCatalogPrice.class))).thenAnswer(invocation -> {
            ServiceCatalogPrice price = invocation.getArgument(0);
            if (!saved.contains(price)) {
                saved.add(price);
            }
            return price;
        });
        when(prices.findByItemCodeAndSegmentOrderByValidFromDescIdDesc(anyString(), anyString())).thenAnswer(invocation -> saved.stream()
                .filter(price -> price.getSegment().equals(invocation.getArgument(1)))
                .sorted(Comparator.comparing(ServiceCatalogPrice::getValidFrom).reversed()).toList());
    }

    @Test
    void shouldResolveTheSegmentPriceAtADateAndKeepTheHistory() {
        LocalDate january = LocalDate.of(2027, 1, 1);
        service.setPrice("BUR-1", new CatalogPriceRequest("MEMBER", new BigDecimal("120000"), LocalDate.of(2026, 11, 1), "Tarif membre"));
        service.setPrice("BUR-1", new CatalogPriceRequest("MEMBER", new BigDecimal("125000"), january, "Hausse annuelle"));

        assertThat(service.price("BUR-1", "MEMBER", LocalDate.of(2026, 12, 15)).unitPrice()).isEqualByComparingTo("120000");
        assertThat(service.price("BUR-1", "MEMBER", LocalDate.of(2027, 2, 1)).unitPrice()).isEqualByComparingTo("125000");
        // Le premier prix s'arrete la veille du second ; rien n'est efface.
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getValidUntil()).isEqualTo(january.minusDays(1));
        // Sans prix pour le segment, le prix de la fiche.
        CatalogPriceResponse visitor = service.price("BUR-1", "VISITOR", LocalDate.of(2026, 12, 15));
        assertThat(visitor.source()).isEqualTo("CATALOG");
        assertThat(visitor.unitPrice()).isEqualByComparingTo("150000");
    }

    @Test
    void shouldImportTheCatalogAndReportBadLines() {
        when(items.findByItemCode("SAL-2")).thenReturn(Optional.empty());

        CatalogPriceService.ImportResult result = service.importCsv(CatalogPriceService.CSV_HEADER + "\n"
                + "BUR-1;Bureau privatif;Espaces;Bureaux;mois;160000;XAF;90000;100000;10;;7061;true\n"
                + "SAL-2;Salle de réunion;Espaces;Salles;heure;25000;XAF;;;;;;true\n"
                + ";sans code;;;;;;;;;;;\n");

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(error -> assertThat(error).startsWith("Ligne 4"));
        assertThat(office.getRevenueAccount()).isEqualTo("7061");
        assertThat(office.getUnitPrice()).isEqualByComparingTo("160000");
    }

    @Test
    void shouldPreviewTheMargin() {
        assertThat(CatalogPriceService.margin(office).marginRate()).isEqualByComparingTo("40.0");
    }

    @Test
    void shouldCarryAStableReasonCode() {
        assertThat(BadRequestException.coded("QUOTE_NOT_ACCEPTED", "Un acompte se demande sur un devis accepté").getReason())
                .isEqualTo("QUOTE_NOT_ACCEPTED");
        ConflictException conflict = ConflictException.coded("CREDIT_NOTE", "l'avoir a déjà été consommé");
        assertThat(conflict.getMessage()).isEqualTo("l'avoir a déjà été consommé");
        assertThat(conflict.getReason()).isEqualTo("CREDIT_NOTE");
    }
}
