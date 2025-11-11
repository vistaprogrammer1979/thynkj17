package com.accumed.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manual tracing utility for non-CDI classes with CPU and memory monitoring
 * Provides parent-child context propagation for hierarchical tracing
 */
public class ManualTracing {

    private static final Logger logger = Logger.getLogger(ManualTracing.class.getName());

    // CPU monitoring utilities
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final boolean isThreadCpuTimeSupported;

    static {
        // Initialize thread CPU time monitoring
        isThreadCpuTimeSupported = threadMXBean.isThreadCpuTimeSupported();
        if (isThreadCpuTimeSupported) {
            threadMXBean.setThreadCpuTimeEnabled(true);
            logger.info("ManualTracing: Thread CPU time measurement ENABLED");
        } else {
            logger.warning("ManualTracing: Thread CPU time measurement NOT SUPPORTED");
        }
    }

    // Private constructor - static utility class
    private ManualTracing() {}

    /**
     * Start a new span with parent context (creates child span in existing trace)
     */
    public static TracingSession startSpan(String spanName, Context parentContext) {
        try {
            Tracer tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("manual-tracing");

            Span span = tracer.spanBuilder(spanName)
                    .setParent(parentContext)
                    .startSpan();

            String uniqueId = generateUniqueId();
            span.setAttribute("span.unique.id", uniqueId);
            span.setAttribute("span.start.timestamp", System.currentTimeMillis());
            span.setAttribute("span.type", "child");

            logger.fine(() -> String.format("Started child span: %s, ID: %s", spanName, uniqueId));

            return new TracingSession(span, uniqueId);

        } catch (Exception e) {
            logger.warning("Failed to start span: " + spanName + " - " + e.getMessage());
            // Return a no-op session if tracing fails
            return new TracingSession(null, "no-op");
        }
    }

    /**
     * Start a root span (creates new trace)
     */
    public static TracingSession startRootSpan(String spanName) {
        try {
            Tracer tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("manual-tracing");

            Span span = tracer.spanBuilder(spanName).startSpan();

            String uniqueId = generateUniqueId();
            span.setAttribute("span.unique.id", uniqueId);
            span.setAttribute("span.start.timestamp", System.currentTimeMillis());
            span.setAttribute("span.type", "root");
            span.setAttribute("trace.root", true);

            logger.fine(() -> String.format("Started root span: %s, ID: %s", spanName, uniqueId));

            return new TracingSession(span, uniqueId);

        } catch (Exception e) {
            logger.warning("Failed to start root span: " + spanName + " - " + e.getMessage());
            // Return a no-op session if tracing fails
            return new TracingSession(null, "no-op");
        }
    }

    /**
     * Get current context for propagation to child spans
     */
    public static Context getCurrentContext() {
        return Context.current();
    }

