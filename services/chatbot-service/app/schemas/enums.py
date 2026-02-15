"""Domain enums for type safety."""

from enum import StrEnum


class MessageRole(StrEnum):
    """채팅 메시지 역할."""

    USER = "user"
    ASSISTANT = "assistant"
    SYSTEM = "system"


class DocumentStatus(StrEnum):
    """문서 인덱싱 상태."""

    INDEXED = "indexed"
    PENDING = "pending"
    FAILED = "failed"
