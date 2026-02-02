from fastapi import APIRouter

from app.core.config import settings
from app.rag.engine import rag_engine

router = APIRouter()


@router.get("/health")
async def health_check():
    doc_count = 0
    if rag_engine._initialized:
        doc_count = rag_engine.vectorstore.get_document_count()

    return {
        "status": "healthy",
        "provider": settings.ai_provider,
        "model": settings.ai_model,
        "vectorstore": "chroma",
        "documents_count": doc_count,
    }
