package com.sni.bokaticowork.features.inventory.catalog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLifecycleStatusTest {

    @Test
    void phaseOutStopsReplenishmentButKeepsIssuingPossible() {
        // C'est tout l'interet du statut : ecouler le stock restant sans en racheter.
        assertFalse(ItemLifecycleStatus.PHASE_OUT.isReceivable());
        assertTrue(ItemLifecycleStatus.PHASE_OUT.isIssuable());
        assertTrue(ItemLifecycleStatus.PHASE_OUT.isActive());
    }

    @Test
    void draftObsoleteAndBlockedAllowNoMovement() {
        for (ItemLifecycleStatus status : new ItemLifecycleStatus[]{
                ItemLifecycleStatus.DRAFT, ItemLifecycleStatus.OBSOLETE, ItemLifecycleStatus.BLOCKED}) {
            assertFalse(status.isReceivable(), status + " ne doit pas accepter d entree");
            assertFalse(status.isIssuable(), status + " ne doit pas accepter de sortie");
            assertFalse(status.isActive(), status + " ne doit pas etre actif");
        }
    }

    @Test
    void newAndActiveAllowEverything() {
        for (ItemLifecycleStatus status : new ItemLifecycleStatus[]{
                ItemLifecycleStatus.NEW, ItemLifecycleStatus.ACTIVE}) {
            assertTrue(status.isReceivable());
            assertTrue(status.isIssuable());
            assertTrue(status.isActive());
        }
    }

    @Test
    void translatesTheLegacyActiveFlag() {
        assertEquals(ItemLifecycleStatus.ACTIVE, ItemLifecycleStatus.fromActiveFlag(Boolean.TRUE));
        assertEquals(ItemLifecycleStatus.OBSOLETE, ItemLifecycleStatus.fromActiveFlag(Boolean.FALSE));
        // Absence d information vaut actif, pour ne pas desactiver un article par omission.
        assertEquals(ItemLifecycleStatus.ACTIVE, ItemLifecycleStatus.fromActiveFlag(null));
    }
}
