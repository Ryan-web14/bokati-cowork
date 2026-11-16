package com.sni.bokaticowork.core.audit.ops;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A4 : chaque execution d'une tache planifiee est comptee sous un nom lisible, ses echecs
 * remontent par le gestionnaire d'erreurs du planificateur, avec le vrai planificateur de Spring.
 */
class ScheduledRunsA4Test {

    /** Tache enregistree comme le font les {@code SchedulingConfigurer} : une fonction anonyme. */
    static final class FakeWorker {
        Runnable task(CountDownLatch latch, boolean fail) {
            return () -> {
                latch.countDown();
                if (fail) {
                    throw new IllegalStateException("operateur injoignable");
                }
            };
        }
    }

    static final class OtherWorker {
        Runnable task(CountDownLatch latch) {
            return latch::countDown;
        }
    }

    @Test
    void namesComeFromTheTaskDescription() {
        assertThat(ScheduledRunDecorator.nameOf("java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask@1a2b"
                + "[Not completed, task = java.util.concurrent.Executors$RunnableAdapter@3c4d[Wrapped task = "
                + "DelegatingErrorHandlingRunnable for com.sni.bokaticowork.features.support.worker.SupportSlaEscalationWorker"
                + "$$Lambda/0x0000012345@5e6f]]")).isEqualTo("SupportSlaEscalationWorker");
        assertThat(ScheduledRunDecorator.nameOf("DelegatingErrorHandlingRunnable for "
                + "com.sni.bokaticowork.features.document.kyc.worker.KycAutomationWorker.remindIncompleteKycCases"))
                .isEqualTo("KycAutomationWorker.remindIncompleteKycCases");
        assertThat(ScheduledRunDecorator.nameOf("java.lang.Object@12")).isNull();
    }

    @Test
    void runsAndFailuresAreCountedThroughTheRealScheduler() throws Exception {
        ScheduledRuns.drain();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setTaskDecorator(new ScheduledRunDecorator());
        scheduler.setErrorHandler(ScheduledRuns::fail);
        scheduler.initialize();
        try {
            CountDownLatch failing = new CountDownLatch(3);
            CountDownLatch working = new CountDownLatch(2);
            scheduler.schedule(new FakeWorker().task(failing, true), new PeriodicTrigger(Duration.ofMillis(30)));
            scheduler.scheduleWithFixedDelay(new OtherWorker().task(working), Duration.ofMillis(30));
            assertThat(failing.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(working.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);
        } finally {
            scheduler.shutdown();
        }

        ScheduledRuns.Drained drained = ScheduledRuns.drain();
        ScheduledRuns.TaskTotals totals = drained.totals().stream()
                .filter(t -> t.task().equals("ScheduledRunsA4Test$FakeWorker")).findFirst().orElseThrow();
        assertThat(totals.runs()).isGreaterThanOrEqualTo(5);
        assertThat(totals.failures()).isGreaterThanOrEqualTo(3);
        List<ScheduledRuns.Run> failures = drained.runs().stream().filter(r -> "FAILURE".equals(r.reason())).toList();
        assertThat(failures).isNotEmpty();
        assertThat(failures.getFirst().error()).isEqualTo("IllegalStateException: operateur injoignable");
        // Premiere execution d'une tache : gardee (tache rare jusqu'a preuve du contraire), les suivantes non.
        assertThat(drained.runs()).filteredOn(r -> r.task().equals("ScheduledRunsA4Test$OtherWorker"))
                .singleElement().extracting(ScheduledRuns.Run::reason).isEqualTo("PERIODIC");
        assertThat(ScheduledRuns.drain().totals()).isEmpty();
    }
}
