package com.sni.bokaticowork.core.audit.ops;

import org.springframework.core.task.TaskDecorator;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decorateur du planificateur ({@code AsyncConfig.taskScheduler}) : chaque execution d'une tache est
 * comptee et chronometree par {@link ScheduledRuns}. Les echecs arrivent par le gestionnaire
 * d'erreurs du planificateur, qui les intercepte avant ce decorateur.
 *
 * <p>Nom d'une tache : {@code Classe.methode} pour un {@code @Scheduled}, {@code Classe} pour une tache
 * enregistree par un {@code SchedulingConfigurer} (fonction anonyme). Tire de la description que
 * Spring et le JDK donnent de la tache.</p>
 */
public class ScheduledRunDecorator implements TaskDecorator {

    /** Taches internes du centre lui-meme, non suivies. */
    static final Set<String> IGNORED = Set.of("OperationsWriter");

    private static final Pattern LAMBDA = Pattern.compile("([A-Za-z0-9_$.]+?)\\$\\$Lambda");
    private static final Pattern METHOD = Pattern.compile("([a-z][a-z0-9_]*\\.)+([A-Z][A-Za-z0-9_$]*)\\.([a-zA-Z0-9_]+)");

    @Override
    public Runnable decorate(Runnable runnable) {
        String task = nameOf(String.valueOf(runnable));
        if (task == null || IGNORED.contains(task)) {
            return runnable;
        }
        return () -> {
            ScheduledRuns.begin(task);
            try {
                runnable.run();
            } catch (RuntimeException | Error failure) {
                ScheduledRuns.fail(failure);
                throw failure;
            } finally {
                ScheduledRuns.end();
            }
        };
    }

    /** Nom lisible tire de la description de la tache ; {@code null} si elle n'en donne pas. */
    static String nameOf(String description) {
        String text = description;
        int wrapped = text.lastIndexOf("Wrapped task = ");
        if (wrapped >= 0) {
            text = text.substring(wrapped + "Wrapped task = ".length());
        }
        text = text.replace("DelegatingErrorHandlingRunnable for ", "");
        Matcher lambda = LAMBDA.matcher(text);
        if (lambda.find()) {
            String owner = lambda.group(1);
            return owner.substring(owner.lastIndexOf('.') + 1);
        }
        Matcher method = METHOD.matcher(text);
        if (method.find()) {
            return method.group(2) + "." + method.group(3);
        }
        return null;
    }
}
