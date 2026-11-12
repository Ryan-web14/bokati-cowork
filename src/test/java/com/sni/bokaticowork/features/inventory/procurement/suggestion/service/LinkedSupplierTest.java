package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LinkedSupplierTest {

    private final SupplierRepository repository = mock(SupplierRepository.class);

    @Test
    @DisplayName("Sans code : le texte libre, comme avant, sans toucher aux fiches")
    void freeText() {
        assertThat(LinkedSupplier.resolve(repository, null, "  Quincaillerie du coin ", 1))
                .isEqualTo(new LinkedSupplier.Resolved(null, "Quincaillerie du coin"));
        assertThat(LinkedSupplier.resolve(repository, " ", null, 1)).isEqualTo(new LinkedSupplier.Resolved(null, null));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Avec un code : la fiche active donne son nom ; inconnue ou inactive, refusee")
    void linkedSupplier() {
        when(repository.findBySupplierCode(anyString())).thenReturn(Optional.empty());
        when(repository.findBySupplierCode("SUP-1")).thenReturn(Optional.of(Supplier.builder().id(1L).supplierCode("SUP-1")
                .name("Papeterie Centrale").status(SupplierStatus.ACTIVE).build()));
        when(repository.findBySupplierCode("SUP-2")).thenReturn(Optional.of(Supplier.builder().id(2L).supplierCode("SUP-2")
                .name("Ancien fournisseur").status(SupplierStatus.INACTIVE).build()));

        assertThat(LinkedSupplier.resolve(repository, "sup-1", "autre nom", 1)).isEqualTo(new LinkedSupplier.Resolved("SUP-1", "Papeterie Centrale"));
        assertThatThrownBy(() -> LinkedSupplier.resolve(repository, "SUP-9", null, 2))
                .isInstanceOf(BadRequestException.class).hasMessage("Ligne 2 : fournisseur inconnu : SUP-9");
        assertThatThrownBy(() -> LinkedSupplier.resolve(repository, "SUP-2", null, 3))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("n'est pas actif");
    }
}
