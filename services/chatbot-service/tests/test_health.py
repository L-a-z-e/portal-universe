from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.fixture
async def client():
    """Mock된 RAG engine으로 테스트 클라이언트 생성."""
    with patch("app.rag.engine.rag_engine.initialize", new_callable=AsyncMock):
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as ac:
            yield ac


@pytest.mark.asyncio
async def test_health_endpoint(client):
    """Health 엔드포인트가 정상 응답하는지 확인."""
    with patch("app.api.routes.health.rag_engine") as mock_engine:
        mock_engine._initialized = True
        mock_engine.vectorstore.get_document_count.return_value = 5

        response = await client.get("/api/v1/chat/health")
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "healthy"
        assert "provider" in data
        assert "model" in data
        assert data["documents_count"] == 5
