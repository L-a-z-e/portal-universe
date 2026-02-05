import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.services.conversation_service import ConversationService


@pytest.fixture
def mock_redis():
    """Mock Redis 클라이언트."""
    return AsyncMock()


@pytest.fixture
def service(mock_redis):
    """Redis를 직접 주입한 ConversationService."""
    svc = ConversationService()
    svc._redis = mock_redis  # bypass lazy init
    return svc


# ============================================================
# Redis 초기화 테스트
# ============================================================


@pytest.mark.asyncio
async def test_get_redis_lazy_init():
    """첫 호출 시 redis.from_url로 커넥션 생성."""
    svc = ConversationService()
    assert svc._redis is None

    with patch("app.services.conversation_service.redis") as mock_redis_module:
        mock_conn = AsyncMock()
        mock_redis_module.from_url.return_value = mock_conn

        result = await svc._get_redis()

        assert result is mock_conn
        assert svc._redis is mock_conn
        mock_redis_module.from_url.assert_called_once()


@pytest.mark.asyncio
async def test_get_redis_cached():
    """두 번째 호출 시 기존 커넥션 재사용."""
    svc = ConversationService()
    mock_conn = AsyncMock()
    svc._redis = mock_conn

    with patch("app.services.conversation_service.redis") as mock_redis_module:
        result = await svc._get_redis()

        assert result is mock_conn
        mock_redis_module.from_url.assert_not_called()


# ============================================================
# 키 포맷 테스트
# ============================================================


def test_conv_key_format():
    """_conv_key가 올바른 형식의 키를 반환."""
    svc = ConversationService()
    assert svc._conv_key("user1") == "chatbot:conversations:user1"


def test_msg_key_format():
    """_msg_key가 올바른 형식의 키를 반환."""
    svc = ConversationService()
    assert svc._msg_key("u1", "c1") == "chatbot:messages:u1:c1"


# ============================================================
# save_message 테스트
# ============================================================


@pytest.mark.asyncio
async def test_save_message_new_conversation(service, mock_redis):
    """새 대화에 메시지 저장 시 대화 메타데이터 생성."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="user",
        content="Hello chatbot",
    )

    # rpush로 메시지 저장
    mock_redis.rpush.assert_called_once()
    call_args = mock_redis.rpush.call_args
    assert call_args[0][0] == "chatbot:messages:user1:conv1"

    # hset으로 대화 메타데이터 저장
    mock_redis.hset.assert_called_once()
    hset_args = mock_redis.hset.call_args
    conv_data = json.loads(hset_args[0][2])
    assert conv_data["conversation_id"] == "conv1"
    assert conv_data["message_count"] == 1


@pytest.mark.asyncio
async def test_save_message_existing_conversation(service, mock_redis):
    """기존 대화에 메시지 추가 시 message_count 증가."""
    existing_conv = json.dumps({
        "conversation_id": "conv1",
        "title": "기존 대화",
        "message_count": 3,
        "created_at": "2026-01-01T00:00:00+00:00",
        "updated_at": "2026-01-01T00:00:00+00:00",
    })
    mock_redis.hget = AsyncMock(return_value=existing_conv)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg4",
        role="assistant",
        content="답변입니다.",
    )

    hset_args = mock_redis.hset.call_args
    conv_data = json.loads(hset_args[0][2])
    assert conv_data["message_count"] == 4


@pytest.mark.asyncio
async def test_save_message_user_role_title(service, mock_redis):
    """role='user'일 때 title은 content[:50]."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    long_content = "A" * 100
    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="user",
        content=long_content,
    )

    hset_args = mock_redis.hset.call_args
    conv_data = json.loads(hset_args[0][2])
    assert conv_data["title"] == "A" * 50


