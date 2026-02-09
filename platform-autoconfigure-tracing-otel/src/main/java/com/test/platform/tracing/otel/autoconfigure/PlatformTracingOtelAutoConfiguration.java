package com.test.platform.tracing.otel.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(PlatformTracingOtelProperties.class)
@ConditionalOnClass({OpenTelemetrySdk.class, OtelTracer.class, ObservationRegistry.class})
@ConditionalOnProperty(prefix = "platform.tracing.otel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformTracingOtelAutoConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean(OpenTelemetrySdk.class)
	public OpenTelemetrySdk platformOpenTelemetrySdk(Environment environment,
			PlatformTracingOtelProperties properties) {
		String serviceName = StringUtils.hasText(properties.getServiceName())
				? properties.getServiceName()
				: environment.getProperty("spring.application.name", "app");

		Resource resource = Resource.getDefault()
				.merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));

		SdkTracerProviderBuilder tracerProvider = SdkTracerProvider.builder().setResource(resource);

		PlatformTracingOtelProperties.Export export = properties.getExport();
		if (export.isEnabled()) {
			String endpoint = export.getOtlpEndpoint();
			if (!StringUtils.hasText(endpoint)) {
				endpoint = environment.getProperty("OTEL_EXPORTER_OTLP_ENDPOINT");
			}
			if (StringUtils.hasText(endpoint)) {
				OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint)
						.setTimeout(export.getTimeout()).build();
				tracerProvider.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
			}
		}

		ContextPropagators propagators = ContextPropagators
				.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance()));

		return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider.build()).setPropagators(propagators).build();
	}

	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
	public OpenTelemetry platformOpenTelemetry(OpenTelemetrySdk openTelemetrySdk) {
		return openTelemetrySdk;
	}

	@Bean
	@ConditionalOnMissingBean(TracerProvider.class)
	public TracerProvider platformOtelTracerProvider(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracerProvider();
	}

	@Bean
	@ConditionalOnMissingBean(name = "platformOtelApiTracer")
	public io.opentelemetry.api.trace.Tracer platformOtelApiTracer(OpenTelemetry openTelemetry,
			PlatformTracingOtelProperties properties) {
		return openTelemetry.getTracer(properties.getInstrumentationName());
	}

	@Bean
	@ConditionalOnMissingBean
	public OtelCurrentTraceContext platformOtelCurrentTraceContext() {
		return new OtelCurrentTraceContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public BaggageManager platformBaggageManager(OtelCurrentTraceContext currentTraceContext) {
		return new OtelBaggageManager(currentTraceContext, List.of(), List.of());
	}

	@Bean
	@ConditionalOnMissingBean
	public EventListener platformSlf4jTraceSpanMdcListener() {
		// Standardize MDC keys for correlation (works with JSON encoder capturing MDC).
		return new Slf4JEventListener("traceId", "spanId");
	}

	@Bean
	@ConditionalOnMissingBean
	public OtelTracer.EventPublisher platformOtelEventPublisher(List<EventListener> listeners) {
		List<EventListener> safeListeners = List.copyOf(listeners);
		return event -> safeListeners.forEach(listener -> listener.onEvent(event));
	}

	@Bean
	@ConditionalOnMissingBean(Tracer.class)
	public Tracer platformMicrometerTracer(io.opentelemetry.api.trace.Tracer otelApiTracer,
			OtelCurrentTraceContext currentTraceContext, OtelTracer.EventPublisher eventPublisher,
			BaggageManager baggageManager) {
		return new OtelTracer(otelApiTracer, currentTraceContext, eventPublisher, baggageManager);
	}

	@Bean
	@ConditionalOnMissingBean(Propagator.class)
	public Propagator platformMicrometerPropagator(OpenTelemetry openTelemetry,
			io.opentelemetry.api.trace.Tracer otelApiTracer) {
		return new OtelPropagator(openTelemetry.getPropagators(), otelApiTracer);
	}

	@Bean
	@ConditionalOnMissingBean(name = "platformTracingObservationRegistryCustomizer")
	public ObservationRegistryCustomizer<ObservationRegistry> platformTracingObservationRegistryCustomizer(
			Tracer tracer, Propagator propagator) {
		return registry -> registry.observationConfig()
				.observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator))
				.observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator))
				.observationHandler(new DefaultTracingObservationHandler(tracer));
	}
}
