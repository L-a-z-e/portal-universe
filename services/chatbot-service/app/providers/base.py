from abc import ABC, abstractmethod
from collections.abc import AsyncIterator

from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage

SYSTEM_PROMPT = """당신은 도움이 되는 Q&A 어시스턴트입니다.
아래 제공된 문서 내용만을 기반으로 질문에 답변하세요.

규칙:
1. 제공된 문서에 없는 내용은 "해당 정보를 찾을 수 없습니다"라고 답변하세요.
2. 답변할 때 어떤 문서를 참고했는지 언급하세요.
3. 추측하거나 문서 외의 지식을 사용하지 마세요."""


class LLMProvider(ABC):
    """AI LLM Provider 추상 인터페이스."""

    _model: BaseChatModel

    @abstractmethod
    def get_chat_model(self) -> BaseChatModel:
        """LangChain ChatModel 인스턴스 반환."""
        ...

    def _build_messages(self, prompt: str, context: str) -> list[BaseMessage]:
        """시스템 프롬프트 + 사용자 질문 메시지 생성."""
        return [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=f"---\n[문서 컨텍스트]\n{context}\n---\n\n질문: {prompt}"),
        ]

    async def generate(self, prompt: str, context: str) -> str:
        """동기 응답 생성."""
        messages = self._build_messages(prompt, context)
        response = await self._model.ainvoke(messages)
        return str(response.content)

    async def stream(self, prompt: str, context: str) -> AsyncIterator[str]:
        """스트리밍 토큰 생성."""
        messages = self._build_messages(prompt, context)
        async for chunk in self._model.astream(messages):
            if chunk.content:
                yield str(chunk.content)


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
