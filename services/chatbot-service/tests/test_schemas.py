"""Pydantic schema 단위 테스트."""

import pytest
from pydantic import ValidationError

from app.schemas.chat import (
    ChatRequest,
    ChatResponse,
    ConversationMessage,
    ConversationSummary,
    SourceInfo,
)
from app.schemas.common import ApiResponse, ErrorDetail
from app.schemas.document import DocumentInfo, DocumentList


class TestApiResponse:
    """ApiResponse 스키마 테스트."""

    def test_api_response_ok_with_data(self):
        """ApiResponse.ok(data)는 success=True, data에 값이 설정된다."""
        resp = ApiResponse.ok({"key": "value"})
        assert resp.success is True
        assert resp.data == {"key": "value"}
        assert resp.error is None

    def test_api_response_ok_without_data(self):
        """ApiResponse.ok()는 success=True, data=None이다."""
        resp = ApiResponse.ok()
        assert resp.success is True
        assert resp.data is None
        assert resp.error is None

    def test_api_response_fail(self):
        """ApiResponse.fail()은 success=False, error에 코드/메시지가 설정된다."""
        resp = ApiResponse.fail("E001", "Something went wrong")
        assert resp.success is False
        assert resp.data is None
        assert resp.error is not None
        assert resp.error.code == "E001"
        assert resp.error.message == "Something went wrong"

    def test_api_response_generic_type(self):
        """ApiResponse에 dict 타입 data를 설정할 수 있다."""
        resp = ApiResponse(success=True, data={"nested": {"a": 1}})
        assert resp.success is True
        assert resp.data["nested"]["a"] == 1


class TestErrorDetail:
    """ErrorDetail 스키마 테스트."""

    def test_error_detail_fields(self):
        """ErrorDetail에 code, message 필드가 정상 설정된다."""
        detail = ErrorDetail(code="E001", message="test error")
        assert detail.code == "E001"
        assert detail.message == "test error"


class TestChatRequest:
    """ChatRequest 스키마 테스트."""

    def test_chat_request_valid(self):
        """유효한 message로 ChatRequest 생성."""
        req = ChatRequest(message="hello")
        assert req.message == "hello"

    def test_chat_request_with_conversation_id(self):
        """conversation_id를 포함한 ChatRequest 생성."""
        req = ChatRequest(message="hi", conversation_id="c1")
        assert req.message == "hi"
        assert req.conversation_id == "c1"

    def test_chat_request_default_conversation_id(self):
        """conversation_id 기본값은 None이다."""
        req = ChatRequest(message="test")
        assert req.conversation_id is None

    def test_chat_request_missing_message(self):
        """message 없이 생성 시 ValidationError가 발생한다."""
        with pytest.raises(ValidationError):
            ChatRequest()


class TestSourceInfo:
    """SourceInfo 스키마 테스트."""

    def test_source_info_fields(self):
        """SourceInfo 필드가 정상 설정된다."""
        info = SourceInfo(document="readme.md", chunk="some text", relevance_score=0.9)
        assert info.document == "readme.md"
        assert info.chunk == "some text"
        assert info.relevance_score == 0.9

    def test_source_info_relevance_score_float(self):
        """relevance_score는 float 타입이다."""
        info = SourceInfo(document="a", chunk="b", relevance_score=0.85)
        assert isinstance(info.relevance_score, float)


class TestChatResponse:
    """ChatResponse 스키마 테스트."""

    def test_chat_response_fields(self):
        """ChatResponse 모든 필드가 정상 설정된다."""
        source = SourceInfo(document="doc.md", chunk="content", relevance_score=0.95)
        resp = ChatResponse(
            answer="답변입니다",
            sources=[source],
            conversation_id="conv-1",
            message_id="msg-1",
        )
        assert resp.answer == "답변입니다"
        assert len(resp.sources) == 1
        assert resp.sources[0].document == "doc.md"
        assert resp.conversation_id == "conv-1"
        assert resp.message_id == "msg-1"

    def test_chat_response_empty_sources(self):
        """sources가 빈 리스트여도 유효하다."""
        resp = ChatResponse(
            answer="no sources",
            sources=[],
            conversation_id="conv-1",
            message_id="msg-1",
        )
        assert resp.sources == []


class TestConversationSummary:
    """ConversationSummary 스키마 테스트."""

    def test_conversation_summary_fields(self):
        """ConversationSummary 모든 필드가 정상 설정된다."""
        summary = ConversationSummary(
            conversation_id="conv-1",
            title="Test Conversation",
            message_count=5,
            created_at="2026-01-01T00:00:00Z",
            updated_at="2026-01-01T01:00:00Z",
        )
        assert summary.conversation_id == "conv-1"
        assert summary.title == "Test Conversation"
        assert summary.message_count == 5
        assert summary.created_at == "2026-01-01T00:00:00Z"
        assert summary.updated_at == "2026-01-01T01:00:00Z"


class TestConversationMessage:
    """ConversationMessage 스키마 테스트."""

    def test_conversation_message_fields(self):
        """ConversationMessage 모든 필드가 정상 설정된다."""
        source = SourceInfo(document="doc.md", chunk="text", relevance_score=0.8)
        msg = ConversationMessage(
            message_id="msg-1",
            role="assistant",
            content="안녕하세요",
            sources=[source],
            created_at="2026-01-01T00:00:00Z",
        )
        assert msg.message_id == "msg-1"
        assert msg.role == "assistant"
        assert msg.content == "안녕하세요"
        assert len(msg.sources) == 1
        assert msg.created_at == "2026-01-01T00:00:00Z"

    def test_conversation_message_sources_optional(self):
        """sources는 None이 허용된다."""
        msg = ConversationMessage(
            message_id="msg-2",
            role="user",
            content="질문",
            created_at="2026-01-01T00:00:00Z",
        )
        assert msg.sources is None


class TestDocumentSchemas:
    """Document 관련 스키마 테스트."""

    def test_document_info_fields(self):
        """DocumentInfo 모든 필드가 정상 설정된다."""
        doc = DocumentInfo(
            document_id="doc-1",
            filename="readme.md",
            chunks=10,
            status="indexed",
        )
        assert doc.document_id == "doc-1"
        assert doc.filename == "readme.md"
        assert doc.chunks == 10
        assert doc.status == "indexed"

    def test_document_list_fields(self):
        """DocumentList에 documents 리스트와 total이 설정된다."""
        doc1 = DocumentInfo(document_id="d1", filename="a.md", chunks=5, status="indexed")
        doc2 = DocumentInfo(document_id="d2", filename="b.pdf", chunks=12, status="processing")
        doc_list = DocumentList(documents=[doc1, doc2], total=2)
        assert len(doc_list.documents) == 2
        assert doc_list.total == 2
        assert doc_list.documents[0].filename == "a.md"
        assert doc_list.documents[1].status == "processing"
