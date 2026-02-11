import logging

from opentelemetry import trace
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

from app.core.config import settings

logger = logging.getLogger(__name__)

_tracer_provider: TracerProvider | None = None


def setup_telemetry() -> None:
    global _tracer_provider
    resource = Resource.create({"service.name": "chatbot-service"})
    provider = TracerProvider(resource=resource)

    if settings.tracing_enabled:
        try:
            from opentelemetry.exporter.zipkin.json import ZipkinExporter

            exporter = ZipkinExporter(endpoint=settings.zipkin_endpoint)
            provider.add_span_processor(BatchSpanProcessor(exporter))
            logger.info("Zipkin tracing enabled: %s", settings.zipkin_endpoint)
        except Exception:
            logger.warning("Failed to initialize Zipkin exporter, tracing disabled")
    else:
        logger.info("Tracing disabled (TRACING_ENABLED=false)")

    trace.set_tracer_provider(provider)
    _tracer_provider = provider


def shutdown_telemetry() -> None:
    global _tracer_provider
    if _tracer_provider is not None:
        _tracer_provider.force_flush(timeout_millis=5000)
        _tracer_provider.shutdown()
        _tracer_provider = None
