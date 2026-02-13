import logging
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException, UploadFile, status

from app.core.config import settings
from app.core.security import get_current_user_id, require_admin
from app.rag.engine import rag_engine
from app.schemas.common import ApiResponse
from app.schemas.document import DocumentInfo

logger = logging.getLogger(__name__)

router = APIRouter()

ALLOWED_EXTENSIONS = {".md", ".txt", ".pdf"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB


@router.post("/upload")
async def upload_document(
    file: UploadFile,
    user_id: str = Depends(require_admin),
):
    """문서 업로드 및 인덱싱."""
    if not file.filename:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Filename required")

    suffix = Path(file.filename).suffix.lower()
    if suffix not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file type: {suffix}. Allowed: {ALLOWED_EXTENSIONS}",
        )

    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File too large. Max: {MAX_FILE_SIZE // (1024*1024)}MB",
        )

    # 파일 저장 (path traversal 방지)
    doc_dir = Path(settings.documents_dir).resolve()
    doc_dir.mkdir(parents=True, exist_ok=True)
    file_path = (doc_dir / Path(file.filename).name).resolve()
    if not file_path.is_relative_to(doc_dir):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid filename",
        )
    file_path.write_bytes(content)

    # 인덱싱
    doc_id, chunk_count = rag_engine.load_and_index_file(file_path)

    logger.info(
        "Document uploaded: user=%s, file=%s, chunks=%d",
        user_id,
        file.filename,
        chunk_count,
    )

    doc_info = DocumentInfo(
        document_id=doc_id,
        filename=file.filename,
        chunks=chunk_count,
        status="indexed",
    )
    return ApiResponse.ok(doc_info.model_dump())


@router.get("")
async def list_documents(user_id: str = Depends(get_current_user_id)):
    """인덱싱된 문서 목록."""
    doc_dir = Path(settings.documents_dir)
    documents = []
    if doc_dir.exists():
        for f in doc_dir.iterdir():
            if f.suffix.lower() in ALLOWED_EXTENSIONS:
                documents.append(
                    {
                        "document_id": f.stem,
                        "filename": f.name,
                        "size_bytes": f.stat().st_size,
                    }
                )
    return ApiResponse.ok({"documents": documents, "total": len(documents)})


@router.delete("/{document_id}")
async def delete_document(
    document_id: str,
    user_id: str = Depends(require_admin),
):
    """문서 삭제 및 벡터 제거."""
    # document_id로 파일 찾기 (메타데이터 또는 파일명 매핑)
    doc_dir = Path(settings.documents_dir).resolve()
    target_file: Path | None = None

    # document_id가 파일명인 경우도 지원 (하위 호환)
    candidate = (doc_dir / document_id).resolve()
    if candidate.is_relative_to(doc_dir) and candidate.exists():
        target_file = candidate
    else:
        # documents 디렉토리에서 파일 탐색
        if doc_dir.exists():
            for f in doc_dir.iterdir():
                if f.suffix.lower() in ALLOWED_EXTENSIONS:
                    # 파일명이 document_id와 매핑되는 경우
                    if f.stem == document_id or f.name == document_id:
                        target_file = f
                        break

    if target_file is None or not target_file.exists():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Document not found")

    filename = target_file.name

    # 벡터 스토어에서 삭제
    rag_engine.vectorstore.delete_by_source(filename)

    # 파일 삭제
    target_file.unlink()

    logger.info("Document deleted: user=%s, document_id=%s, file=%s", user_id, document_id, filename)
    return ApiResponse.ok({"deleted": document_id, "filename": filename})


@router.post("/reindex")
async def reindex_all(user_id: str = Depends(require_admin)):
    """전체 문서 재인덱싱."""
    doc_dir = Path(settings.documents_dir)
    indexed = []

    if doc_dir.exists():
        for f in doc_dir.iterdir():
            if f.suffix.lower() in ALLOWED_EXTENSIONS:
                doc_id, chunk_count = rag_engine.load_and_index_file(f)
                indexed.append({"filename": f.name, "document_id": doc_id, "chunks": chunk_count})

    logger.info("Reindexed %d documents", len(indexed))
    return ApiResponse.ok({"reindexed": indexed, "total": len(indexed)})
