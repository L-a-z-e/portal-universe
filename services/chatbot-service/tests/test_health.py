from unittest.mock import AsyncMock, patch

import pytest


# ============================================================
# 기존 테스트 (1개)
# ============================================================


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


# ============================================================
# 신규 테스트 (2개)
# ============================================================


@pytest.mark.asyncio
async def test_health_not_initialized(client):
    """RAG engine 미초기화 시 documents_count=0."""
    with patch("app.api.routes.health.rag_engine") as mock_engine:
        mock_engine._initialized = False

        response = await client.get("/api/v1/chat/health")
        assert response.status_code == 200

        data = response.json()
        assert data["status"] == "healthy"
        assert data["documents_count"] == 0
        # vectorstore 접근하지 않아야 함
        mock_engine.vectorstore.get_document_count.assert_not_called()


@pytest.mark.asyncio
async def test_health_response_fields(client):
    """Health 응답에 필수 5개 필드가 모두 존재."""
    with patch("app.api.routes.health.rag_engine") as mock_engine:
        mock_engine._initialized = True
        mock_engine.vectorstore.get_document_count.return_value = 0

        response = await client.get("/api/v1/chat/health")
        assert response.status_code == 200

        data = response.json()
        expected_fields = {"status", "provider", "model", "vectorstore", "documents_count"}
        assert expected_fields == set(data.keys())
