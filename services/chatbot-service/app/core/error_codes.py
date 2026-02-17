"""Chatbot service error codes.

모든 에러 코드는 'CH' prefix + 3자리 숫자를 사용합니다.
"""

from enum import Enum


class ChatbotErrorCode(Enum):
    """Chatbot service 에러 코드 정의."""

    # CH001-CH009: General
    ENGINE_NOT_INITIALIZED = ("CH001", "RAG engine not initialized", 500)
    INTERNAL_ERROR = ("CH002", "Internal server error", 500)

    # CH010-CH019: Document
    UNSUPPORTED_FILE_TYPE = ("CH010", "Unsupported file type", 400)
    INVALID_FILENAME = ("CH011", "Invalid filename", 400)
    FILE_TOO_LARGE = ("CH012", "File too large", 400)
    DOCUMENT_NOT_FOUND = ("CH013", "Document not found", 404)

    # CH020-CH029: Auth
    AUTHENTICATION_REQUIRED = ("CH020", "Authentication required", 401)
    ADMIN_REQUIRED = ("CH021", "Admin role required", 403)

    # CH030-CH039: Conversation
    CONVERSATION_NOT_FOUND = ("CH030", "Conversation not found", 404)

    # CH040-CH049: Provider
    PROVIDER_ERROR = ("CH040", "AI provider error", 502)

    def __init__(self, code: str, message: str, status_code: int):
        self._code = code
        self._message = message
        self._status_code = status_code

    @property
    def code(self) -> str:
        return self._code

    @property
    def default_message(self) -> str:
        return self._message

    @property
    def status_code(self) -> int:
        return self._status_code
