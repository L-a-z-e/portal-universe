from pydantic import field_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Service
    service_port: int = 8086
    log_level: str = "INFO"

    # AI Provider
    ai_provider: str = "ollama"
    ai_model: str = "llama3"
    ai_api_key: str = ""

    # Embedding Provider
    embedding_provider: str = "ollama"
    embedding_model: str = "nomic-embed-text"
    embedding_api_key: str = ""  # AI_API_KEY와 다를 경우

    # Ollama
    ollama_base_url: str = "http://localhost:11434"

    # Vector DB
    vector_db_type: str = "chroma"  # chroma | elasticsearch
    chroma_persist_dir: str = "./data/chroma"

    # Redis
    redis_url: str = "redis://localhost:6379/1"

    # CORS (Gateway 경유 시 false, 독립 실행 시 true)
    cors_enabled: bool = True
    cors_origins: list[str] = ["http://localhost:30000"]

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, v: object) -> list[str]:
        if isinstance(v, str):
            return [origin.strip() for origin in v.split(",")]
        return v  # type: ignore[return-value]

    # Observability (ADR-033)
    tracing_enabled: bool = False
    zipkin_endpoint: str = "http://localhost:9411/api/v2/spans"

    # Documents
    documents_dir: str = "./documents"

    # RAG
    rag_chunk_size: int = 1000
    rag_chunk_overlap: int = 200
    rag_top_k: int = 5
    rag_score_threshold: float = 0.7

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
