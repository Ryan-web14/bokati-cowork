package com.sni.bokaticowork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.core.metrics.StartupStep;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Taches planifiees : core.async.SchedulingConfig, sur le seul dyno designe.
@SpringBootApplication
public class BokatiCoworkApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(BokatiCoworkApplication.class);
		// Mesure du demarrage, desactivee par defaut : -Dbokati.startup.profile=true affiche les
		// beans et les etapes les plus lents (temps propre, sans celui des beans qu'ils creent).
		BufferingApplicationStartup startup = Boolean.getBoolean("bokati.startup.profile")
				? new BufferingApplicationStartup(200_000) : null;
		if (startup != null) {
			application.setApplicationStartup(startup);
		}
		application.run(args);
		if (startup != null) {
			printSlowestSteps(startup.getBufferedTimeline().getEvents());
		}
	}

	private static void printSlowestSteps(List<StartupTimeline.TimelineEvent> events) {
		Map<Long, Duration> childTime = new HashMap<>();
		for (StartupTimeline.TimelineEvent event : events) {
			Long parent = event.getStartupStep().getParentId();
			if (parent != null) {
				childTime.merge(parent, event.getDuration(), Duration::plus);
			}
		}
		record Row(String label, long selfMs, long totalMs) {
		}
		List<Row> rows = events.stream().map(event -> {
			StartupStep step = event.getStartupStep();
			StringBuilder label = new StringBuilder(step.getName());
			step.getTags().forEach(tag -> {
				if ("beanName".equals(tag.getKey())) {
					label.append(' ').append(tag.getValue());
				}
			});
			long total = event.getDuration().toMillis();
			long self = total - childTime.getOrDefault(step.getId(), Duration.ZERO).toMillis();
			return new Row(label.toString(), self, total);
		}).sorted(Comparator.comparingLong(Row::selfMs).reversed()).limit(60).toList();
		System.out.println("=== Demarrage : etapes les plus lentes (temps propre / total, ms) ===");
		rows.forEach(row -> System.out.printf("%7d %7d  %s%n", row.selfMs(), row.totalMs(), row.label()));
	}

}
