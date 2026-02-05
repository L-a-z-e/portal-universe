"""chunker/embeddings 단위 테스트."""

from unittest.mock import MagicMock, patch

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.rag.chunker import create_text_splitter


class TestCreateTextSplitter:
    """create_text_splitter 함수 테스트."""

    @patch("app.rag.chunker.settings")
    def test_create_text_splitter_type(self, mock_settings):
        """RecursiveCharacterTextSplitter 인스턴스를 반환한다."""
        mock_settings.rag_chunk_size = 1000
        mock_settings.rag_chunk_overlap = 200

        splitter = create_text_splitter()

        assert isinstance(splitter, RecursiveCharacterTextSplitter)

    @patch("app.rag.chunker.settings")
    def test_chunk_size_from_settings(self, mock_settings):
        """chunk_size가 settings 값과 일치한다."""
        mock_settings.rag_chunk_size = 500
        mock_settings.rag_chunk_overlap = 100

        splitter = create_text_splitter()

        assert splitter._chunk_size == 500

    @patch("app.rag.chunker.settings")
    def test_chunk_overlap_from_settings(self, mock_settings):
        """chunk_overlap이 settings 값과 일치한다."""
        mock_settings.rag_chunk_size = 1000
        mock_settings.rag_chunk_overlap = 150

        splitter = create_text_splitter()

        assert splitter._chunk_overlap == 150

    @patch("app.rag.chunker.settings")
    def test_separators_order(self, mock_settings):
        """구분자 순서가 [\\n\\n, \\n, . , ' ', '']이다."""
        mock_settings.rag_chunk_size = 1000
        mock_settings.rag_chunk_overlap = 200

        splitter = create_text_splitter()

        assert splitter._separators == ["\n\n", "\n", ". ", " ", ""]


class TestGetEmbeddingFunction:
    """get_embedding_function 함수 테스트."""

    def test_embeddings_function(self):
        """get_embedding_function이 create_embedding_provider를 호출한다."""
        mock_provider = MagicMock()
        mock_embeddings = MagicMock()
        mock_provider.get_embeddings.return_value = mock_embeddings

        with patch(
            "app.rag.embeddings.create_embedding_provider",
            return_value=mock_provider,
        ):
            from app.rag.embeddings import get_embedding_function

            result = get_embedding_function()

        mock_provider.get_embeddings.assert_called_once()
        assert result is mock_embeddings
