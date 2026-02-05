"""RAGEngine 단위 테스트."""

from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from langchain_core.documents import Document

import app.rag.engine as engine_module
from app.rag.engine import RAGEngine
from app.schemas.chat import SourceInfo


class TestRAGEngineInitialize:
    """RAGEngine 초기화 테스트."""

    @pytest.mark.asyncio
    async def test_initialize_creates_providers(self):
        """initialize() 호출 후 _llm, _vectorstore가 설정된다."""
        engine = RAGEngine()

        mock_llm = MagicMock()
        mock_embedding = MagicMock()
        mock_vs = MagicMock()

        with (
            patch("app.rag.engine.create_llm_provider", return_value=mock_llm),
            patch("app.rag.engine.create_embedding_provider", return_value=mock_embedding),
            patch("app.rag.engine.VectorStoreManager", return_value=mock_vs),
        ):
            await engine.initialize()

        assert engine._llm is mock_llm
        assert engine._vectorstore is mock_vs
        assert engine._initialized is True

    @pytest.mark.asyncio
    async def test_initialize_idempotent(self):
        """initialize()를 두 번 호출해도 provider가 한 번만 생성된다."""
        engine = RAGEngine()

        mock_llm = MagicMock()
        mock_embedding = MagicMock()
        mock_vs = MagicMock()

        with (
            patch(
                "app.rag.engine.create_llm_provider", return_value=mock_llm
            ) as mock_create_llm,
            patch("app.rag.engine.create_embedding_provider", return_value=mock_embedding),
            patch("app.rag.engine.VectorStoreManager", return_value=mock_vs),
        ):
            await engine.initialize()
            await engine.initialize()

        mock_create_llm.assert_called_once()


class TestRAGEngineProperties:
    """RAGEngine property 접근 테스트."""

    def test_llm_property_before_init(self):
        """초기화 전 llm 접근 시 AssertionError가 발생한다."""
        engine = RAGEngine()
        with pytest.raises(AssertionError, match="RAG engine not initialized"):
            _ = engine.llm

    def test_vectorstore_property_before_init(self):
        """초기화 전 vectorstore 접근 시 AssertionError가 발생한다."""
        engine = RAGEngine()
        with pytest.raises(AssertionError, match="RAG engine not initialized"):
            _ = engine.vectorstore

    def test_llm_property_after_init(self):
        """초기화 후 llm property가 provider를 반환한다."""
        engine = RAGEngine()
        mock_llm = MagicMock()
        engine._llm = mock_llm
        assert engine.llm is mock_llm


class TestRAGEnginePreprocessQuery:
    """RAGEngine._preprocess_query 정적 메서드 테스트."""

    def test_preprocess_query_strips_whitespace(self):
        """양 끝 공백이 제거된다."""
        assert RAGEngine._preprocess_query("  hello  ") == "hello"

    def test_preprocess_query_removes_question_mark(self):
        """끝의 물음표(?)가 제거된다."""
        assert RAGEngine._preprocess_query("질문?") == "질문"

    def test_preprocess_query_removes_fullwidth_question(self):
        """끝의 전각 물음표(？)가 제거된다."""
        result = RAGEngine._preprocess_query("질문？")
        assert result == "질문"

    def test_preprocess_query_normalizes_spaces(self):
        """연속 공백이 하나로 정규화된다."""
        assert RAGEngine._preprocess_query("a  b   c") == "a b c"

    def test_preprocess_query_empty_returns_original(self):
        """전처리 결과가 빈 문자열이면 원본을 반환한다."""
        result = RAGEngine._preprocess_query("?")
        assert result == "?"


