package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryBarcodeRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryPackagingRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryBarcodeResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryPackagingResponse;

import java.util.List;

/**
 * Codes-barres et conditionnements d'un article.
 *
 * <p>Un article porte souvent plusieurs codes : celui du fournisseur, un code interne, et un code
 * par niveau de conditionnement. Les separer de l'article evite d'ajouter une colonne a chaque
 * nouveau besoin.</p>
 */
public interface InventoryIdentificationService {

    InventoryBarcodeResponse addBarcode(String itemCode, InventoryBarcodeRequest request);

    List<InventoryBarcodeResponse> listBarcodes(String itemCode);

    /** Promeut un code en code principal, en retirant la marque au precedent. */
    InventoryBarcodeResponse markPrimary(Long barcodeId);

    void deleteBarcode(Long barcodeId);

    InventoryPackagingResponse addPackaging(String itemCode, InventoryPackagingRequest request);

    List<InventoryPackagingResponse> listPackagings(String itemCode);

    void deletePackaging(Long packagingId);
}
