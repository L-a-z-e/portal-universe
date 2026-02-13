from unittest.mock import MagicMock, patch

import pytest

# ============================================================
# 기존 테스트 (8개)
# ============================================================


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


# ============================================================
# 신규 테스트 (5개)
# ============================================================


@pytest.mark.asyncio
async def test_upload_no_filename(client):
    """파일명 없이 업로드 시 에러(422 - FastAPI가 빈 파일명을 유효한 UploadFile로 파싱하지 못함)."""
    response = await client.post(
        "/api/v1/chat/documents/upload",
        files={"file": ("", b"# Test content", "text/markdown")},
        headers={"X-User-Id": "user-123"},
    )
    # 빈 파일명은 httpx/starlette 레벨에서 UploadFile로 파싱되지 않아 422 반환
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_upload_file_too_large(client, tmp_path):
    """10MB 초과 파일 업로드 시 400."""
    with patch("app.api.routes.documents.settings") as mock_settings:
        mock_settings.documents_dir = str(tmp_path)

        # 10MB + 1 byte 크기의 파일
        large_content = b"x" * (10 * 1024 * 1024 + 1)
        response = await client.post(
            "/api/v1/chat/documents/upload",
            files={"file": ("large.txt", large_content, "text/plain")},
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 400


@pytest.mark.asyncio
async def test_list_documents_with_files(client, tmp_path):
    """실제 파일이 있는 디렉토리에서 문서 목록 조회."""
    # 테스트 파일 생성
    (tmp_path / "readme.md").write_text("# README")
    (tmp_path / "guide.txt").write_text("Guide content")
    (tmp_path / "image.png").write_bytes(b"not-a-doc")  # 지원하지 않는 확장자

    with patch("app.api.routes.documents.settings") as mock_settings:
        mock_settings.documents_dir = str(tmp_path)

        response = await client.get(
            "/api/v1/chat/documents",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        # .md와 .txt만 포함, .png 제외
        assert data["data"]["total"] == 2
        filenames = [d["filename"] for d in data["data"]["documents"]]
        assert "readme.md" in filenames
        assert "guide.txt" in filenames
        assert "image.png" not in filenames


@pytest.mark.asyncio
async def test_reindex_empty_directory(client, tmp_path):
    """빈 디렉토리 재인덱싱 시 total=0."""
    with (
        patch("app.api.routes.documents.settings") as mock_settings,
        patch("app.api.routes.documents.rag_engine") as mock_engine,
    ):
        mock_settings.documents_dir = str(tmp_path)
        mock_settings.ALLOWED_EXTENSIONS = {".md", ".txt", ".pdf"}

        response = await client.post(
            "/api/v1/chat/documents/reindex",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["total"] == 0
        assert data["data"]["reindexed"] == []
        mock_engine.load_and_index_file.assert_not_called()


@pytest.mark.asyncio
async def test_delete_document_removes_from_vectorstore(client, tmp_path):
    """문서 삭제 시 vectorstore의 delete_by_source가 호출."""
    test_file = tmp_path / "target.md"
    test_file.write_text("# Target document")

    with (
        patch("app.api.routes.documents.settings") as mock_settings,
        patch("app.api.routes.documents.rag_engine") as mock_engine,
    ):
        mock_settings.documents_dir = str(tmp_path)
        mock_engine.vectorstore = MagicMock()
        mock_engine.vectorstore.delete_by_source.return_value = None

        response = await client.delete(
            "/api/v1/chat/documents/target.md",
            headers={"X-User-Id": "user-123"},
        )

        assert response.status_code == 200
        mock_engine.vectorstore.delete_by_source.assert_called_once_with("target.md")
        # 파일도 실제로 삭제되었는지 확인
        assert not test_file.exists()
