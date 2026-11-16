package com.sni.bokaticowork.core.async;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Decide si cette instance execute les taches planifiees.
 *
 * <ul>
 *   <li>{@code bokati.scheduling.enabled} ({@code BOKATI_SCHEDULING_ENABLED}) a {@code true} ou
 *   {@code false} : force le choix ;</li>
 *   <li>sinon ({@code auto}, par defaut) : sur Heroku (variable {@code DYNO} presente), seul le dyno
 *   {@code bokati.scheduling.dyno} ({@code BOKATI_SCHEDULING_DYNO}, {@code web.1} par defaut)
 *   planifie ; les dynos ponctuels ({@code run.*}, {@code release.*}) et les autres dynos web non ;
 *   hors Heroku (poste de dev, tests), l'instance planifie.</li>
 * </ul>
 */
public class SchedulingCondition implements Condition {

    static final String ENABLED = "bokati.scheduling.enabled";
    static final String DYNO = "bokati.scheduling.dyno";
    static final String DEFAULT_DYNO = "web.1";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return enabled(context.getEnvironment());
    }

    static boolean enabled(Environment environment) {
        String setting = environment.getProperty(ENABLED, "auto").trim().toLowerCase(Locale.ROOT);
        if ("true".equals(setting)) {
            return true;
        }
        if ("false".equals(setting)) {
            return false;
        }
        String dyno = environment.getProperty("DYNO");
        if (!StringUtils.hasText(dyno)) {
            return true;
        }
        return dyno.trim().equals(environment.getProperty(DYNO, DEFAULT_DYNO).trim());
    }
}
