"""Global exception handlers for consistent ApiResponse error format.

Ensures all error responses follow docs/contracts/error-response.schema.json,
regardless of whether the error originates from HTTPException, Pydantic
validation, or unhandled exceptions.
"""

import logging
from http import HTTPStatus

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.schemas.common import ApiResponse, FieldError

logger = logging.getLogger(__name__)

# Common error codes matching docs/contracts/error-codes.md
_STATUS_TO_CODE: dict[int, tuple[str, str]] = {
    400: ("C002", "Invalid Input Value"),
    401: ("C005", "Unauthorized"),
    403: ("C004", "Forbidden"),
    404: ("C003", "Not Found"),
    500: ("C001", "Internal Server Error"),
}


class BusinessException(Exception):
    """Domain-specific exception with error code.

    Usage:
        raise BusinessException("CH01", "RAG Engine Error", status_code=500)
        raise BusinessException("CH02", "Invalid Document", status_code=400)
    """

    def __init__(self, code: str, message: str, *, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


async def business_exception_handler(
    request: Request, exc: BusinessException
) -> JSONResponse:
    """Handle domain BusinessException → ApiResponse error."""
    logger.warning(
        "BusinessException: code=%s message=%s path=%s",
        exc.code,
        exc.message,
        request.url.path,
    )
    response = ApiResponse.fail(
        exc.code, exc.message, path=request.url.path
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=response.model_dump(by_alias=True, exclude_none=True),
    )


async def http_exception_handler(
    request: Request, exc: StarletteHTTPException
) -> JSONResponse:
    """Handle HTTPException → ApiResponse error with common error codes."""
    code, default_msg = _STATUS_TO_CODE.get(
        exc.status_code, (f"C{exc.status_code:03d}", HTTPStatus(exc.status_code).phrase)
    )
    message = str(exc.detail) if exc.detail else default_msg

    response = ApiResponse.fail(code, message, path=request.url.path)
    return JSONResponse(
        status_code=exc.status_code,
        content=response.model_dump(by_alias=True, exclude_none=True),
    )


async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Handle Pydantic RequestValidationError → ApiResponse with field details."""
    details = [
        FieldError(
            field=".".join(str(loc) for loc in err["loc"] if loc != "body"),
            message=err["msg"],
            rejected_value=err.get("input"),
        )
        for err in exc.errors()
    ]
    logger.warning(
        "Validation error: path=%s fields=%s",
        request.url.path,
        [d.field for d in details],
    )
    response = ApiResponse.fail(
        "C002", "Invalid Input Value", path=request.url.path, details=details
    )
    return JSONResponse(
        status_code=422,
        content=response.model_dump(by_alias=True, exclude_none=True),
    )


async def unhandled_exception_handler(
    request: Request, exc: Exception
) -> JSONResponse:
    """Catch-all for unhandled exceptions → ApiResponse 500."""
    logger.exception(
        "Unhandled exception: path=%s", request.url.path
    )
    response = ApiResponse.fail(
        "C001", "Internal Server Error", path=request.url.path
    )
    return JSONResponse(
        status_code=500,
        content=response.model_dump(by_alias=True, exclude_none=True),
    )


def register_exception_handlers(app) -> None:
    """Register all exception handlers on the FastAPI app."""
    app.add_exception_handler(BusinessException, business_exception_handler)
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(RequestValidationError, validation_exception_handler)
    app.add_exception_handler(Exception, unhandled_exception_handler)
