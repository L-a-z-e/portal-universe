from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app
from app.schemas.chat import SourceInfo


@pytest.fixture
async def client():
    with patch("app.rag.engine.rag_engine.initialize", new_callable=AsyncMock):
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as ac:
            yield ac


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
    mock_sources = [
        SourceInfo(document="test.md", chunk="test content", relevance_score=0.95)
    ]

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
