from pydantic import BaseModel, Field, field_validator

from app.core.validators import check_no_xss


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=10000)
    conversation_id: str | None = None

    @field_validator("message")
    @classmethod
    def validate_message(cls, v: str) -> str:
        return check_no_xss(v)


class SourceInfo(BaseModel):
    document: str
    chunk: str
    relevance_score: float


class ChatResponse(BaseModel):
    answer: str
    sources: list[SourceInfo]
    conversation_id: str
    message_id: str


class ConversationSummary(BaseModel):
    conversation_id: str
    title: str
    message_count: int
    created_at: str
    updated_at: str


class ConversationMessage(BaseModel):
    message_id: str
    role: str  # "user" | "assistant"
    content: str
    sources: list[SourceInfo] | None = None
    created_at: str
