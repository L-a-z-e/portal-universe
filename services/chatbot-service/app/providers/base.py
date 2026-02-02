from abc import ABC, abstractmethod
from collections.abc import AsyncIterator

from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel


class LLMProvider(ABC):
    """AI LLM Provider 추상 인터페이스."""

    @abstractmethod
    def get_chat_model(self) -> BaseChatModel:
        """LangChain ChatModel 인스턴스 반환."""
        ...

    @abstractmethod
    async def generate(self, prompt: str, context: str) -> str:
        """동기 응답 생성."""
        ...

    @abstractmethod
    async def stream(self, prompt: str, context: str) -> AsyncIterator[str]:
        """스트리밍 토큰 생성."""
        ...


class EmbeddingProvider(ABC):
    """Embedding Provider 추상 인터페이스."""

    @abstractmethod
    def get_embeddings(self) -> Embeddings:
        """LangChain Embeddings 인스턴스 반환."""
        ...

    async def embed_text(self, text: str) -> list[float]:
        """단일 텍스트 임베딩 생성."""
        return await self.get_embeddings().aembed_query(text)

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """배치 텍스트 임베딩 생성."""
        return await self.get_embeddings().aembed_documents(texts)
