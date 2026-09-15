package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemLifecycleRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRevisionRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemPriceHistoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemRevisionHistoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryItemService {

    InventoryItemResponse create(InventoryItemRequest request);

    InventoryItemResponse update(String itemCode, InventoryItemRequest request);

    InventoryItemResponse get(String itemCode);

    Page<InventoryItemResponse> search(String query, String categoryCode, InventoryItemType itemType, Boolean active, Pageable pageable);

    InventoryItemResponse activate(String itemCode);

    InventoryItemResponse deactivate(String itemCode);

    void delete(String itemCode);

    /**
     * Change le statut de cycle de vie, qui pilote ce que l'article a encore le droit de faire.
     *
     * <p>Plus expressif que {@code activate} et {@code deactivate}, qui restent disponibles et sont
     * traduits en {@code ACTIVE} et {@code OBSOLETE}.</p>
     */
    InventoryItemResponse changeLifecycle(String itemCode, InventoryItemLifecycleRequest request);

    /**
     * Passe l'article a une nouvelle revision et journalise le changement.
     */
    InventoryItemResponse changeRevision(String itemCode, InventoryItemRevisionRequest request);

    /**
     * Cree une variante rattachee a un modele. Reutilise toute la generation de codes de
     * {@link #create(InventoryItemRequest)} pour que variantes et articles simples soient identiques
     * du point de vue du stock.
     */
    InventoryItemResponse createVariant(InventoryItemRequest request, String templateCode, String variantSignature);

    Page<InventoryItemPriceHistoryResponse> priceHistory(String itemCode, Pageable pageable);

    List<InventoryItemRevisionHistoryResponse> revisionHistory(String itemCode);
}
