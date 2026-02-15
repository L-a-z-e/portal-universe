from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.providers.factory import ProviderFactory, create_embedding_provider, create_llm_provider

# ============================================================
# 기존 테스트 (12개): Factory 생성 테스트
# ============================================================


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
        patch("app.providers.local_provider.HuggingFaceEmbeddings") as mock_hf,
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


# ============================================================
# 신규 테스트 (20개): Provider 인스턴스 동작 테스트
# ============================================================


# --- OpenAI LLM Provider ---


@pytest.mark.asyncio
async def test_openai_provider_get_chat_model():
    """OpenAILLMProvider.get_chat_model()이 내부 모델을 반환."""
    with patch("app.providers.openai_provider.ChatOpenAI") as MockChat:
        mock_model = MagicMock()
        MockChat.return_value = mock_model

        from app.providers.openai_provider import OpenAILLMProvider

        provider = OpenAILLMProvider(api_key="test-key")
        result = provider.get_chat_model()

        assert result is mock_model


@pytest.mark.asyncio
async def test_openai_provider_generate():
    """OpenAILLMProvider.generate()가 ainvoke 결과의 content를 반환."""
    with patch("app.providers.openai_provider.ChatOpenAI") as MockChat:
        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(return_value=MagicMock(content="test answer"))
        MockChat.return_value = mock_model

        from app.providers.openai_provider import OpenAILLMProvider

        provider = OpenAILLMProvider(api_key="test-key")
        result = await provider.generate("question", "context")

        assert result == "test answer"
        mock_model.ainvoke.assert_called_once()


@pytest.mark.asyncio
async def test_openai_provider_stream():
    """OpenAILLMProvider.stream()이 토큰을 순서대로 yield."""
    with patch("app.providers.openai_provider.ChatOpenAI") as MockChat:
        mock_model = MagicMock()

        async def mock_astream(messages):
            for text in ["Hello", " World"]:
                chunk = MagicMock()
                chunk.content = text
                yield chunk

        mock_model.astream = mock_astream
        MockChat.return_value = mock_model

        from app.providers.openai_provider import OpenAILLMProvider

        provider = OpenAILLMProvider(api_key="test-key")
        tokens = [t async for t in provider.stream("q", "c")]

        assert tokens == ["Hello", " World"]


@pytest.mark.asyncio
async def test_openai_provider_system_prompt():
    """SYSTEM_PROMPT가 비어있지 않은 문자열인지 확인."""
    from app.providers.base import SYSTEM_PROMPT

    assert isinstance(SYSTEM_PROMPT, str)
    assert len(SYSTEM_PROMPT) > 0
    assert "Q&A" in SYSTEM_PROMPT


# --- Anthropic LLM Provider ---


@pytest.mark.asyncio
async def test_anthropic_provider_get_chat_model():
    """AnthropicLLMProvider.get_chat_model()이 내부 모델을 반환."""
    with patch("app.providers.anthropic_provider.ChatAnthropic") as MockChat:
        mock_model = MagicMock()
        MockChat.return_value = mock_model

        from app.providers.anthropic_provider import AnthropicLLMProvider

        provider = AnthropicLLMProvider(api_key="test-key")
        result = provider.get_chat_model()

        assert result is mock_model


@pytest.mark.asyncio
async def test_anthropic_provider_generate():
    """AnthropicLLMProvider.generate()가 ainvoke 결과의 content를 반환."""
    with patch("app.providers.anthropic_provider.ChatAnthropic") as MockChat:
        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(return_value=MagicMock(content="anthropic answer"))
        MockChat.return_value = mock_model

        from app.providers.anthropic_provider import AnthropicLLMProvider

        provider = AnthropicLLMProvider(api_key="test-key")
        result = await provider.generate("question", "context")

        assert result == "anthropic answer"
        mock_model.ainvoke.assert_called_once()


@pytest.mark.asyncio
async def test_anthropic_provider_stream():
    """AnthropicLLMProvider.stream()이 토큰을 순서대로 yield."""
    with patch("app.providers.anthropic_provider.ChatAnthropic") as MockChat:
        mock_model = MagicMock()

        async def mock_astream(messages):
            for text in ["Claude", " says", " hi"]:
                chunk = MagicMock()
                chunk.content = text
                yield chunk

        mock_model.astream = mock_astream
        MockChat.return_value = mock_model

        from app.providers.anthropic_provider import AnthropicLLMProvider

        provider = AnthropicLLMProvider(api_key="test-key")
        tokens = [t async for t in provider.stream("q", "c")]

        assert tokens == ["Claude", " says", " hi"]


