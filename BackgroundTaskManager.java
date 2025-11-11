/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import com.accumed.exceptions.ExceptionBatchProcessor;
import com.accumed.exceptions.ExceptionLogJPA;
import com.accumed.re.agents.CachedRepositoryService;
import com.accumed.re.agents.WorkLogger;
import com.accumed.tracing.ManualTracing;
import com.accumed.tracing.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author smutlak
 */
public class BackgroundTaskManager implements ServletContextListener {

    private static final int MAXIMUM_CURRENT = 3;
    private static int INITIAL_DELAY = 10;
    private static int REPO_CHECK_INTERVAL = 30;
    static {

        try {
            INITIAL_DELAY = Integer.parseInt((String) (new InitialContext().lookup("java:comp/env/com.accumed.rules.engine.initialDelay")));
            REPO_CHECK_INTERVAL = Integer.parseInt((String) (new InitialContext().lookup("java:comp/env/com.accumed.rules.engine.repository.check.interval")));
        } catch (Exception ex) {
            Logger.getLogger(BackgroundTaskManager.class.getName()).log(Level.SEVERE,
                    "exception caught", ex);
        }
        Logger.getLogger(BackgroundTaskManager.class
                .getName()).log(Level.INFO, "initialDelay={0}, check.interval={1}",
                        new Object[]{INITIAL_DELAY, REPO_CHECK_INTERVAL});
        Logger.getLogger(BackgroundTaskManager.class.getName()).info("BackgroundTaskManager static initializer - forcing OpenTelemetry init");
        OpenTelemetryConfig.getOpenTelemetry();
    }

    private static ScheduledThreadPoolExecutor executor = null;
    private static ScheduledFuture cachedRepositoryFuture;
    private static ScheduledFuture droolsUpdaterFuture;
    private static ScheduledFuture workLoggerFuture;
    private static ScheduledFuture ResolvedRateJobFuture;

    synchronized public static void restartAgents() {
        // Add detailed tracing for restartAgents
        try (com.accumed.tracing.ManualTracing.TracingSession tracing =
                     com.accumed.tracing.ManualTracing.startRootSpan("BackgroundTaskManager.restartAgents")) {

            tracing.addAttribute("background.task", true);
            tracing.addAttribute("thread.name", Thread.currentThread().getName());
            tracing.addAttribute("maximum.concurrent.tasks", MAXIMUM_CURRENT);
            tracing.addAttribute("initial.delay", INITIAL_DELAY);
            tracing.addAttribute("repo.check.interval", REPO_CHECK_INTERVAL);

            logger.info("Restarting background agents...");

            long startTime = System.nanoTime();

            if (executor != null) {
                logger.info("Shutting down existing executor...");
                executor.shutdownNow();
                while (!executor.isTerminated()) {
                    logger.info("Waiting for agents to shutdown...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                executor = null;
                logger.info("Existing executor shut down");
            }

            executor = new ScheduledThreadPoolExecutor(MAXIMUM_CURRENT);
            tracing.addAttribute("executor.created", true);
            tracing.addAttribute("executor.pool.size", MAXIMUM_CURRENT);

            // Start CachedRepositoryService with tracing
            logger.info("Scheduling CachedRepositoryService...");
            cachedRepositoryFuture = executor.scheduleWithFixedDelay(new CachedRepositoryService(),
                    INITIAL_DELAY, REPO_CHECK_INTERVAL, TimeUnit.SECONDS);
            tracing.addAttribute("cached.repository.scheduled", true);

            // Start WorkLogger with tracing
            logger.info("Scheduling WorkLogger...");
            WorkLogger workLogger = new WorkLogger();
            workLoggerFuture = executor.scheduleWithFixedDelay(workLogger,
                    INITIAL_DELAY, 30, TimeUnit.SECONDS);
            tracing.addAttribute("work.logger.scheduled", true);

            AccumedValidatorWS.setWorkLogger(workLogger);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            tracing.addAttribute("restart.duration.ms", durationMs);

            logger.info("Background agents restarted successfully in " + durationMs + "ms");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to restart background agents", e);
            throw e;
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("BackgroundTaskManager contextDestroyed - shutting down");

        try (com.accumed.tracing.ManualTracing.TracingSession tracing =
                     com.accumed.tracing.ManualTracing.startRootSpan("BackgroundTaskManager.contextDestroyed")) {

            tracing.addAttribute("servlet.context", "destroyed");
            tracing.addAttribute("background.task", true);

            if (executor != null) {
                executor.shutdown();
                tracing.addAttribute("executor.shutdown", true);
            }

            if (AccumedValidatorWS.saveFixedPool != null) {
                AccumedValidatorWS.saveFixedPool.shutdown();
                tracing.addAttribute("save.pool.shutdown", true);
            }

            if (AccumedValidatorWS.returnFixedPool != null) {
                AccumedValidatorWS.returnFixedPool.shutdown();
                tracing.addAttribute("return.pool.shutdown", true);
            }
            ExceptionBatchProcessor.shutdown();
            ExceptionLogJPA.close();
            Logger.getLogger(BackgroundTaskManager.class.getName())
                    .info("Exception Logging Service Stopped");

            logger.info("Background tasks shut down successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during context destruction", e);
        }

        logger.info("BackgroundTaskManager contextDestroyed completed");
    }
    private static final Logger logger = Logger.getLogger(BackgroundTaskManager.class.getName());
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("BackgroundTaskManager contextInitialized - starting agents");

        // Use manual tracing for the initial startup
        try (com.accumed.tracing.ManualTracing.TracingSession tracing =
                     com.accumed.tracing.ManualTracing.startRootSpan("BackgroundTaskManager.contextInitialized")) {

            tracing.addAttribute("servlet.context", "initialized");
            tracing.addAttribute("background.task", true);

            restartAgents();

            logger.info("Background agents started successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start background agents", e);
        }

        logger.info("BackgroundTaskManager contextInitialized completed");
    }

    public static boolean isRunningCachedRepositoryFuture() {
        return (cachedRepositoryFuture == null || !(cachedRepositoryFuture instanceof ScheduledFuture)) ? false
                : cachedRepositoryFuture.getDelay(TimeUnit.MILLISECONDS) > 0;
    }

    public static boolean isRunningDroolsUpdaterFuture() {
        return (droolsUpdaterFuture == null || !(droolsUpdaterFuture instanceof ScheduledFuture)) ? false
                : droolsUpdaterFuture.getDelay(TimeUnit.MILLISECONDS) > 0;
    }

    public static boolean isRunningWorkLoggerFuture() {
        return (workLoggerFuture == null || !(workLoggerFuture instanceof ScheduledFuture)) ? false
                : workLoggerFuture.getDelay(TimeUnit.MILLISECONDS) > 0;
    }

}
