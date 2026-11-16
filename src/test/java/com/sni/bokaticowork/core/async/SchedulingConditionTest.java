package com.sni.bokaticowork.core.async;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingsRefresher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Les taches planifiees ne tournent que sur le dyno designe ; la relecture des parametres, elle,
 * tourne sur chaque instance.
 */
class SchedulingConditionTest {

    @Test
    void outsideHerokuTheInstanceSchedules() {
        assertThat(SchedulingCondition.enabled(new MockEnvironment())).isTrue();
    }

    @Test
    void onHerokuOnlyTheFirstWebDynoSchedules() {
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "web.1"))).isTrue();
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "web.2"))).isFalse();
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "run.4821"))).isFalse();
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "release.7"))).isFalse();
    }

    @Test
    void theDesignatedDynoCanBeChanged() {
        MockEnvironment environment = new MockEnvironment().withProperty("DYNO", "web.2")
                .withProperty("bokati.scheduling.dyno", "web.2");
        assertThat(SchedulingCondition.enabled(environment)).isTrue();
    }

    @Test
    void anExplicitSettingWins() {
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "web.2")
                .withProperty("bokati.scheduling.enabled", "true"))).isTrue();
        assertThat(SchedulingCondition.enabled(new MockEnvironment().withProperty("DYNO", "web.1")
                .withProperty("bokati.scheduling.enabled", "false"))).isFalse();
        assertThat(SchedulingCondition.enabled(new MockEnvironment()
                .withProperty("bokati.scheduling.enabled", "false"))).isFalse();
    }

    @Test
    void settingsAreReadAtOnceThenEveryMinuteOnEveryInstance() {
        PlatformSettings settings = mock(PlatformSettings.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);

        new PlatformSettingsRefresher(settings, scheduler).afterSingletonsInstantiated();

        verify(settings).refresh();
        verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(Duration.ofMinutes(1)));
    }

    @Test
    void schedulingIsEnabledInOnePlaceOnly() throws IOException {
        List<String> files;
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            files = sources.filter(path -> path.toString().endsWith(".java")).filter(path -> {
                try {
                    return Files.readString(path).lines().anyMatch(line -> line.strip().equals("@EnableScheduling"));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            }).map(path -> path.getFileName().toString()).toList();
        }
        // Un autre @EnableScheduling ferait tourner les taches sur tous les dynos.
        assertThat(files).containsExactly("SchedulingConfig.java");
    }
}