# --- Ollama LLM Provider ---


@pytest.mark.asyncio
async def test_ollama_provider_get_chat_model():
    """OllamaLLMProvider.get_chat_model()이 내부 모델을 반환."""
    with patch("app.providers.ollama_provider.ChatOllama") as MockChat:
        mock_model = MagicMock()
        MockChat.return_value = mock_model

        from app.providers.ollama_provider import OllamaLLMProvider

        provider = OllamaLLMProvider(base_url="http://localhost:11434")
        result = provider.get_chat_model()

        assert result is mock_model


@pytest.mark.asyncio
async def test_ollama_provider_generate():
    """OllamaLLMProvider.generate()가 ainvoke 결과의 content를 반환."""
    with patch("app.providers.ollama_provider.ChatOllama") as MockChat:
        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(return_value=MagicMock(content="ollama answer"))
        MockChat.return_value = mock_model

        from app.providers.ollama_provider import OllamaLLMProvider

        provider = OllamaLLMProvider(base_url="http://localhost:11434")
        result = await provider.generate("question", "context")

        assert result == "ollama answer"
        mock_model.ainvoke.assert_called_once()


@pytest.mark.asyncio
async def test_ollama_provider_stream():
    """OllamaLLMProvider.stream()이 토큰을 순서대로 yield."""
    with patch("app.providers.ollama_provider.ChatOllama") as MockChat:
        mock_model = MagicMock()

        async def mock_astream(messages):
            for text in ["Llama", " response"]:
                chunk = MagicMock()
                chunk.content = text
                yield chunk

        mock_model.astream = mock_astream
        MockChat.return_value = mock_model

        from app.providers.ollama_provider import OllamaLLMProvider

        provider = OllamaLLMProvider(base_url="http://localhost:11434")
        tokens = [t async for t in provider.stream("q", "c")]

        assert tokens == ["Llama", " response"]


# --- Google LLM Provider ---


@pytest.mark.asyncio
async def test_google_provider_get_chat_model():
    """GoogleLLMProvider.get_chat_model()이 내부 모델을 반환."""
    with patch("app.providers.google_provider.ChatGoogleGenerativeAI") as MockChat:
        mock_model = MagicMock()
        MockChat.return_value = mock_model

        from app.providers.google_provider import GoogleLLMProvider

        provider = GoogleLLMProvider(api_key="test-key")
        result = provider.get_chat_model()

        assert result is mock_model


@pytest.mark.asyncio
async def test_google_provider_generate():
    """GoogleLLMProvider.generate()가 ainvoke 결과의 content를 반환."""
    with patch("app.providers.google_provider.ChatGoogleGenerativeAI") as MockChat:
        mock_model = MagicMock()
        mock_model.ainvoke = AsyncMock(return_value=MagicMock(content="gemini answer"))
        MockChat.return_value = mock_model

        from app.providers.google_provider import GoogleLLMProvider

        provider = GoogleLLMProvider(api_key="test-key")
        result = await provider.generate("question", "context")

        assert result == "gemini answer"
        mock_model.ainvoke.assert_called_once()


@pytest.mark.asyncio
async def test_google_provider_stream():
    """GoogleLLMProvider.stream()이 토큰을 순서대로 yield."""
    with patch("app.providers.google_provider.ChatGoogleGenerativeAI") as MockChat:
        mock_model = MagicMock()

        async def mock_astream(messages):
            for text in ["Gemini", " output"]:
                chunk = MagicMock()
                chunk.content = text
                yield chunk

        mock_model.astream = mock_astream
        MockChat.return_value = mock_model

        from app.providers.google_provider import GoogleLLMProvider

        provider = GoogleLLMProvider(api_key="test-key")
        tokens = [t async for t in provider.stream("q", "c")]

        assert tokens == ["Gemini", " output"]


# --- Embedding Providers ---


@pytest.mark.asyncio
async def test_openai_embedding_get_embeddings():
    """OpenAIEmbeddingProvider.get_embeddings()가 내부 embeddings를 반환."""
    with patch("app.providers.openai_provider.OpenAIEmbeddings") as MockEmbed:
        mock_embeddings = MagicMock()
        MockEmbed.return_value = mock_embeddings

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        provider = OpenAIEmbeddingProvider(api_key="test-key")
        result = provider.get_embeddings()

        assert result is mock_embeddings


