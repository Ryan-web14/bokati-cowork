package com.sni.bokaticowork.features.payment.service;

import com.sni.bokaticowork.features.payment.service.implementation.PaymentReceiptServiceImpl;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le PDF du recu cote administration lit l'intention de paiement (chargement paresseux) : il doit
 * s'executer dans la transaction de l'implementation. Une methode par defaut de l'interface
 * echappait au {@code @Transactional} de la classe (LazyInitializationException en production).
 */
class PaymentReceiptTransactionTest {

    @Test
    @DisplayName("Les impressions de l'administration sont declarees dans la classe transactionnelle, pas en methodes par defaut")
    void adminPrintsRunInsideTheTransaction() throws Exception {
        assertThat(PaymentReceiptServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        for (String name : new String[]{"generateAdminPdfByTransactionNumber", "generateAdminPdfByReceiptNumber"}) {
            Method declared = PaymentReceiptServiceImpl.class.getDeclaredMethod(name, String.class);
            assertThat(declared.getDeclaringClass()).isEqualTo(PaymentReceiptServiceImpl.class);
            Method contract = PaymentReceiptService.class.getMethod(name, String.class);
            assertThat(contract.isDefault()).as(name + " ne doit pas etre une methode par defaut").isFalse();
        }
        assertThat(Arrays.stream(PaymentReceiptService.class.getMethods()).noneMatch(Method::isDefault)).isTrue();
    }
}
