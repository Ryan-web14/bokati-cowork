package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.features.accounting.model.AccSupplier;
import com.sni.bokaticowork.features.accounting.repository.AccSupplierRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** La comptabilite voit aussi les fournisseurs de l'inventaire, sans doublon avec ceux deja lies. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseSuppliersListTest {

    @Mock private AccSupplierRepository suppliers;
    @Mock private SupplierRepository inventorySuppliers;
    @Mock private AccountingSettings settings;
    @InjectMocks private PurchaseService service;

    private static Supplier inventory(long id, String code, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setSupplierCode(code);
        s.setName(name);
        s.setStatus(SupplierStatus.ACTIVE);
        return s;
    }

    @Test
    void inventorySuppliersAreListedAndLinkedOnesAppearOnce() {
        AccSupplier linked = AccSupplier.builder().code("SUP-1").name("Bureau Plus").inventorySupplierId(1L).status("ACTIVE").build();
        AccSupplier local = AccSupplier.builder().code("FRN-9").name("Avocat Ngoma").status("ACTIVE").build();
        when(suppliers.findAllByOrderByNameAsc()).thenReturn(List.of(linked, local));
        when(inventorySuppliers.findAllByStatusOrderByNameAsc(SupplierStatus.ACTIVE)).thenReturn(List.of(
                inventory(1, "SUP-1", "Bureau Plus"), inventory(2, "SUP-2", "Café du Port")));
        when(settings.text(anyString(), anyString())).thenReturn("401");

        List<PurchaseService.SupplierView> views = service.suppliers();

        assertThat(views).extracting(PurchaseService.SupplierView::code).containsExactly("FRN-9", "SUP-1", "SUP-2");
        assertThat(views).filteredOn(v -> v.code().equals("SUP-1")).singleElement()
                .satisfies(v -> {
                    assertThat(v.source()).isEqualTo("ACCOUNTING");
                    assertThat(v.linked()).isTrue();
                });
        assertThat(views).filteredOn(v -> v.code().equals("SUP-2")).singleElement()
                .satisfies(v -> {
                    assertThat(v.source()).isEqualTo("INVENTORY");
                    assertThat(v.linked()).isFalse();
                    assertThat(v.accountNumber()).isEqualTo("401");
                });
    }

    @Mock private com.sni.bokaticowork.features.payment.service.support.CashActorResolver actors;

    @Test
    void anInventorySupplierWithoutAccountingRecordGetsOne() {
        Supplier inventory = inventory(5, "SUP-5", "Imprimerie Moderne");
        inventory.setTaxId("M123");
        when(suppliers.findByInventorySupplierId(5L)).thenReturn(java.util.Optional.empty());
        when(suppliers.existsByCode("SUP-5")).thenReturn(false);
        when(settings.text(anyString(), anyString())).thenReturn("401");
        when(suppliers.save(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.syncFromInventory(inventory)).isTrue();

        org.mockito.ArgumentCaptor<AccSupplier> saved = org.mockito.ArgumentCaptor.forClass(AccSupplier.class);
        org.mockito.Mockito.verify(suppliers).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("SUP-5");
        assertThat(saved.getValue().getNiu()).isEqualTo("M123");
        assertThat(saved.getValue().getInventorySupplierId()).isEqualTo(5L);
    }

    @Test
    void aLinkedSupplierFollowsTheInventoryRecordButKeepsItsAccountingFields() {
        AccSupplier linked = AccSupplier.builder().code("SUP-5").name("Ancien nom").inventorySupplierId(5L)
                .accountNumber("4011").paymentTermsDays(45).status("ACTIVE").build();
        when(suppliers.findByInventorySupplierId(5L)).thenReturn(java.util.Optional.of(linked));
        Supplier inventory = inventory(5, "SUP-5", "Imprimerie Moderne");
        inventory.setStatus(SupplierStatus.INACTIVE);

        assertThat(service.syncFromInventory(inventory)).isFalse();

        assertThat(linked.getName()).isEqualTo("Imprimerie Moderne");
        assertThat(linked.getStatus()).isEqualTo("INACTIVE");
        assertThat(linked.getAccountNumber()).isEqualTo("4011");
        assertThat(linked.getPaymentTermsDays()).isEqualTo(45);
    }
}
