from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel
from langchain_ollama import ChatOllama, OllamaEmbeddings

from app.providers.base import EmbeddingProvider, LLMProvider


class OllamaLLMProvider(LLMProvider):
    def __init__(self, base_url: str, model: str = "llama3"):
        self._model = ChatOllama(base_url=base_url, model=model, temperature=0.1)

    def get_chat_model(self) -> BaseChatModel:
        return self._model


class OllamaEmbeddingProvider(EmbeddingProvider):
    def __init__(self, base_url: str, model: str = "nomic-embed-text"):
        self._embeddings = OllamaEmbeddings(base_url=base_url, model=model)

    def get_embeddings(self) -> Embeddings:
        return self._embeddings
