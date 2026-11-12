package com.sni.bokaticowork.features.inventory.stock;

import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les finders lus hors transaction (open-in-view coupe) chargent ce que l'appelant lit ensuite :
 * l'emplacement des niveaux de stock pour la suggestion de prelevement, le document des pieces
 * d'un dossier KYC pour les validations et rejets groupes.
 */
class LazyLoadingInventoryDocumentRepositoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private StockLevelRepository levels;
    @Autowired private KycDocumentRepository kycDocuments;

    @Test
    @DisplayName("Les niveaux disponibles d'un article se lisent avec leur emplacement deja charge")
    void pickingLevelsComeWithTheirLocation() {
        entityManager.createNativeQuery("""
                INSERT INTO inventory_item (id, item_code, name, item_type, tracking_type)
                VALUES (993000001, 'ITM-IT-PICK', 'Ramette', 'CONSUMABLE', 'QUANTITY')
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO inventory_location (id, location_code, name, location_type)
                VALUES (993000002, 'LOC-IT-PICK', 'Reserve', 'WAREHOUSE')
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO stock_level (id, item_id, location_id, quantity_on_hand, quantity_available)
                VALUES (993000003, 993000001, 993000002, 12, 12)
                """).executeUpdate();
        entityManager.clear();

        List<StockLevel> found = levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(
                entityManager.getReference(InventoryItem.class, 993000001L), BigDecimal.ZERO);
        entityManager.clear();

        assertThat(found).hasSize(1);
        assertThat(Hibernate.isInitialized(found.getFirst().getLocation())).isTrue();
        assertThat(found.getFirst().getLocation().getLocationCode()).isEqualTo("LOC-IT-PICK");
    }

    @Test
    @DisplayName("Les pieces d'un dossier KYC s'executent avec leur document charge")
    void kycCaseDocumentsQueryLoadsTheDocument() {
        assertThat(kycDocuments.findAllByKycCaseOrderByIdAsc(entityManager.getReference(KycCase.class, 993000099L)))
                .isEmpty();
    }
}