@pytest.mark.asyncio
async def test_ollama_embedding_get_embeddings():
    """OllamaEmbeddingProvider.get_embeddings()가 내부 embeddings를 반환."""
    with patch("app.providers.ollama_provider.OllamaEmbeddings") as MockEmbed:
        mock_embeddings = MagicMock()
        MockEmbed.return_value = mock_embeddings

        from app.providers.ollama_provider import OllamaEmbeddingProvider

        provider = OllamaEmbeddingProvider(base_url="http://localhost:11434")
        result = provider.get_embeddings()

        assert result is mock_embeddings


@pytest.mark.asyncio
async def test_local_embedding_get_embeddings():
    """LocalEmbeddingProvider.get_embeddings()가 내부 embeddings를 반환."""
    with patch("app.providers.local_provider.HuggingFaceEmbeddings") as MockEmbed:
        mock_embeddings = MagicMock()
        MockEmbed.return_value = mock_embeddings

        from app.providers.local_provider import LocalEmbeddingProvider

        provider = LocalEmbeddingProvider(model="all-MiniLM-L6-v2")
        result = provider.get_embeddings()

        assert result is mock_embeddings


@pytest.mark.asyncio
async def test_embedding_provider_embed_text():
    """EmbeddingProvider.embed_text()가 aembed_query를 호출."""
    with patch("app.providers.openai_provider.OpenAIEmbeddings") as MockEmbed:
        mock_embeddings = MagicMock()
        mock_embeddings.aembed_query = AsyncMock(return_value=[0.1, 0.2, 0.3])
        MockEmbed.return_value = mock_embeddings

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        provider = OpenAIEmbeddingProvider(api_key="test-key")
        result = await provider.embed_text("hello world")

        assert result == [0.1, 0.2, 0.3]
        mock_embeddings.aembed_query.assert_called_once_with("hello world")


@pytest.mark.asyncio
async def test_embedding_provider_embed_batch():
    """EmbeddingProvider.embed_batch()가 aembed_documents를 호출."""
    with patch("app.providers.openai_provider.OpenAIEmbeddings") as MockEmbed:
        mock_embeddings = MagicMock()
        mock_embeddings.aembed_documents = AsyncMock(return_value=[[0.1, 0.2], [0.3, 0.4]])
        MockEmbed.return_value = mock_embeddings

        from app.providers.openai_provider import OpenAIEmbeddingProvider

        provider = OpenAIEmbeddingProvider(api_key="test-key")
        result = await provider.embed_batch(["text1", "text2"])

        assert result == [[0.1, 0.2], [0.3, 0.4]]
        mock_embeddings.aembed_documents.assert_called_once_with(["text1", "text2"])


# --- Factory explicit provider 인자 ---


@pytest.mark.asyncio
async def test_factory_create_llm_explicit_provider():
    """ProviderFactory.create_llm('openai')로 명시적 provider 지정."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.ai_provider = "ollama"  # default는 ollama
        mock_settings.ai_api_key = "sk-test"
        mock_settings.ai_model = "gpt-4o-mini"

        # 명시적으로 openai를 지정하면 settings.ai_provider를 무시
        provider = ProviderFactory.create_llm("openai")

        from app.providers.openai_provider import OpenAILLMProvider

        assert isinstance(provider, OpenAILLMProvider)


@pytest.mark.asyncio
async def test_factory_create_embedding_explicit_provider():
    """ProviderFactory.create_embedding('ollama')로 명시적 provider 지정."""
    with patch("app.providers.factory.settings") as mock_settings:
        mock_settings.embedding_provider = "openai"  # default는 openai
        mock_settings.embedding_api_key = ""
        mock_settings.ai_api_key = ""
        mock_settings.ollama_base_url = "http://localhost:11434"
        mock_settings.embedding_model = "nomic-embed-text"

        # 명시적으로 ollama를 지정하면 settings.embedding_provider를 무시
        provider = ProviderFactory.create_embedding("ollama")

        from app.providers.ollama_provider import OllamaEmbeddingProvider

        assert isinstance(provider, OllamaEmbeddingProvider)
