from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.providers.base import EmbeddingProvider, LLMProvider


class OpenAILLMProvider(LLMProvider):
    def __init__(self, api_key: str, model: str = "gpt-4o-mini"):
        self._model = ChatOpenAI(api_key=api_key, model=model, temperature=0.1)

    def get_chat_model(self) -> BaseChatModel:
        return self._model


class OpenAIEmbeddingProvider(EmbeddingProvider):
    def __init__(self, api_key: str, model: str = "text-embedding-3-small"):
        self._embeddings = OpenAIEmbeddings(api_key=api_key, model=model)

    def get_embeddings(self) -> Embeddings:
        return self._embeddings
