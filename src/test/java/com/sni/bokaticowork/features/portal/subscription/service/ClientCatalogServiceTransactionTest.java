package com.sni.bokaticowork.features.portal.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les achats du portail se font d'un bloc : lire le plan et creer le pass dans la meme transaction,
 * pour que rien ne reste a moitie enregistre si la creation echoue.
 */
class ClientCatalogServiceTransactionTest {

    @Test
    @DisplayName("L'achat d'un pass pour soi et pour son organisation est transactionnel, en ecriture")
    void purchasesAreTransactional() {
        for (String name : new String[]{"purchasePass", "purchasePassForCompany", "subscribe"}) {
            Method method = Arrays.stream(ClientCatalogService.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name) && java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                    .findFirst().orElseThrow();
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).as(name).isNotNull();
            assertThat(transactional.readOnly()).as(name).isFalse();
        }
    }
}
