from datetime import datetime, timezone
from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class FieldError(BaseModel):
    """Validation failure detail for a single field."""

    field: str
    message: str
    rejected_value: Any | None = Field(default=None, serialization_alias="rejectedValue")


class ErrorDetail(BaseModel):
    """Standardized error object matching docs/contracts/error-response.schema.json."""

    code: str
    message: str
    timestamp: str | None = None
    path: str | None = None
    details: list[FieldError] | None = None


class ApiResponse(BaseModel, Generic[T]):
    """기존 Java 서비스와 호환되는 통일 응답 형식."""

    success: bool
    data: T | None = None
    error: ErrorDetail | None = None

    @classmethod
    def ok(cls, data: Any = None) -> "ApiResponse":
        return cls(success=True, data=data)

    @classmethod
    def fail(
        cls,
        code: str,
        message: str,
        *,
        path: str | None = None,
        details: list[FieldError] | None = None,
    ) -> "ApiResponse":
        return cls(
            success=False,
            error=ErrorDetail(
                code=code,
                message=message,
                timestamp=datetime.now(timezone.utc).isoformat(),
                path=path,
                details=details if details else None,
            ),
        )
