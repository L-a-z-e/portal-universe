from collections.abc import AsyncIterator

from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.providers.base import EmbeddingProvider, LLMProvider

SYSTEM_PROMPT = """당신은 도움이 되는 Q&A 어시스턴트입니다.
아래 제공된 문서 내용만을 기반으로 질문에 답변하세요.

규칙:
1. 제공된 문서에 없는 내용은 "해당 정보를 찾을 수 없습니다"라고 답변하세요.
2. 답변할 때 어떤 문서를 참고했는지 언급하세요.
3. 추측하거나 문서 외의 지식을 사용하지 마세요."""


class OpenAILLMProvider(LLMProvider):
    def __init__(self, api_key: str, model: str = "gpt-4o-mini"):
        self._model = ChatOpenAI(api_key=api_key, model=model, temperature=0.1)

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


class OpenAIEmbeddingProvider(EmbeddingProvider):
    def __init__(self, api_key: str, model: str = "text-embedding-3-small"):
        self._embeddings = OpenAIEmbeddings(api_key=api_key, model=model)

    def get_embeddings(self) -> Embeddings:
        return self._embeddings
