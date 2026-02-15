import json
import logging
from datetime import datetime, timezone

import redis.asyncio as redis

from app.core.config import settings
from app.schemas.chat import SourceInfo
from app.schemas.enums import MessageRole

logger = logging.getLogger(__name__)


class ConversationService:
    """Redis 기반 대화 이력 관리."""

    def __init__(self) -> None:
        self._redis: redis.Redis | None = None

    async def _get_redis(self) -> redis.Redis:
        if self._redis is None:
            self._redis = redis.from_url(settings.redis_url, decode_responses=True)
        return self._redis

    def _conv_key(self, user_id: str) -> str:
        return f"chatbot:conversations:{user_id}"

    def _msg_key(self, user_id: str, conversation_id: str) -> str:
        return f"chatbot:messages:{user_id}:{conversation_id}"

    async def save_message(
        self,
        user_id: str,
        conversation_id: str,
        message_id: str,
        role: MessageRole,
        content: str,
        sources: list[SourceInfo] | None = None,
    ) -> None:
        r = await self._get_redis()
        now = datetime.now(timezone.utc).isoformat()

        # 메시지 저장
        message = {
            "message_id": message_id,
            "role": role,
            "content": content,
            "sources": [s.model_dump() for s in sources] if sources else None,
            "created_at": now,
        }
        await r.rpush(self._msg_key(user_id, conversation_id), json.dumps(message))

        # 대화 목록에 추가/업데이트
        conv_data = await r.hget(self._conv_key(user_id), conversation_id)
        if conv_data:
            conv = json.loads(conv_data)
            conv["message_count"] = conv.get("message_count", 0) + 1
            conv["updated_at"] = now
        else:
            title = content[:50] if role == MessageRole.USER else "New conversation"
            conv = {
                "conversation_id": conversation_id,
                "title": title,
                "message_count": 1,
                "created_at": now,
                "updated_at": now,
            }
        await r.hset(self._conv_key(user_id), conversation_id, json.dumps(conv))

        # TTL: 7일
        await r.expire(self._conv_key(user_id), 7 * 24 * 3600)
        await r.expire(self._msg_key(user_id, conversation_id), 7 * 24 * 3600)

    async def list_conversations(self, user_id: str) -> list[dict]:
        r = await self._get_redis()
        raw = await r.hgetall(self._conv_key(user_id))
        conversations = [json.loads(v) for v in raw.values()]
        conversations.sort(key=lambda c: c.get("updated_at", ""), reverse=True)
        return conversations

    async def get_messages(self, user_id: str, conversation_id: str) -> list[dict]:
        r = await self._get_redis()
        raw = await r.lrange(self._msg_key(user_id, conversation_id), 0, -1)
        return [json.loads(m) for m in raw]

    async def delete_conversation(self, user_id: str, conversation_id: str) -> None:
        r = await self._get_redis()
        await r.hdel(self._conv_key(user_id), conversation_id)
        await r.delete(self._msg_key(user_id, conversation_id))


conversation_service = ConversationService()
