import json
from unittest.mock import AsyncMock, patch

import pytest

from app.schemas.chat import SourceInfo

# ============================================================
# 기존 테스트 (8개)
# ============================================================


@pytest.mark.asyncio
async def test_chat_message_requires_auth(client):
    """인증 없이 chat message 호출 시 401."""
    response = await client.post(
        "/api/v1/chat/message",
        json={"message": "hello"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_chat_message_with_auth(client):
    """인증된 사용자가 chat message 호출 시 정상 응답."""
    mock_sources = [SourceInfo(document="test.md", chunk="test content", relevance_score=0.95)]

    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):
        mock_engine.query = AsyncMock(return_value=("Test answer", mock_sources))
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/message",
            json={"message": "What is the shipping policy?"},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["answer"] == "Test answer"
        assert len(data["data"]["sources"]) == 1
        assert data["data"]["sources"][0]["document"] == "test.md"


@pytest.mark.asyncio
async def test_conversations_requires_auth(client):
    """인증 없이 conversations 조회 시 401."""
    response = await client.get("/api/v1/chat/conversations")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_list_conversations(client):
    """인증된 사용자의 대화 목록 조회."""
    with patch("app.api.routes.chat.conversation_service") as mock_conv:
        mock_conv.list_conversations = AsyncMock(return_value=[])

        response = await client.get(
            "/api/v1/chat/conversations",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True


@pytest.mark.asyncio
async def test_delete_conversation(client):
    """대화 삭제 API."""
    with patch("app.api.routes.chat.conversation_service") as mock_conv:
        mock_conv.delete_conversation = AsyncMock()

        response = await client.delete(
            "/api/v1/chat/conversations/conv-123",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True


@pytest.mark.asyncio
async def test_get_conversation_detail(client):
    """특정 대화 이력 조회."""
    mock_messages = [
        {"message_id": "msg-1", "role": "user", "content": "hello"},
        {"message_id": "msg-2", "role": "assistant", "content": "hi there"},
    ]

    with patch("app.api.routes.chat.conversation_service") as mock_conv:
        mock_conv.get_messages = AsyncMock(return_value=mock_messages)

        response = await client.get(
            "/api/v1/chat/conversations/conv-123",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert len(data["data"]) == 2


@pytest.mark.asyncio
async def test_stream_requires_auth(client):
    """인증 없이 stream 호출 시 401."""
    response = await client.post(
        "/api/v1/chat/stream",
        json={"message": "hello"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_stream_endpoint_exists(client):
    """SSE stream 엔드포인트 존재 확인."""
    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):

        async def mock_stream(question):
            yield {"type": "token", "content": "Test"}
            yield {"type": "sources", "sources": []}
            yield {"type": "done"}

        mock_engine.query_stream = mock_stream
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/stream",
            json={"message": "hello"},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        assert "text/event-stream" in response.headers.get("content-type", "")


# ============================================================
# 신규 테스트 (7개)
# ============================================================


@pytest.mark.asyncio
async def test_chat_message_creates_conversation_id(client):
    """conversation_id 없이 전송 시 UUID 형식의 conversation_id가 생성."""
    import uuid

    mock_sources = [SourceInfo(document="doc.md", chunk="text", relevance_score=0.9)]

    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):
        mock_engine.query = AsyncMock(return_value=("answer", mock_sources))
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/message",
            json={"message": "hello"},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        conv_id = data["data"]["conversation_id"]
        # UUID 형식 검증
        parsed = uuid.UUID(conv_id)
        assert str(parsed) == conv_id


@pytest.mark.asyncio
async def test_chat_message_uses_existing_conversation_id(client):
    """기존 conversation_id를 전송하면 그대로 사용."""
    mock_sources = []

    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):
        mock_engine.query = AsyncMock(return_value=("answer", mock_sources))
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/message",
            json={"message": "hello", "conversation_id": "my-conv-1"},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["data"]["conversation_id"] == "my-conv-1"


@pytest.mark.asyncio
async def test_chat_message_saves_user_and_assistant(client):
    """chat message 호출 시 user + assistant 두 번 save_message 호출."""
    mock_sources = [SourceInfo(document="doc.md", chunk="text", relevance_score=0.9)]

    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):
        mock_engine.query = AsyncMock(return_value=("Test answer", mock_sources))
        mock_conv.save_message = AsyncMock()

        await client.post(
            "/api/v1/chat/message",
            json={"message": "question"},
            headers={"X-User-Id": "user-123"},
        )

        assert mock_conv.save_message.call_count == 2

        # 첫 번째 호출: user 메시지
        first_call = mock_conv.save_message.call_args_list[0]
        assert first_call.kwargs["role"] == "user"
        assert first_call.kwargs["content"] == "question"

        # 두 번째 호출: assistant 메시지
        second_call = mock_conv.save_message.call_args_list[1]
        assert second_call.kwargs["role"] == "assistant"
        assert second_call.kwargs["content"] == "Test answer"
        assert second_call.kwargs["sources"] == mock_sources


@pytest.mark.asyncio
async def test_chat_message_returns_sources(client):
    """응답에 sources 구조가 포함."""
    mock_sources = [
        SourceInfo(document="guide.md", chunk="chunk content", relevance_score=0.88),
        SourceInfo(document="faq.md", chunk="faq content", relevance_score=0.75),
    ]

    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):
        mock_engine.query = AsyncMock(return_value=("answer", mock_sources))
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/message",
            json={"message": "question"},
            headers={"X-User-Id": "user-123"},
        )

        data = response.json()
        sources = data["data"]["sources"]
        assert len(sources) == 2
        assert sources[0]["document"] == "guide.md"
        assert sources[0]["relevance_score"] == 0.88
        assert sources[1]["document"] == "faq.md"


