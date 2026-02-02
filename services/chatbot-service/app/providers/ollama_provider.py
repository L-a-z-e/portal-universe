from collections.abc import AsyncIterator

from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_ollama import ChatOllama, OllamaEmbeddings

from app.providers.base import EmbeddingProvider, LLMProvider
from app.providers.openai_provider import SYSTEM_PROMPT


class OllamaLLMProvider(LLMProvider):
    def __init__(self, base_url: str, model: str = "llama3"):
        self._model = ChatOllama(base_url=base_url, model=model, temperature=0.1)

    def get_chat_model(self) -> BaseChatModel:
        return self._model

    async def generate(self, prompt: str, context: str) -> str:
        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=f"---\n[문서 컨텍스트]\n{context}\n---\n\n질문: {prompt}"),
        ]
        response = await self._model.ainvoke(messages)
        return str(response.content)

    async def stream(self, prompt: str, context: str) -> AsyncIterator[str]:
        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=f"---\n[문서 컨텍스트]\n{context}\n---\n\n질문: {prompt}"),
        ]
        async for chunk in self._model.astream(messages):
            if chunk.content:
                yield str(chunk.content)


class OllamaEmbeddingProvider(EmbeddingProvider):
    def __init__(self, base_url: str, model: str = "nomic-embed-text"):
        self._embeddings = OllamaEmbeddings(base_url=base_url, model=model)

    def get_embeddings(self) -> Embeddings:
        return self._embeddings
