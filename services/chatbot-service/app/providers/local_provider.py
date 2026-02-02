from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_core.embeddings import Embeddings

from app.providers.base import EmbeddingProvider


class LocalEmbeddingProvider(EmbeddingProvider):
    """sentence-transformers 기반 로컬 Embedding Provider."""

    def __init__(self, model: str = "all-MiniLM-L6-v2"):
        self._embeddings = HuggingFaceEmbeddings(model_name=model)

    def get_embeddings(self) -> Embeddings:
        return self._embeddings
