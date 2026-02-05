"""Conversation/Message 모델 단위 테스트."""

from datetime import datetime, timezone

from app.models.conversation import Conversation, Message


class TestMessage:
    """Message 모델 테스트."""

    def test_message_creation(self):
        """기본 Message 생성 확인."""
        msg = Message(message_id="msg-1", role="user", content="hello")
        assert msg.message_id == "msg-1"
        assert msg.role == "user"
        assert msg.content == "hello"

    def test_message_default_created_at(self):
        """created_at 기본값이 자동으로 UTC datetime으로 설정된다."""
        msg = Message(message_id="msg-1", role="user", content="test")
        assert isinstance(msg.created_at, datetime)
        assert msg.created_at.tzinfo is not None

    def test_message_sources_optional(self):
        """sources 기본값은 None이다."""
        msg = Message(message_id="msg-1", role="assistant", content="reply")
        assert msg.sources is None

        msg_with_sources = Message(
            message_id="msg-2",
            role="assistant",
            content="reply",
            sources=[{"document": "test.md", "chunk": "text"}],
        )
        assert len(msg_with_sources.sources) == 1


class TestConversation:
    """Conversation 모델 테스트."""

    def test_conversation_creation(self):
        """기본 Conversation 생성 확인."""
        conv = Conversation(
            conversation_id="conv-1",
            user_id="user-1",
        )
        assert conv.conversation_id == "conv-1"
        assert conv.user_id == "user-1"

    def test_conversation_default_title(self):
        """title 기본값은 'New conversation'이다."""
        conv = Conversation(conversation_id="conv-1", user_id="user-1")
        assert conv.title == "New conversation"

    def test_conversation_default_empty_messages(self):
        """messages 기본값은 빈 리스트이다."""
        conv = Conversation(conversation_id="conv-1", user_id="user-1")
        assert conv.messages == []
        assert conv.message_count == 0

    def test_conversation_timestamps_utc(self):
        """created_at, updated_at이 UTC timezone으로 설정된다."""
        conv = Conversation(conversation_id="conv-1", user_id="user-1")
        assert isinstance(conv.created_at, datetime)
        assert isinstance(conv.updated_at, datetime)
        assert conv.created_at.tzinfo == timezone.utc
        assert conv.updated_at.tzinfo == timezone.utc
