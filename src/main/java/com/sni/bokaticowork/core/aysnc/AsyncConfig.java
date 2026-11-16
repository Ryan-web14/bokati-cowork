package com.sni.bokaticowork.core.aysnc;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
// Planification activee par core.async.SchedulingConfig, sur le seul dyno designe.
public class AsyncConfig implements AsyncConfigurer, SchedulingConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        // Centre d'audit (lot A0) : la tache garde l'identifiant de requete et le compte de son appelant.
        executor.setTaskDecorator(new com.sni.bokaticowork.core.observability.MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // Heroku tue le processus 30 s apres SIGTERM : Tomcat, cet executeur et le planificateur
        // doivent s'arreter ensemble dans ce delai (8 s chacun au plus).
        executor.setAwaitTerminationSeconds(8);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(8);
        // Centre d'audit (lot A4) : chaque execution de tache est comptee, chronometree, et ses echecs gardes.
        scheduler.setTaskDecorator(new com.sni.bokaticowork.core.audit.ops.ScheduledRunDecorator());
        scheduler.setErrorHandler(ex -> {
            com.sni.bokaticowork.core.audit.ops.ScheduledRuns.fail(ex);
            org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                    .error("Scheduler error: {}", ex.getMessage(), ex);
        });
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler());
    }

    // Logs toutes les exceptions non catchées des méthodes @Async void
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                        .error("Async error in {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
