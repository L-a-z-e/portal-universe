"""대화 이력 모델."""

from datetime import datetime, timezone

from pydantic import BaseModel, Field


class Message(BaseModel):
    """단일 메시지 모델."""

    message_id: str
    role: str  # "user" | "assistant"
    content: str
    sources: list[dict] | None = None
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class Conversation(BaseModel):
    """대화 세션 모델."""

    conversation_id: str
    user_id: str
    title: str = "New conversation"
    messages: list[Message] = Field(default_factory=list)
    message_count: int = 0
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    updated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
