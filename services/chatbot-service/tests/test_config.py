"""Settings 설정값 단위 테스트."""

from app.core.config import Settings


class TestSettings:
    """Settings 기본값 테스트.

    model_fields를 통해 클래스 정의 기본값을 직접 확인하여
    .env 파일의 간섭을 방지한다.
    """

    def test_settings_default_values(self):
        """service_port=8086, log_level='INFO' 기본값 확인."""
        assert Settings.model_fields["service_port"].default == 8086
        assert Settings.model_fields["log_level"].default == "INFO"

    def test_settings_ai_defaults(self):
        """ai_provider='ollama', ai_model='llama3' 기본값 확인."""
        assert Settings.model_fields["ai_provider"].default == "ollama"
        assert Settings.model_fields["ai_model"].default == "llama3"

    def test_settings_rag_defaults(self):
        """RAG 관련 기본값: chunk_size=1000, overlap=200, top_k=5, threshold=0.7."""
        assert Settings.model_fields["rag_chunk_size"].default == 1000
        assert Settings.model_fields["rag_chunk_overlap"].default == 200
        assert Settings.model_fields["rag_top_k"].default == 5
        assert Settings.model_fields["rag_score_threshold"].default == 0.7

    def test_settings_redis_default(self):
        """redis_url 기본값 확인."""
        assert Settings.model_fields["redis_url"].default == "redis://localhost:6379/1"

    def test_settings_cors_defaults(self):
        """cors_enabled=True, cors_origins에 localhost:30000 포함."""
        assert Settings.model_fields["cors_enabled"].default is True
        default_origins = Settings.model_fields["cors_origins"].default
        assert "http://localhost:30000" in default_origins

    def test_settings_documents_dir(self):
        """documents_dir 기본값은 './documents'이다."""
        assert Settings.model_fields["documents_dir"].default == "./documents"
