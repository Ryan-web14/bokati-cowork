package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {
    WalletResponse adminTopUp(WalletTopUpRequest request);
    WalletResponse getOrCreate(String ownerType, String ownerCode, String currency);
    WalletResponse get(String walletNumber);
    PaginatedResponse<WalletResponse> list(String ownerType, String ownerCode, Pageable pageable);
    PaginatedResponse<WalletLedgerEntryResponse> ledger(String walletNumber, Pageable pageable);
    WalletAccount serviceWallet(String walletNumber);

    /**
     * Credite le portefeuille.
     *
     * @param idempotencyKey cle stable identifiant l'operation metier, ou {@code null} lorsque
     *                       l'operation est legitimement repetable (remboursements partiels
     *                       successifs sur une meme transaction, par exemple). Un second appel
     *                       avec la meme cle non nulle renvoie l'ecriture d'origine sans
     *                       remuter le solde.
     */
    void credit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType,
                String sourceCode, String reference, String createdBy, String idempotencyKey);

    /** Debite le portefeuille. Voir {@link #credit} pour la semantique de {@code idempotencyKey}. */
    void debit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType,
               String sourceCode, String reference, String createdBy, String idempotencyKey);
}
