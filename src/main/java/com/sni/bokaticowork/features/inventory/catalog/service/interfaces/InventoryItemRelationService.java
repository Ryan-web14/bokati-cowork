package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemSubstituteRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTranslationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemSubstituteResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTranslationResponse;

import java.util.List;

/**
 * Relations d'un article avec d'autres articles, et libelles multilingues.
 */
public interface InventoryItemRelationService {

    InventoryItemSubstituteResponse addSubstitute(String itemCode, InventoryItemSubstituteRequest request);

    /**
     * Substituts de l'article, declarations directes et reciproques confondues, tries par priorite.
     */
    List<InventoryItemSubstituteResponse> listSubstitutes(String itemCode);

    void deleteSubstitute(Long substituteId);

    /** Cree ou met a jour la traduction pour la langue fournie. */
    InventoryItemTranslationResponse upsertTranslation(String itemCode, InventoryItemTranslationRequest request);

    List<InventoryItemTranslationResponse> listTranslations(String itemCode);

    void deleteTranslation(Long translationId);
}
