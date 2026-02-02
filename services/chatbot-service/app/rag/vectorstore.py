import logging

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings

from app.core.config import settings

logger = logging.getLogger(__name__)

COLLECTION_NAME = "chatbot_documents"


class VectorStoreManager:
    """ChromaDB 벡터 스토어 관리."""

    def __init__(self, embeddings: Embeddings):
        self._store = Chroma(
            collection_name=COLLECTION_NAME,
            embedding_function=embeddings,
            persist_directory=settings.chroma_persist_dir,
        )
        logger.info("ChromaDB initialized: %s", settings.chroma_persist_dir)

    @property
    def store(self) -> Chroma:
        return self._store

    def add_documents(self, documents: list[Document]) -> list[str]:
        """문서 청크를 벡터 스토어에 추가."""
        ids = self._store.add_documents(documents)
        logger.info("Added %d document chunks to vector store", len(ids))
        return ids

    def search(
        self, query: str, k: int | None = None, score_threshold: float | None = None
    ) -> list[tuple[Document, float]]:
        """유사 문서 검색."""
        k = k or settings.rag_top_k
        score_threshold = score_threshold or settings.rag_score_threshold
        results = self._store.similarity_search_with_relevance_scores(query, k=k)
        # score threshold 필터링
        filtered = [(doc, score) for doc, score in results if score >= score_threshold]
        return filtered

    def delete_by_source(self, source: str) -> None:
        """특정 소스 문서의 모든 청크 삭제."""
        # ChromaDB where 필터로 삭제
        self._store._collection.delete(where={"source": source})
        logger.info("Deleted chunks for source: %s", source)

    def get_document_count(self) -> int:
        """저장된 총 청크 수."""
        return self._store._collection.count()
