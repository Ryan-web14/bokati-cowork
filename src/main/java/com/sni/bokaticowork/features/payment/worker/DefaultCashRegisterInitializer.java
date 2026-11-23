package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Caisse par defaut · creee par le systeme au demarrage, avec une session ouverte tenue par le
 * systeme. Elle recoit les paiements saisis sans session de caisse ; sa session est close et
 * renouvelee chaque fin de mois par {@link CashSessionMonthEndWorker}, solde reporte.
 *
 * <p>Sans effet si la caisse et sa session existent deja. Un echec (base indisponible, course entre
 * deux instances sur l'index unique) n'empeche pas le demarrage : la caisse sera creee au premier
 * paiement saisi sans session.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCashRegisterInitializer {

    private final CashRegisterService cashRegisterService;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureDefaultRegister() {
        try {
            var register = cashRegisterService.ensureDefaultRegister();
            log.info("Caisse par defaut prete : {}", register.registerCode());
        } catch (RuntimeException ex) {
            log.warn("Caisse par defaut non preparee au demarrage : {}", ex.getMessage());
        }
    }
}
