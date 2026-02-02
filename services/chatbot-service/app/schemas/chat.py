from pydantic import BaseModel


class ChatRequest(BaseModel):
    message: str
    conversation_id: str | None = None


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
