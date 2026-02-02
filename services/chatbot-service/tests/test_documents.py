from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.fixture
async def client():
    with patch("app.rag.engine.rag_engine.initialize", new_callable=AsyncMock):
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://test"
        ) as ac:
            yield ac


@pytest.mark.asyncio
async def test_upload_requires_auth(client):
    """인증 없이 문서 업로드 시 401."""
    response = await client.post(
        "/api/v1/chat/documents/upload",
        files={"file": ("test.md", b"# Test", "text/markdown")},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_upload_requires_admin_role(client):
    """관리자가 아닌 사용자가 문서 업로드 시 403."""
    response = await client.post(
        "/api/v1/chat/documents/upload",
        files={"file": ("test.md", b"# Test", "text/markdown")},
        headers={"X-User-Id": "user-123", "X-User-Roles": "ROLE_USER"},
    )
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_upload_unsupported_file(client):
    """지원하지 않는 파일 형식 업로드 시 400."""
    response = await client.post(
        "/api/v1/chat/documents/upload",
        files={"file": ("test.exe", b"binary", "application/octet-stream")},
        headers={"X-User-Id": "user-123"},
    )
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_upload_document(client, tmp_path):
    """정상적인 문서 업로드 및 인덱싱."""
    with patch("app.api.routes.documents.settings") as mock_settings:
        mock_settings.documents_dir = str(tmp_path)

        with patch("app.api.routes.documents.rag_engine") as mock_engine:
            mock_engine.load_and_index_file.return_value = ("doc-uuid", 5)

            response = await client.post(
                "/api/v1/chat/documents/upload",
                files={"file": ("test.md", b"# Hello World\n\nContent here", "text/markdown")},
                headers={"X-User-Id": "user-123"},
            )

            assert response.status_code == 200
            data = response.json()
            assert data["success"] is True
            assert data["data"]["filename"] == "test.md"
            assert data["data"]["chunks"] == 5
            assert data["data"]["status"] == "indexed"


@pytest.mark.asyncio
async def test_list_documents(client):
    """문서 목록 조회."""
    with patch("app.api.routes.documents.settings") as mock_settings:
        mock_settings.documents_dir = "/nonexistent"

        response = await client.get(
            "/api/v1/chat/documents",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["total"] == 0


@pytest.mark.asyncio
async def test_delete_nonexistent_document(client):
    """존재하지 않는 문서 삭제 시 404."""
    with patch("app.api.routes.documents.settings") as mock_settings:
        mock_settings.documents_dir = "/nonexistent"

        response = await client.delete(
            "/api/v1/chat/documents/doc-uuid-nonexistent",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 404


@pytest.mark.asyncio
async def test_delete_document_by_id(client, tmp_path):
    """document_id로 문서 삭제."""
    # 테스트 파일 생성
    test_file = tmp_path / "test.md"
    test_file.write_text("# Test document")

    with (
        patch("app.api.routes.documents.settings") as mock_settings,
        patch("app.api.routes.documents.rag_engine") as mock_engine,
    ):
        mock_settings.documents_dir = str(tmp_path)
        mock_engine.vectorstore.delete_by_source.return_value = None

        response = await client.delete(
            "/api/v1/chat/documents/test.md",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True


@pytest.mark.asyncio
async def test_reindex_documents(client, tmp_path):
    """전체 문서 재인덱싱."""
    # 테스트 파일 생성
    test_file = tmp_path / "test.md"
    test_file.write_text("# Test document")

    with (
        patch("app.api.routes.documents.settings") as mock_settings,
        patch("app.api.routes.documents.rag_engine") as mock_engine,
    ):
        mock_settings.documents_dir = str(tmp_path)
        mock_settings.ALLOWED_EXTENSIONS = {".md", ".txt", ".pdf"}
        mock_engine.load_and_index_file.return_value = ("doc-uuid", 3)

        response = await client.post(
            "/api/v1/chat/documents/reindex",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["total"] >= 0
