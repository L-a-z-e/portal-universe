"""Audit logging middleware for tracking state-changing requests.

Logs userId, method, path, duration, and status for POST/PUT/PATCH/DELETE
requests, matching the Java @AuditLog and NestJS AuditInterceptor patterns.
"""

import logging
import time

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger("AuditLog")

_AUDITABLE_METHODS = {"POST", "PUT", "PATCH", "DELETE"}


class AuditMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        if request.method not in _AUDITABLE_METHODS:
            return await call_next(request)

        user_id = request.headers.get("x-user-id", "anonymous")
        start = time.monotonic()

        try:
            response = await call_next(request)
            duration = int((time.monotonic() - start) * 1000)

            logger.info(
                {
                    "userId": user_id,
                    "method": request.method,
                    "path": request.url.path,
                    "duration": duration,
                    "status": "success",
                    "statusCode": response.status_code,
                }
            )
            return response
        except Exception:
            duration = int((time.monotonic() - start) * 1000)
            logger.warning(
                {
                    "userId": user_id,
                    "method": request.method,
                    "path": request.url.path,
                    "duration": duration,
                    "status": "error",
                }
            )
            raise
