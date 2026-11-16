package com.sni.bokaticowork.core.settings.platform;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Relecture des parametres de la plateforme sur chaque instance.
 *
 * <p>Les valeurs modifiees sont gardees en memoire par {@link PlatformSettings} : chaque dyno doit
 * les relire, y compris ceux qui n'executent pas les taches planifiees. Cette relecture ne passe donc
 * pas par {@code @Scheduled}, reserve au dyno designe : elle est lancee ici, une premiere fois des
 * que les beans sont crees, puis chaque minute.</p>
 */
@Component
public class PlatformSettingsRefresher implements SmartInitializingSingleton {

    static final Duration PERIOD = Duration.ofMinutes(1);

    private final PlatformSettings settings;
    private final TaskScheduler scheduler;

    public PlatformSettingsRefresher(PlatformSettings settings, @Qualifier("taskScheduler") TaskScheduler scheduler) {
        this.settings = settings;
        this.scheduler = scheduler;
    }

    @Override
    public void afterSingletonsInstantiated() {
        settings.refresh();
        scheduler.scheduleWithFixedDelay(settings::refresh, Instant.now().plus(PERIOD), PERIOD);
    }
}
