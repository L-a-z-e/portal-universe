"""문서 검색 로직 모듈.

VectorStoreManager를 활용한 문서 검색 및 score 기반 필터링을 담당합니다.
"""

from langchain_core.documents import Document

from app.core.config import settings
from app.rag.vectorstore import VectorStoreManager


class DocumentRetriever:
    """문서 검색 및 컨텍스트 조합."""

    def __init__(self, vectorstore: VectorStoreManager):
        self._vectorstore = vectorstore

    def retrieve(
        self,
        query: str,
        k: int | None = None,
        score_threshold: float | None = None,
    ) -> list[tuple[Document, float]]:
        """쿼리 기반 유사 문서 검색."""
        return self._vectorstore.search(
            query,
            k=k or settings.rag_top_k,
            score_threshold=score_threshold or settings.rag_score_threshold,
        )

    def retrieve_as_context(self, query: str) -> tuple[str, list[tuple[Document, float]]]:
        """검색 결과를 컨텍스트 문자열로 조합하여 반환."""
        results = self.retrieve(query)

        if not results:
            return "", results

        context = "\n\n---\n\n".join(
            f"[출처: {doc.metadata.get('source', 'unknown')}]\n{doc.page_content}"
            for doc, _ in results
        )
        return context, results
