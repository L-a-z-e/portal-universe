from unittest.mock import patch

import pytest

from app.providers.factory import ProviderFactory, create_embedding_provider, create_llm_provider


@pytest.mark.asyncio
async def test_create_openai_provider():
    """OpenAI provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "openai"
        mock_settings.ai_api_key = "sk-test"
        mock_settings.ai_model = "gpt-4o-mini"

        provider = create_llm_provider()

        from app.providers.openai_provider import OpenAILLMProvider

        assert isinstance(provider, OpenAILLMProvider)


@pytest.mark.asyncio
async def test_create_anthropic_provider():
    """Anthropic provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "anthropic"
        mock_settings.ai_api_key = "sk-ant-test"
        mock_settings.ai_model = "claude-3-5-sonnet-20241022"

        provider = create_llm_provider()

        from app.providers.anthropic_provider import AnthropicLLMProvider

        assert isinstance(provider, AnthropicLLMProvider)


@pytest.mark.asyncio
async def test_create_ollama_provider():
    """Ollama provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "ollama"
        mock_settings.ollama_base_url = "http://localhost:11434"
        mock_settings.ai_model = "llama3"

        provider = create_llm_provider()

        from app.providers.ollama_provider import OllamaLLMProvider

        assert isinstance(provider, OllamaLLMProvider)


@pytest.mark.asyncio
async def test_create_google_provider():
    """Google provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "google"
        mock_settings.ai_api_key = "test-key"
        mock_settings.ai_model = "gemini-pro"

        provider = create_llm_provider()

        from app.providers.google_provider import GoogleLLMProvider

        assert isinstance(provider, GoogleLLMProvider)


@pytest.mark.asyncio
async def test_create_unknown_provider_raises():
    """알 수 없는 provider 생성 시 ValueError."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "unknown"

        with pytest.raises(ValueError, match="Unknown AI provider"):
            create_llm_provider()


@pytest.mark.asyncio
async def test_create_openai_embedding():
    """OpenAI embedding provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "openai"
        mock_settings.ai_api_key = "sk-test"
        mock_settings.embedding_model = "text-embedding-3-small"

        provider = create_embedding_provider()

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        assert isinstance(provider, OpenAIEmbeddingProvider)


@pytest.mark.asyncio
async def test_create_ollama_embedding():
    """Ollama embedding provider 생성 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "ollama"
        mock_settings.ollama_base_url = "http://localhost:11434"
        mock_settings.embedding_model = "nomic-embed-text"

        provider = create_embedding_provider()

        from app.providers.ollama_provider import OllamaEmbeddingProvider

        assert isinstance(provider, OllamaEmbeddingProvider)


@pytest.mark.asyncio
async def test_create_unknown_embedding_raises():
    """알 수 없는 embedding provider 생성 시 ValueError."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "unknown"

        with pytest.raises(ValueError, match="Unknown embedding provider"):
            create_embedding_provider()


@pytest.mark.asyncio
async def test_provider_factory_class_create_llm():
    """ProviderFactory.create_llm() 클래스 메서드 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "openai"
        mock_settings.ai_api_key = "sk-test"
        mock_settings.ai_model = "gpt-4o-mini"

        provider = ProviderFactory.create_llm()

        from app.providers.openai_provider import OpenAILLMProvider

        assert isinstance(provider, OpenAILLMProvider)


@pytest.mark.asyncio
async def test_provider_factory_class_create_embedding():
    """ProviderFactory.create_embedding() 클래스 메서드 확인."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "openai"
        mock_settings.embedding_api_key = ""
        mock_settings.ai_api_key = "sk-test"
        mock_settings.embedding_model = "text-embedding-3-small"

        provider = ProviderFactory.create_embedding()

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        assert isinstance(provider, OpenAIEmbeddingProvider)


@pytest.mark.asyncio
async def test_create_sentence_transformers_embedding():
    """sentence-transformers embedding provider 생성 확인."""
    with (
        patch("app.providers.factory.settings") as mock_settings,
        patch(
            "app.providers.local_provider.HuggingFaceEmbeddings"
        ) as mock_hf,
    ):
        mock_settings.embedding_provider = "sentence-transformers"
        mock_settings.embedding_api_key = ""
        mock_settings.ai_api_key = ""
        mock_settings.embedding_model = "all-MiniLM-L6-v2"

        # HuggingFaceEmbeddings import를 모킹하여 sentence-transformers 미설치 환경에서도 테스트
        provider = ProviderFactory.create_embedding()

        from app.providers.local_provider import LocalEmbeddingProvider

        assert isinstance(provider, LocalEmbeddingProvider)
        mock_hf.assert_called_once_with(model_name="all-MiniLM-L6-v2")


@pytest.mark.asyncio
async def test_embedding_api_key_fallback():
    """embedding_api_key가 없으면 ai_api_key를 사용."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "openai"
        mock_settings.embedding_api_key = "embed-specific-key"
        mock_settings.ai_api_key = "general-key"
        mock_settings.embedding_model = "text-embedding-3-small"

        provider = ProviderFactory.create_embedding()

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        assert isinstance(provider, OpenAIEmbeddingProvider)