    /**
     * Check if OpenTelemetry is available and initialized
     */
    public static boolean isTracingAvailable() {
        try {
            return OpenTelemetryConfig.getOpenTelemetry() != null &&
                    OpenTelemetryConfig.isInitialized();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a unique identifier for spans
     */
    private static String generateUniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get current thread CPU time in nanoseconds
     */
    private static long getCurrentThreadCPUTime() {
        if (isThreadCpuTimeSupported) {
            try {
                return threadMXBean.getCurrentThreadCpuTime();
            } catch (Exception e) {
                logger.fine("Failed to get thread CPU time: " + e.getMessage());
            }
        }
        return -1;
    }

    /**
     * Get system load average
     */
    private static double getSystemLoadAverage() {
        try {
            return osBean.getSystemLoadAverage();
        } catch (Exception e) {
            logger.fine("Failed to get system load average: " + e.getMessage());
            return -1.0;
        }
    }

    /**
     * Get current memory usage in bytes
     */
    private static long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Get number of available processors
     */
    private static int getAvailableProcessors() {
        return osBean.getAvailableProcessors();
    }

    /**
     * Round to two decimal places for metrics
     */
    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ====================================================================
    // TRACING SESSION INNER CLASS
    // ====================================================================

    /**
     * Represents a tracing session that automatically records metrics when closed
     * Implements AutoCloseable for use with try-with-resources
     */
    public static class TracingSession implements AutoCloseable {
        private final Span span;
        private final long startTime;
        private final long startCpuTime;
        private final double startSystemLoad;
        private final long startMemory;
        private final Scope scope;
        private final String uniqueId;
        private boolean success = true;
        private Throwable error;
        private boolean closed = false;

        /**
         * Constructor - captures all initial metrics
         */
        public TracingSession(Span span, String uniqueId) {
            this.span = span;
            this.uniqueId = uniqueId;
            this.startTime = System.nanoTime();
            this.startCpuTime = getCurrentThreadCPUTime();
            this.startSystemLoad = getSystemLoadAverage();
            this.startMemory = getCurrentMemoryUsage();

            // Only create scope if we have a real span (not no-op)
            this.scope = (span != null) ? span.makeCurrent() : null;
        }

        // ====================================================================
        // GETTER METHODS
        // ====================================================================

        public String getUniqueId() {
            return uniqueId;
        }

        public String getTraceId() {
            if (span != null) {
                return span.getSpanContext().getTraceId();
            }
            return "no-trace";
        }

        public String getSpanId() {
            if (span != null) {
                return span.getSpanContext().getSpanId();
            }
            return "no-span";
        }

        public boolean isNoOp() {
            return span == null;
        }

        // ====================================================================
        // ATTRIBUTE SETTER METHODS
        // ====================================================================

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setError(Throwable error) {
            this.error = error;
            this.success = false;
        }

        public void addAttribute(String key, String value) {
            if (span != null && value != null) {
                span.setAttribute(key, value);
            }
        }

        public void addAttribute(String key, long value) {
            if (span != null) {
                span.setAttribute(key, value);
            }
        }

        public void addAttribute(String key, double value) {
            if (span != null) {
                span.setAttribute(key, value);
            }
        }

        public void addAttribute(String key, boolean value) {
            if (span != null) {
                span.setAttribute(key, value);
            }
        }

        /**
         * Add multiple attributes at once
         */
        public void addAttributes(java.util.Map<String, Object> attributes) {
            if (span != null && attributes != null) {
                attributes.forEach((key, value) -> {
                    if (value instanceof String) {
                        span.setAttribute(key, (String) value);
                    } else if (value instanceof Long) {
                        span.setAttribute(key, (Long) value);
                    } else if (value instanceof Integer) {
                        span.setAttribute(key, (Integer) value);
                    } else if (value instanceof Double) {
                        span.setAttribute(key, (Double) value);
                    } else if (value instanceof Boolean) {
                        span.setAttribute(key, (Boolean) value);
                    } else if (value != null) {
                        span.setAttribute(key, value.toString());
                    }
                });
            }
        }

        // ====================================================================
        // METRIC RECORDING AND CLEANUP
        // ====================================================================

        /**
         * Record all metrics and end the span
         */
        @Override
        public void close() {
            if (closed) {
                return; // Already closed
            }
            closed = true;

            try {
                if (span != null) {
                    recordMetrics();
                }
            } catch (Exception e) {
                logger.warning("Error recording tracing metrics: " + e.getMessage());
            } finally {
                // Always close the scope and end the span
                if (scope != null) {
                    scope.close();
                }
                if (span != null) {
                    span.end();
                    logger.fine(() -> String.format("Ended span: %s, ID: %s",
                            getSpanName(span), uniqueId));
                }
            }
        }

        /**
         * Record performance metrics to the span
         */
        private void recordMetrics() {
            long endTime = System.nanoTime();
            long endCpuTime = getCurrentThreadCPUTime();
            double endSystemLoad = getSystemLoadAverage();
            long endMemory = getCurrentMemoryUsage();

            long durationNs = endTime - startTime;
            long durationMs = durationNs / 1_000_000;

            // ====================================================================
            // BASIC TIMING METRICS
            // ====================================================================
            span.setAttribute("duration.ns", durationNs);
            span.setAttribute("duration.ms", durationMs);
            span.setAttribute("success", success);
            span.setAttribute("thread.name", Thread.currentThread().getName());
            span.setAttribute("thread.id", Thread.currentThread().getId());

            // ====================================================================
            // CPU METRICS
            // ====================================================================
            recordCPUMetrics(durationNs, startCpuTime, endCpuTime, startSystemLoad, endSystemLoad);

            // ====================================================================
            // MEMORY METRICS
            // ====================================================================
            recordMemoryMetrics(startMemory, endMemory);

            // ====================================================================
            // ERROR HANDLING
            // ====================================================================
            if (error != null) {
                span.setAttribute("error.type", error.getClass().getSimpleName());
                span.setAttribute("error.message", error.getMessage());
                span.recordException(error);
            }

            // Log performance summary
            logger.fine(() -> String.format(
                    "Span completed: %s | Duration: %dms | CPU: %.1f%% | Memory: %dMB | Success: %s",
                    getSpanName(span), durationMs,
                    calculateCPUUsage(durationNs, startCpuTime, endCpuTime),
                    (endMemory - startMemory) / (1024 * 1024),
                    success
            ));
        }

        /**
         * Record CPU-related metrics
         */
        private void recordCPUMetrics(long durationNs, long startCpuTime, long endCpuTime,
                                      double startSystemLoad, double endSystemLoad) {
            // Thread CPU Time (most accurate for method-level CPU usage)
            if (isThreadCpuTimeSupported && startCpuTime != -1 && endCpuTime != -1 && durationNs > 0) {
                long threadCpuTimeNs = endCpuTime - startCpuTime;
                double threadCpuUsagePercent = (double) threadCpuTimeNs / durationNs * 100.0;

                span.setAttribute("cpu.thread.time.ns", threadCpuTimeNs);
                span.setAttribute("cpu.thread.time.ms", threadCpuTimeNs / 1_000_000);
                span.setAttribute("cpu.thread.usage.percent", roundToTwoDecimals(threadCpuUsagePercent));
            }

            // System CPU Load
            if (startSystemLoad >= 0 && endSystemLoad >= 0) {
                double avgSystemLoad = (startSystemLoad + endSystemLoad) / 2.0;
                span.setAttribute("cpu.system.load.avg", roundToTwoDecimals(avgSystemLoad));

                // Convert to percentage based on available processors
                int processors = getAvailableProcessors();
                double systemLoadPercentage = (avgSystemLoad / processors) * 100.0;
                span.setAttribute("cpu.system.load.percent", roundToTwoDecimals(systemLoadPercentage));
            }

            span.setAttribute("system.processors", getAvailableProcessors());
        }

        /**
         * Record memory-related metrics
         */
        private void recordMemoryMetrics(long startMemory, long endMemory) {
            Runtime runtime = Runtime.getRuntime();
            long memoryUsed = endMemory - startMemory;

            span.setAttribute("memory.consumption.bytes", memoryUsed);
            span.setAttribute("memory.consumption.mb", memoryUsed / (1024 * 1024));
            span.setAttribute("memory.heap.used.bytes", endMemory);
            span.setAttribute("memory.heap.max.bytes", runtime.maxMemory());
            span.setAttribute("memory.heap.committed.bytes", runtime.totalMemory());
            span.setAttribute("memory.heap.free.bytes", runtime.freeMemory());
            span.setAttribute("memory.measurement.type", "real.world");
        }

        /**
         * Calculate CPU usage percentage
         */
        private double calculateCPUUsage(long durationNs, long startCpuTime, long endCpuTime) {
            if (isThreadCpuTimeSupported && startCpuTime != -1 && endCpuTime != -1 && durationNs > 0) {
                long threadCpuTimeNs = endCpuTime - startCpuTime;
                return (double) threadCpuTimeNs / durationNs * 100.0;
            }
            return -1.0;
        }

        /**
         * Extract span name for logging
         */
        private String getSpanName(Span span) {
            // This is a simplified approach - in real implementation you might want to store the name
            return "ManualSpan";
        }

        /**
         * Finalizer as safety net - ensures resources are cleaned up
         */
        @Override
        protected void finalize() throws Throwable {
            if (!closed) {
                logger.warning("TracingSession not properly closed: " + uniqueId);
                close();
            }
            super.finalize();
        }
    }
}