@pytest.mark.asyncio
async def test_stream_event_order(client):
    """SSE 스트림에 token, sources, done 이벤트가 순서대로 포함."""
    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):

        async def mock_stream(question):
            yield {"type": "token", "content": "Hello"}
            yield {"type": "token", "content": " World"}
            yield {
                "type": "sources",
                "sources": [{"document": "d.md", "chunk": "c", "relevance_score": 0.9}],
            }
            yield {"type": "done"}

        mock_engine.query_stream = mock_stream
        mock_conv.save_message = AsyncMock()

        response = await client.post(
            "/api/v1/chat/stream",
            json={"message": "hello"},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        body = response.text

        # SSE 이벤트에서 data 라인 추출
        events = []
        for line in body.strip().split("\n"):
            line = line.strip()
            if line.startswith("data:"):
                event_data = json.loads(line[len("data:") :].strip())
                events.append(event_data["type"])

        assert "token" in events
        assert "sources" in events
        assert "done" in events
        # done은 마지막
        assert events[-1] == "done"


@pytest.mark.asyncio
async def test_stream_collects_full_answer(client):
    """스트림 종료 시 수집된 전체 답변이 assistant 메시지로 저장."""
    with (
        patch("app.api.routes.chat.rag_engine") as mock_engine,
        patch("app.api.routes.chat.conversation_service") as mock_conv,
    ):

        async def mock_stream(question):
            yield {"type": "token", "content": "Hello"}
            yield {"type": "token", "content": " World"}
            yield {"type": "sources", "sources": []}
            yield {"type": "done"}

        mock_engine.query_stream = mock_stream
        mock_conv.save_message = AsyncMock()

        await client.post(
            "/api/v1/chat/stream",
            json={"message": "test question"},
            headers={"X-User-Id": "user-123"},
        )

        # save_message가 2번 호출 (user + assistant)
        assert mock_conv.save_message.call_count == 2

        # assistant 메시지에 수집된 전체 답변이 포함
        assistant_call = mock_conv.save_message.call_args_list[1]
        assert assistant_call.kwargs["role"] == "assistant"
        assert assistant_call.kwargs["content"] == "Hello World"


@pytest.mark.asyncio
async def test_delete_conversation_returns_id(client):
    """대화 삭제 응답에 deleted conversation_id가 포함."""
    with patch("app.api.routes.chat.conversation_service") as mock_conv:
        mock_conv.delete_conversation = AsyncMock()

        response = await client.delete(
            "/api/v1/chat/conversations/conv-123",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["deleted"] == "conv-123"
