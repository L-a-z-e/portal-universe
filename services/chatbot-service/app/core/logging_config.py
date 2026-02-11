import logging
import sys

from pythonjsonlogger.json import JsonFormatter

from app.core.config import settings


class TraceJsonFormatter(JsonFormatter):
    """JSON formatter that injects OTel trace context into log records."""

    def add_fields(
        self,
        log_record: dict,
        record: logging.LogRecord,
        message_dict: dict,
    ) -> None:
        super().add_fields(log_record, record, message_dict)
        log_record["service_name"] = "chatbot-service"

        # Inject OTel trace/span IDs if available
        trace_id = getattr(record, "otelTraceID", "0" * 32)
        span_id = getattr(record, "otelSpanID", "0" * 16)
        log_record["traceId"] = trace_id
        log_record["spanId"] = span_id


def setup_logging() -> None:
    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(
        TraceJsonFormatter(
            fmt="%(timestamp)s %(level)s %(name)s %(message)s",
            rename_fields={"levelname": "level", "asctime": "timestamp"},
        )
    )

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    root_logger.handlers.clear()
    root_logger.addHandler(handler)

    # 외부 라이브러리 로그 레벨 조정
    logging.getLogger("chromadb").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)
