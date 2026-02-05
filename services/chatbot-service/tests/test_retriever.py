"""DocumentRetriever 단위 테스트."""

from unittest.mock import MagicMock, patch

from langchain_core.documents import Document

from app.rag.retriever import DocumentRetriever


class TestDocumentRetriever:
    """DocumentRetriever 테스트."""

    def _make_retriever(self, search_results=None):
        """mock VectorStoreManager를 주입한 DocumentRetriever를 생성한다."""
        mock_vs = MagicMock()
        mock_vs.search.return_value = search_results or []
        return DocumentRetriever(mock_vs), mock_vs

    def test_retrieve_delegates_to_vectorstore(self):
        """retrieve()가 vectorstore.search를 호출한다."""
        retriever, mock_vs = self._make_retriever()

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            retriever.retrieve("test query")

        mock_vs.search.assert_called_once()

    def test_retrieve_passes_params(self):
        """k, score_threshold 파라미터가 vectorstore.search에 전달된다."""
        retriever, mock_vs = self._make_retriever()

        retriever.retrieve("query", k=3, score_threshold=0.8)

        mock_vs.search.assert_called_once_with("query", k=3, score_threshold=0.8)

    def test_retrieve_as_context_with_results(self):
        """검색 결과가 있으면 context 문자열과 results를 반환한다."""
        doc = Document(page_content="test content", metadata={"source": "doc.md"})
        retriever, _ = self._make_retriever([(doc, 0.9)])

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            context, results = retriever.retrieve_as_context("query")

        assert len(results) == 1
        assert isinstance(context, str)
        assert len(context) > 0

    def test_retrieve_as_context_format(self):
        """context 문자열이 '[출처: {source}]\\n{content}' 형식이다."""
        doc = Document(page_content="some text", metadata={"source": "readme.md"})
        retriever, _ = self._make_retriever([(doc, 0.9)])

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            context, _ = retriever.retrieve_as_context("query")

        assert "[출처: readme.md]" in context
        assert "some text" in context

    def test_retrieve_as_context_separator(self):
        """여러 결과 간 구분자가 '\\n\\n---\\n\\n'이다."""
        doc1 = Document(page_content="first", metadata={"source": "a.md"})
        doc2 = Document(page_content="second", metadata={"source": "b.md"})
        retriever, _ = self._make_retriever([(doc1, 0.9), (doc2, 0.8)])

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            context, _ = retriever.retrieve_as_context("query")

        assert "\n\n---\n\n" in context

    def test_retrieve_as_context_empty(self):
        """검색 결과가 없으면 빈 문자열과 빈 리스트를 반환한다."""
        retriever, _ = self._make_retriever([])

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            context, results = retriever.retrieve_as_context("unknown")

        assert context == ""
        assert results == []

    def test_retrieve_as_context_unknown_source(self):
        """source metadata가 없으면 'unknown'이 사용된다."""
        doc = Document(page_content="no source doc", metadata={})
        retriever, _ = self._make_retriever([(doc, 0.85)])

        with patch("app.rag.retriever.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            context, _ = retriever.retrieve_as_context("query")

        assert "[출처: unknown]" in context
