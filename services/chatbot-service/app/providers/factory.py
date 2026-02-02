from app.core.config import settings
from app.providers.base import EmbeddingProvider, LLMProvider


class ProviderFactory:
    """설정 기반 Provider 생성."""

    @staticmethod
    def create_llm(provider: str | None = None) -> LLMProvider:
        """LLM Provider 인스턴스 생성."""
        provider = provider or settings.ai_provider
        match provider:
            case "openai":
                from app.providers.openai_provider import OpenAILLMProvider

                return OpenAILLMProvider(
                    api_key=settings.ai_api_key, model=settings.ai_model
                )
            case "anthropic":
                from app.providers.anthropic_provider import AnthropicLLMProvider

                return AnthropicLLMProvider(
                    api_key=settings.ai_api_key, model=settings.ai_model
                )
            case "google":
                from app.providers.google_provider import GoogleLLMProvider

                return GoogleLLMProvider(
                    api_key=settings.ai_api_key, model=settings.ai_model
                )
            case "ollama":
                from app.providers.ollama_provider import OllamaLLMProvider

                return OllamaLLMProvider(
                    base_url=settings.ollama_base_url, model=settings.ai_model
                )
            case _:
                raise ValueError(f"Unknown AI provider: {provider}")

    @staticmethod
    def create_embedding(provider: str | None = None) -> EmbeddingProvider:
        """Embedding Provider 인스턴스 생성."""
        provider = provider or settings.embedding_provider
        api_key = settings.embedding_api_key or settings.ai_api_key
        match provider:
            case "openai":
                from app.providers.openai_provider import OpenAIEmbeddingProvider

                return OpenAIEmbeddingProvider(
                    api_key=api_key, model=settings.embedding_model
                )
            case "sentence-transformers":
                from app.providers.local_provider import LocalEmbeddingProvider

                return LocalEmbeddingProvider(model=settings.embedding_model)
            case "ollama":
                from app.providers.ollama_provider import OllamaEmbeddingProvider

                return OllamaEmbeddingProvider(
                    base_url=settings.ollama_base_url, model=settings.embedding_model
                )
            case _:
                raise ValueError(f"Unknown embedding provider: {provider}")


# 하위 호환 함수
def create_llm_provider() -> LLMProvider:
    return ProviderFactory.create_llm()


def create_embedding_provider() -> EmbeddingProvider:
    return ProviderFactory.create_embedding()
