package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Fournisseur d'une ligne (etat de besoin, demande de fonds, justification) : la fiche
 * d'inventaire quand un code est donne, sinon le texte libre, comme avant.
 */
public final class LinkedSupplier {

    /** Longueur de la colonne texte {@code supplier} des lignes. */
    private static final int NAME_MAX = 255;

    private LinkedSupplier() {
    }

    public record Resolved(String code, String name) {
    }

    /**
     * Avec un code : la fiche doit exister et etre active ; son nom remplace le texte libre.
     * Sans code : le texte libre, nettoye.
     */
    public static Resolved resolve(SupplierRepository repository, String supplierCode, String supplier, int lineNo) {
        if (!StringUtils.hasText(supplierCode)) {
            return new Resolved(null, StringUtils.hasText(supplier) ? supplier.trim() : null);
        }
        Supplier found = find(repository, supplierCode)
                .orElseThrow(() -> new BadRequestException("Ligne " + lineNo + " : fournisseur inconnu : " + supplierCode.trim()));
        if (found.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException("Ligne " + lineNo + " : le fournisseur " + found.getName() + " n'est pas actif");
        }
        String name = found.getName();
        return new Resolved(found.getSupplierCode(), name.length() > NAME_MAX ? name.substring(0, NAME_MAX) : name);
    }

    public static Optional<Supplier> find(SupplierRepository repository, String supplierCode) {
        if (!StringUtils.hasText(supplierCode)) {
            return Optional.empty();
        }
        String code = supplierCode.trim();
        return repository.findBySupplierCode(code).or(() -> repository.findBySupplierCode(code.toUpperCase(Locale.ROOT)));
    }
}
