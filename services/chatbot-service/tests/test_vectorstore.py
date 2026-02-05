"""VectorStoreManager 단위 테스트."""

from unittest.mock import MagicMock, patch

from langchain_core.documents import Document

from app.rag.vectorstore import COLLECTION_NAME, VectorStoreManager


class TestVectorStoreManagerInit:
    """VectorStoreManager 초기화 테스트."""

    @patch("app.rag.vectorstore.Chroma")
    def test_init_creates_chroma(self, mock_chroma_cls):
        """생성 시 Chroma가 올바른 파라미터로 호출된다."""
        mock_embeddings = MagicMock()

        with patch("app.rag.vectorstore.settings") as mock_settings:
            mock_settings.chroma_persist_dir = "/tmp/chroma"
            manager = VectorStoreManager(mock_embeddings)

        mock_chroma_cls.assert_called_once_with(
            collection_name=COLLECTION_NAME,
            embedding_function=mock_embeddings,
            persist_directory="/tmp/chroma",
        )

    @patch("app.rag.vectorstore.Chroma")
    def test_store_property(self, mock_chroma_cls):
        """store property가 _store를 반환한다."""
        mock_embeddings = MagicMock()

        with patch("app.rag.vectorstore.settings"):
            manager = VectorStoreManager(mock_embeddings)

        assert manager.store is manager._store


class TestVectorStoreManagerOperations:
    """VectorStoreManager 연산 테스트."""

    def _make_manager(self):
        """mock된 _store를 가진 VectorStoreManager를 생성한다."""
        with (
            patch("app.rag.vectorstore.Chroma"),
            patch("app.rag.vectorstore.settings"),
        ):
            manager = VectorStoreManager(MagicMock())
        return manager

    def test_add_documents(self):
        """add_documents가 _store.add_documents를 호출하고 id 목록을 반환한다."""
        manager = self._make_manager()
        docs = [Document(page_content="test", metadata={})]
        manager._store.add_documents.return_value = ["id-1"]

        result = manager.add_documents(docs)

        manager._store.add_documents.assert_called_once_with(docs)
        assert result == ["id-1"]

    def test_search_with_defaults(self):
        """검색 시 기본 settings 값(top_k, score_threshold)이 사용된다."""
        manager = self._make_manager()
        doc = Document(page_content="result", metadata={})
        manager._store.similarity_search_with_relevance_scores.return_value = [
            (doc, 0.9)
        ]

        with patch("app.rag.vectorstore.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            results = manager.search("test query")

        manager._store.similarity_search_with_relevance_scores.assert_called_once_with(
            "test query", k=5
        )
        assert len(results) == 1
        assert results[0][1] == 0.9

    def test_search_with_custom_params(self):
        """커스텀 k, score_threshold 파라미터가 전달된다."""
        manager = self._make_manager()
        doc = Document(page_content="result", metadata={})
        manager._store.similarity_search_with_relevance_scores.return_value = [
            (doc, 0.85)
        ]

        results = manager.search("query", k=3, score_threshold=0.8)

        manager._store.similarity_search_with_relevance_scores.assert_called_once_with(
            "query", k=3
        )
        assert len(results) == 1

    def test_search_filters_below_threshold(self):
        """score가 threshold 미만인 결과는 필터링된다."""
        manager = self._make_manager()
        doc_high = Document(page_content="high", metadata={})
        doc_low = Document(page_content="low", metadata={})
        manager._store.similarity_search_with_relevance_scores.return_value = [
            (doc_high, 0.9),
            (doc_low, 0.3),
        ]

        results = manager.search("query", k=5, score_threshold=0.5)

        assert len(results) == 1
        assert results[0][0].page_content == "high"

    def test_search_returns_empty(self):
        """검색 결과가 없으면 빈 리스트를 반환한다."""
        manager = self._make_manager()
        manager._store.similarity_search_with_relevance_scores.return_value = []

        with patch("app.rag.vectorstore.settings") as mock_settings:
            mock_settings.rag_top_k = 5
            mock_settings.rag_score_threshold = 0.7
            results = manager.search("no match")

        assert results == []

    def test_delete_by_source(self):
        """delete_by_source가 _collection.delete를 올바르게 호출한다."""
        manager = self._make_manager()
        mock_collection = MagicMock()
        manager._store._collection = mock_collection

        manager.delete_by_source("test.md")

        mock_collection.delete.assert_called_once_with(where={"source": "test.md"})

    def test_get_document_count(self):
        """get_document_count가 _collection.count()를 반환한다."""
        manager = self._make_manager()
        mock_collection = MagicMock()
        mock_collection.count.return_value = 42
        manager._store._collection = mock_collection

        count = manager.get_document_count()

        assert count == 42
        mock_collection.count.assert_called_once()


class TestCollectionName:
    """COLLECTION_NAME 상수 테스트."""

    def test_collection_name_constant(self):
        """COLLECTION_NAME이 'chatbot_documents'이다."""
        assert COLLECTION_NAME == "chatbot_documents"