class TestRAGEngineLoadAndIndex:
    """RAGEngine.load_and_index_file 테스트."""

    def _make_engine_with_vectorstore(self):
        """vectorstore가 설정된 엔진을 반환한다."""
        engine = RAGEngine()
        engine._vectorstore = MagicMock()
        engine._vectorstore.add_documents = MagicMock(return_value=["id1", "id2"])
        engine._initialized = True
        return engine

    def test_load_and_index_md_file(self):
        """Markdown 파일 로드 시 해당 loader가 사용된다."""
        engine = self._make_engine_with_vectorstore()

        mock_loader_cls = MagicMock()
        mock_loader_instance = mock_loader_cls.return_value
        mock_loader_instance.load.return_value = [
            Document(page_content="# Hello", metadata={})
        ]

        mock_splitter = MagicMock()
        mock_splitter.split_documents.return_value = [
            Document(page_content="chunk1", metadata={}),
            Document(page_content="chunk2", metadata={}),
        ]

        with (
            patch.dict(engine_module.LOADER_MAP, {".md": mock_loader_cls}),
            patch("app.rag.engine.create_text_splitter", return_value=mock_splitter),
        ):
            doc_id, count = engine.load_and_index_file(Path("test.md"))

        mock_loader_cls.assert_called_once_with("test.md")
        assert count == 2
        assert isinstance(doc_id, str)

    def test_load_and_index_txt_file(self):
        """텍스트 파일 로드 시 TextLoader가 사용된다."""
        engine = self._make_engine_with_vectorstore()

        mock_loader_cls = MagicMock()
        mock_loader_instance = mock_loader_cls.return_value
        mock_loader_instance.load.return_value = [
            Document(page_content="text content", metadata={})
        ]

        mock_splitter = MagicMock()
        mock_splitter.split_documents.return_value = [
            Document(page_content="chunk1", metadata={})
        ]

        with (
            patch.dict(engine_module.LOADER_MAP, {".txt": mock_loader_cls}),
            patch("app.rag.engine.create_text_splitter", return_value=mock_splitter),
        ):
            doc_id, count = engine.load_and_index_file(Path("test.txt"))

        mock_loader_cls.assert_called_once_with("test.txt")
        assert count == 1

    def test_load_and_index_pdf_file(self):
        """PDF 파일 로드 시 PyPDFLoader가 사용된다."""
        engine = self._make_engine_with_vectorstore()

        mock_loader_cls = MagicMock()
        mock_loader_instance = mock_loader_cls.return_value
        mock_loader_instance.load.return_value = [
            Document(page_content="pdf content", metadata={})
        ]

        mock_splitter = MagicMock()
        mock_splitter.split_documents.return_value = [
            Document(page_content="chunk1", metadata={}),
            Document(page_content="chunk2", metadata={}),
            Document(page_content="chunk3", metadata={}),
        ]

        with (
            patch.dict(engine_module.LOADER_MAP, {".pdf": mock_loader_cls}),
            patch("app.rag.engine.create_text_splitter", return_value=mock_splitter),
        ):
            doc_id, count = engine.load_and_index_file(Path("test.pdf"))

        mock_loader_cls.assert_called_once_with("test.pdf")
        assert count == 3

    def test_load_unsupported_file_type(self):
        """지원하지 않는 파일 확장자는 ValueError가 발생한다."""
        engine = self._make_engine_with_vectorstore()

        with pytest.raises(ValueError, match="Unsupported file type"):
            engine.load_and_index_file(Path("malware.exe"))


class TestRAGEngineQuery:
    """RAGEngine.query 테스트."""

    def _make_engine_with_mocks(self):
        """_llm, _vectorstore가 설정된 엔진을 반환한다."""
        engine = RAGEngine()
        engine._llm = MagicMock()
        engine._vectorstore = MagicMock()
        engine._initialized = True
        return engine

    @pytest.mark.asyncio
    async def test_query_with_results(self):
        """검색 결과가 있으면 LLM 답변과 sources를 반환한다."""
        engine = self._make_engine_with_mocks()

        doc = Document(page_content="test content", metadata={"source": "test.md"})
        engine._vectorstore.search.return_value = [(doc, 0.95)]
        engine._llm.generate = AsyncMock(return_value="Generated answer")

        answer, sources = await engine.query("test question")

        assert answer == "Generated answer"
        assert len(sources) == 1
        assert isinstance(sources[0], SourceInfo)
        assert sources[0].document == "test.md"
        assert sources[0].relevance_score == 0.95

    @pytest.mark.asyncio
    async def test_query_no_results(self):
        """검색 결과가 없으면 기본 메시지와 빈 sources를 반환한다."""
        engine = self._make_engine_with_mocks()
        engine._vectorstore.search.return_value = []

        answer, sources = await engine.query("unknown question")

        assert "찾을 수 없습니다" in answer
        assert sources == []


class TestRAGEngineQueryStream:
    """RAGEngine.query_stream 테스트."""

    def _make_engine_with_mocks(self):
        """_llm, _vectorstore가 설정된 엔진을 반환한다."""
        engine = RAGEngine()
        engine._llm = MagicMock()
        engine._vectorstore = MagicMock()
        engine._initialized = True
        return engine

    @pytest.mark.asyncio
    async def test_query_stream_with_results(self):
        """검색 결과가 있으면 token, sources, done 이벤트를 yield한다."""
        engine = self._make_engine_with_mocks()

        doc = Document(page_content="test content", metadata={"source": "test.md"})
        engine._vectorstore.search.return_value = [(doc, 0.9)]

        async def mock_stream(prompt, context):
            yield "Hello"
            yield " World"

        engine._llm.stream = mock_stream

        events = [e async for e in engine.query_stream("test")]

        token_events = [e for e in events if e["type"] == "token"]
        source_events = [e for e in events if e["type"] == "sources"]
        done_events = [e for e in events if e["type"] == "done"]

        assert len(token_events) == 2
        assert token_events[0]["content"] == "Hello"
        assert token_events[1]["content"] == " World"
        assert len(source_events) == 1
        assert len(source_events[0]["sources"]) == 1
        assert len(done_events) == 1

    @pytest.mark.asyncio
    async def test_query_stream_no_results(self):
        """검색 결과가 없으면 기본 메시지 token과 done 이벤트를 yield한다."""
        engine = self._make_engine_with_mocks()
        engine._vectorstore.search.return_value = []

        events = [e async for e in engine.query_stream("unknown")]

        assert len(events) == 2
        assert events[0]["type"] == "token"
        assert "찾을 수 없습니다" in events[0]["content"]
        assert events[1]["type"] == "done"