@pytest.mark.asyncio
async def test_save_message_assistant_role_title(service, mock_redis):
    """role='assistant'일 때 title은 'New conversation'."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="assistant",
        content="도움이 필요하신가요?",
    )

    hset_args = mock_redis.hset.call_args
    conv_data = json.loads(hset_args[0][2])
    assert conv_data["title"] == "New conversation"


@pytest.mark.asyncio
async def test_save_message_with_sources(service, mock_redis):
    """sources가 있을 때 model_dump()로 직렬화."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    from app.schemas.chat import SourceInfo

    sources = [
        SourceInfo(document="doc.md", chunk="some text", relevance_score=0.95)
    ]

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="assistant",
        content="answer",
        sources=sources,
    )

    rpush_args = mock_redis.rpush.call_args
    msg_data = json.loads(rpush_args[0][1])
    assert msg_data["sources"] is not None
    assert len(msg_data["sources"]) == 1
    assert msg_data["sources"][0]["document"] == "doc.md"
    assert msg_data["sources"][0]["relevance_score"] == 0.95


@pytest.mark.asyncio
async def test_save_message_sets_ttl(service, mock_redis):
    """메시지 저장 후 두 키 모두 7일 TTL 설정."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="user",
        content="test",
    )

    # expire가 2번 호출 (conv_key, msg_key)
    assert mock_redis.expire.call_count == 2
    expected_ttl = 7 * 24 * 3600
    for call in mock_redis.expire.call_args_list:
        assert call[0][1] == expected_ttl


@pytest.mark.asyncio
async def test_save_message_json_serialization(service, mock_redis):
    """rpush에 전달되는 값이 유효한 JSON 문자열."""
    mock_redis.hget = AsyncMock(return_value=None)
    mock_redis.rpush = AsyncMock()
    mock_redis.hset = AsyncMock()
    mock_redis.expire = AsyncMock()

    await service.save_message(
        user_id="user1",
        conversation_id="conv1",
        message_id="msg1",
        role="user",
        content="test message",
    )

    rpush_args = mock_redis.rpush.call_args
    json_str = rpush_args[0][1]
    # json.loads가 성공하면 유효한 JSON
    parsed = json.loads(json_str)
    assert parsed["message_id"] == "msg1"
    assert parsed["role"] == "user"
    assert parsed["content"] == "test message"
    assert "created_at" in parsed


# ============================================================
# list / get / delete 테스트
# ============================================================


@pytest.mark.asyncio
async def test_list_conversations_sorted(service, mock_redis):
    """대화 목록이 updated_at 기준 내림차순 정렬."""
    conv1 = json.dumps({
        "conversation_id": "c1",
        "title": "Old",
        "updated_at": "2026-01-01T00:00:00",
    })
    conv2 = json.dumps({
        "conversation_id": "c2",
        "title": "New",
        "updated_at": "2026-02-01T00:00:00",
    })
    mock_redis.hgetall = AsyncMock(return_value={"c1": conv1, "c2": conv2})

    result = await service.list_conversations("user1")

    assert len(result) == 2
    assert result[0]["conversation_id"] == "c2"  # 최신 먼저
    assert result[1]["conversation_id"] == "c1"


@pytest.mark.asyncio
async def test_list_conversations_empty(service, mock_redis):
    """대화가 없으면 빈 리스트 반환."""
    mock_redis.hgetall = AsyncMock(return_value={})

    result = await service.list_conversations("user1")

    assert result == []


@pytest.mark.asyncio
async def test_get_messages(service, mock_redis):
    """lrange로 메시지 목록 조회."""
    msg1 = json.dumps({"message_id": "m1", "role": "user", "content": "hi"})
    msg2 = json.dumps({"message_id": "m2", "role": "assistant", "content": "hello"})
    mock_redis.lrange = AsyncMock(return_value=[msg1, msg2])

    result = await service.get_messages("user1", "conv1")

    assert len(result) == 2
    assert result[0]["message_id"] == "m1"
    assert result[1]["role"] == "assistant"
    mock_redis.lrange.assert_called_once_with("chatbot:messages:user1:conv1", 0, -1)


@pytest.mark.asyncio
async def test_delete_conversation(service, mock_redis):
    """대화 삭제 시 hdel + delete 호출."""
    mock_redis.hdel = AsyncMock()
    mock_redis.delete = AsyncMock()

    await service.delete_conversation("user1", "conv1")

    mock_redis.hdel.assert_called_once_with("chatbot:conversations:user1", "conv1")
    mock_redis.delete.assert_called_once_with("chatbot:messages:user1:conv1")
