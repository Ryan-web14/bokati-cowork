package com.sni.bokaticowork.core.async;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Taches planifiees ({@code @Scheduled} et {@code SchedulingConfigurer}) sur une seule instance.
 *
 * <p>Avec plusieurs dynos, chaque tache tournerait sur chacun : relances, ecritures comptables et
 * courriels en double. Elles ne tournent que sur le dyno designe, voir {@link SchedulingCondition}.
 * Les autres instances servent les requetes et traitent la boite de sortie (reclamation par verrou),
 * mais ne planifient rien. La relecture des parametres en memoire, propre a chaque instance, ne
 * passe pas par ici ({@code PlatformSettingsRefresher}).</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@Conditional(SchedulingCondition.class)
public class SchedulingConfig {
}
