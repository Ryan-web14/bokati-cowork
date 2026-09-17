package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.LotBlockRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotQuarantineRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotReleaseRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;

import java.util.List;

/**
 * Immobilisation et remise en circulation des lots.
 *
 * <p>Deux mecanismes distincts. La quarantaine est un etat d attente decide par le controle
 * qualite, dont la levee exige un second regard. Le blocage est une decision de gestion, levee par
 * qui l a posee. Les deux retirent le lot du stock disponible.</p>
 */
public interface StockQuarantineService {

    StockLotResponse quarantine(Long lotId, LotQuarantineRequest request);

    /**
     * Leve la quarantaine. Exige un approbateur different du demandeur : liberer un lot declare non
     * conforme ne doit pas pouvoir se faire seul.
     */
    StockLotResponse release(Long lotId, LotReleaseRequest request);

    StockLotResponse block(Long lotId, LotBlockRequest request);

    StockLotResponse unblock(Long lotId, String unblockedBy);

    /** Lots actuellement immobilises, en quarantaine ou bloques. */
    List<StockLotResponse> listImmobilised(String itemCode, String locationCode);
}
