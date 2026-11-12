package com.sni.bokaticowork.core.settings.platform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Declencheurs de planification qui relisent leur parametre a chaque execution : un changement de
 * cron ou de delai prend effet au prochain passage, sans redemarrage.
 *
 * <p>Usage dans un traitement : il implemente {@code SchedulingConfigurer} et enregistre sa tache
 * avec {@code registrar.addTriggerTask(this::run, triggers.fixedDelay(KEY, defaut, delaiInitial))}
 * ou {@code triggers.cron(KEY, defaut, zone)}, a la place de {@code @Scheduled}. Le delai initial
 * n'a de sens qu'au demarrage : il reste lu une fois.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettingTriggers {

    /** Delai minimal entre deux executions, pour qu'une valeur aberrante ne sature pas le planificateur. */
    static final long MIN_DELAY_MS = 1_000;

    private final PlatformSettings settings;

    /**
     * Delai fixe entre la fin d'une execution et le debut de la suivante, relu a chaque fois.
     *
     * @param initialDelayMs delai avant la premiere execution ; negatif : le delai courant
     */
    public Trigger fixedDelay(String key, long fallbackMs, long initialDelayMs) {
        return context -> {
            Instant last = context.lastCompletion();
            long delay = Math.max(MIN_DELAY_MS, settings.number(key, fallbackMs));
            if (last == null) {
                return context.getClock().instant().plusMillis(initialDelayMs >= 0 ? initialDelayMs : delay);
            }
            return last.plusMillis(delay);
        };
    }

    public Trigger fixedDelay(String key, long fallbackMs) {
        return fixedDelay(key, fallbackMs, 0);
    }

    /** Expression cron relue a chaque execution ; une expression invalide garde le defaut, journalise. */
    public Trigger cron(String key, String fallback, ZoneId zone) {
        return context -> {
            String expression = settings.text(key, fallback);
            CronTrigger trigger;
            try {
                trigger = new CronTrigger(expression, zone);
            } catch (IllegalArgumentException ex) {
                log.warn("Planification {} : expression cron invalide « {} », valeur par defaut « {} » appliquee", key, expression, fallback);
                trigger = new CronTrigger(fallback, zone);
            }
            return trigger.nextExecution(context);
        };
    }

    public Trigger cron(String key, String fallback) {
        return cron(key, fallback, ZoneId.systemDefault());
    }
}
