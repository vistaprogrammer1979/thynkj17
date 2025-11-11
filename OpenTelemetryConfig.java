package com.accumed.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;



public class OpenTelemetryConfig {

    private static final Logger logger = Logger.getLogger(OpenTelemetryConfig.class.getName());
    private static OpenTelemetry openTelemetry;
    private static boolean autoInstrumentationEnabled = false;


    public static boolean isInitialized() {
        return openTelemetry != null && openTelemetry != OpenTelemetry.noop();
    }
    public static String getInitializationStatus() {
        if (openTelemetry == null) {
            return "NOT_INITIALIZED";
        } else if (openTelemetry == OpenTelemetry.noop()) {
            return "NOOP_INSTANCE";
        } else {
            return "INITIALIZED";
        }
    }

    // Add debug logging
    private static void debugLog(String message) {
        logger.info("[OpenTelemetryConfig] " + message);
    }
    static {
        debugLog("Static initializer started");
        initializeOpenTelemetry();
        debugLog("Static initializer completed. Status: " + getInitializationStatus());
    }

    private static void initializeOpenTelemetry() {
        try {
            String instrumentationMode = System.getProperty("otel.auto.instrument", "manual");

            if ("auto".equalsIgnoreCase(instrumentationMode)) {
                initializeWithAutoInstrumentation();
            } else {
                initializeManual();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize OpenTelemetry", e);
            openTelemetry = OpenTelemetry.noop();
        }
    }


    private static void initializeManual() {
        logger.info("Initializing OpenTelemetry manually with OTLP HTTP exporter...");

        // OTLP HTTP Span Exporter
        OtlpHttpSpanExporter httpExporter = OtlpHttpSpanExporter.builder()
              .setEndpoint("http://127.0.0.1:4318/v1/traces") // OTLP HTTP endpoint
                // .setEndpoint("http://localhost:4318") // OTLP HTTP endpoint
                // .ظ.addHeader("Content-Type", "application/json")                // Add any required headers (e.g., authentication)
                // .ظ.addHeader("Content-Type", "application/json")                // Add any required headers (e.g., authentication)
                // .addHeader("Authorization", "Bearer your-token")
              // .setEndpoint("http://localhost:4318")
                .setTimeout(30, TimeUnit.SECONDS)
                .build();

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put("service.name", getServiceName())
                        .put("service.version", getServiceVersion())
                        .put("deployment.environment", getEnvironment())
                        .put("telemetry.sdk.language", "java")
                        .build()));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(
                        BatchSpanProcessor.builder(httpExporter)
                                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                                .setMaxExportBatchSize(512)
                                .setMaxQueueSize(2048)
                                .build()
                )
                .setResource(resource)
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        logger.info("Manual OpenTelemetry initialization completed successfully");
    }


    private static void initializeWithAutoInstrumentation() {
        logger.info("Initializing OpenTelemetry with auto-instrumentation...");

        try {
            AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();

            System.setProperty("otel.service.name", getServiceName());
            System.setProperty("otel.resource.attributes",
                    String.format("service.version=%s,deployment.environment=%s",
                            getServiceVersion(), getEnvironment()));

            System.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
            System.setProperty("otel.exporter.otlp.endpoint", "http://127.0.0.1:4318/v1/traces");
          // System.setProperty("otel.exporter.otlp.endpoint", "http://localhost:4318");
            System.setProperty("otel.exporter.otlp.headers", "Content-Type=application/x-protobuf");

            System.setProperty("otel.instrumentation.common.default-enabled", "true");
            System.setProperty("otel.instrumentation.http.capture-headers", ".*");

            OpenTelemetrySdk autoConfiguredSdk = sdkBuilder.build().getOpenTelemetrySdk();
            openTelemetry = autoConfiguredSdk;
            autoInstrumentationEnabled = true;

            logger.info("Auto-instrumentation OpenTelemetry initialization completed successfully");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Auto-instrumentation failed, falling back to manual", e);
            initializeManual();
        }
    }

    private static String getServiceName() {
        return System.getProperty("otel.service.name", "RulesEngineStaging");
    }

    private static String getServiceVersion() {
        return System.getProperty("otel.service.version", "3.0.10.50");
    }

    private static String getEnvironment() {
        return System.getProperty("otel.environment", "production");
    }

    public static OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    public static boolean isAutoInstrumentationEnabled() {
        return autoInstrumentationEnabled;
    }


    public static void reinitialize() {
        initializeOpenTelemetry();
    }


    public static void setInstrumentationMode(boolean useAutoInstrumentation) {
        System.setProperty("otel.auto.instrument", useAutoInstrumentation ? "auto" : "manual");
        reinitialize();
    }

    public Tracer getTracer(String tracer) {
        return this.openTelemetry.getTracer(tracer);
    }
    public static Span createChildSpan(String spanName, Context parentContext) {
        return getOpenTelemetry().getTracer("validation-chain")
                .spanBuilder(spanName)
                .setParent(parentContext)
                .startSpan();
    }
    public static Context getCurrentContext() {
        return Context.current();
    }


    public static io.opentelemetry.api.trace.SpanBuilder spanBuilder(String spanName) {
        return getOpenTelemetry().getTracer("validation-chain")
                .spanBuilder(spanName)
                .setParent(getCurrentContext());
    }
}