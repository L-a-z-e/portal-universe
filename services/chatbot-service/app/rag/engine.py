import logging
import uuid
from pathlib import Path

from langchain_community.document_loaders import (
    PyPDFLoader,
    TextLoader,
    UnstructuredMarkdownLoader,
)

from app.core.config import settings
from app.providers.base import EmbeddingProvider, LLMProvider
from app.providers.factory import create_embedding_provider, create_llm_provider
from app.rag.chunker import create_text_splitter
from app.rag.vectorstore import VectorStoreManager
from app.schemas.chat import SourceInfo

logger = logging.getLogger(__name__)

LOADER_MAP = {
    ".md": UnstructuredMarkdownLoader,
    ".txt": TextLoader,
    ".pdf": PyPDFLoader,
}


class RAGEngine:
    """RAG 파이프라인 핵심 엔진."""

    def __init__(self) -> None:
        self._llm: LLMProvider | None = None
        self._embedding: EmbeddingProvider | None = None
        self._vectorstore: VectorStoreManager | None = None
        self._initialized = False

    @property
    def llm(self) -> LLMProvider:
        assert self._llm is not None, "RAG engine not initialized"
        return self._llm

    @property
    def vectorstore(self) -> VectorStoreManager:
        assert self._vectorstore is not None, "RAG engine not initialized"
        return self._vectorstore

    async def initialize(self) -> None:
        """엔진 초기화: Provider 생성 + VectorStore 연결."""
        if self._initialized:
            return
        self._llm = create_llm_provider()
        self._embedding = create_embedding_provider()
        self._vectorstore = VectorStoreManager(self._embedding.get_embeddings())
        self._initialized = True
        logger.info(
            "RAG engine ready: llm=%s, embedding=%s",
            settings.ai_provider,
            settings.embedding_provider,
        )

    def load_and_index_file(self, file_path: Path) -> tuple[str, int]:
        """파일을 로드하고 벡터 스토어에 인덱싱. (document_id, chunk_count) 반환."""
        suffix = file_path.suffix.lower()
        loader_cls = LOADER_MAP.get(suffix)
        if loader_cls is None:
            raise ValueError(f"Unsupported file type: {suffix}. Supported: {list(LOADER_MAP)}")

        loader = loader_cls(str(file_path))
        raw_docs = loader.load()

        splitter = create_text_splitter()
        chunks = splitter.split_documents(raw_docs)

        doc_id = str(uuid.uuid4())
        for chunk in chunks:
            chunk.metadata["source"] = file_path.name
            chunk.metadata["document_id"] = doc_id

        self.vectorstore.add_documents(chunks)
        logger.info("Indexed file %s: %d chunks, doc_id=%s", file_path.name, len(chunks), doc_id)
        return doc_id, len(chunks)

    @staticmethod
    def _preprocess_query(question: str) -> str:
        """질문 전처리: 불필요한 공백 제거, 핵심어 정제."""
        # 양 끝 공백 및 연속 공백 제거
        cleaned = " ".join(question.split())
        # 질문 끝 물음표 정규화
        cleaned = cleaned.rstrip("?？").strip()
        return cleaned if cleaned else question

    async def query(self, question: str) -> tuple[str, list[SourceInfo]]:
        """질문에 대한 RAG 기반 답변 생성."""
        processed_question = self._preprocess_query(question)
        results = self.vectorstore.search(processed_question)

        if not results:
            return "해당 정보를 찾을 수 없습니다. 제공된 문서에 관련 내용이 없습니다.", []

        context = "\n\n---\n\n".join(
            f"[출처: {doc.metadata.get('source', 'unknown')}]\n{doc.page_content}"
            for doc, _ in results
        )

        answer = await self.llm.generate(question, context)

        sources = [
            SourceInfo(
                document=doc.metadata.get("source", "unknown"),
                chunk=doc.page_content[:200],
                relevance_score=round(score, 3),
            )
            for doc, score in results
        ]

        return answer, sources

    async def query_stream(self, question: str):
        """질문에 대한 RAG 기반 스트리밍 답변 생성."""
        processed_question = self._preprocess_query(question)
        results = self.vectorstore.search(processed_question)

        if not results:
            yield {
                "type": "token",
                "content": "해당 정보를 찾을 수 없습니다. 제공된 문서에 관련 내용이 없습니다.",
            }
            yield {"type": "done"}
            return

        context = "\n\n---\n\n".join(
            f"[출처: {doc.metadata.get('source', 'unknown')}]\n{doc.page_content}"
            for doc, _ in results
        )

        async for token in self.llm.stream(question, context):
            yield {"type": "token", "content": token}

        sources = [
            {
                "document": doc.metadata.get("source", "unknown"),
                "chunk": doc.page_content[:200],
                "relevance_score": round(score, 3),
            }
            for doc, score in results
        ]
        yield {"type": "sources", "sources": sources}
        yield {"type": "done"}


# 싱글톤 인스턴스
rag_engine = RAGEngine()
