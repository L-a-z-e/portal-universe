"""Embedding Provider 추상화 모듈.

providers/base.py의 EmbeddingProvider를 활용하여 RAG 파이프라인에서
임베딩 생성을 추상화합니다.
"""

from langchain_core.embeddings import Embeddings

from app.providers.base import EmbeddingProvider
from app.providers.factory import create_embedding_provider


def get_embedding_function() -> Embeddings:
    """현재 설정에 맞는 Embedding 함수를 반환."""
    provider: EmbeddingProvider = create_embedding_provider()
    return provider.get_embeddings()
