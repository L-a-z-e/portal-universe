import json
import logging
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sse_starlette.sse import EventSourceResponse

from app.core.security import get_current_user_id
from app.rag.engine import rag_engine
from app.schemas.chat import ChatRequest, ChatResponse, SourceInfo
from app.schemas.common import ApiResponse
from app.services.conversation_service import conversation_service

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post("/message")
async def send_message(
    request: ChatRequest,
    user_id: str = Depends(get_current_user_id),
):
    """질문에 대한 동기 답변."""
    conversation_id = request.conversation_id or str(uuid.uuid4())
    message_id = str(uuid.uuid4())

    logger.info(
        "Chat message: user=%s, conversation=%s, message=%s",
        user_id,
        conversation_id,
        request.message[:50],
    )

    answer, sources = await rag_engine.query(request.message)

    # 대화 이력 저장
    await conversation_service.save_message(
        user_id=user_id,
        conversation_id=conversation_id,
        message_id=message_id,
        role="user",
        content=request.message,
    )
    await conversation_service.save_message(
        user_id=user_id,
        conversation_id=conversation_id,
        message_id=str(uuid.uuid4()),
        role="assistant",
        content=answer,
        sources=sources,
    )

    response = ChatResponse(
        answer=answer,
        sources=sources,
        conversation_id=conversation_id,
        message_id=message_id,
    )
    return ApiResponse.ok(response.model_dump())


@router.post("/stream")
async def stream_message(
    request: ChatRequest,
    user_id: str = Depends(get_current_user_id),
):
    """질문에 대한 SSE 스트리밍 답변."""
    conversation_id = request.conversation_id or str(uuid.uuid4())
    message_id = str(uuid.uuid4())

    logger.info(
        "Chat stream: user=%s, conversation=%s",
        user_id,
        conversation_id,
    )

    # 사용자 메시지 저장
    await conversation_service.save_message(
        user_id=user_id,
        conversation_id=conversation_id,
        message_id=message_id,
        role="user",
        content=request.message,
    )

    async def event_generator():
        full_answer = []
        sources: list[dict] = []

        async for event in rag_engine.query_stream(request.message):
            if event["type"] == "token":
                full_answer.append(event["content"])
                yield {"data": json.dumps(event, ensure_ascii=False)}
            elif event["type"] == "sources":
                sources = event["sources"]
                yield {"data": json.dumps(event, ensure_ascii=False)}
            elif event["type"] == "done":
                # assistant 메시지 저장
                answer_text = "".join(full_answer)
                source_infos = [
                    SourceInfo(
                        document=s["document"],
                        chunk=s["chunk"],
                        relevance_score=s["relevance_score"],
                    )
                    for s in sources
                ]
                await conversation_service.save_message(
                    user_id=user_id,
                    conversation_id=conversation_id,
                    message_id=str(uuid.uuid4()),
                    role="assistant",
                    content=answer_text,
                    sources=source_infos,
                )
                done_event = {
                    "type": "done",
                    "message_id": message_id,
                    "conversation_id": conversation_id,
                }
                yield {"data": json.dumps(done_event, ensure_ascii=False)}

    return EventSourceResponse(event_generator())


@router.get("/conversations")
async def list_conversations(user_id: str = Depends(get_current_user_id)):
    """대화 목록 조회."""
    conversations = await conversation_service.list_conversations(user_id)
    return ApiResponse.ok(conversations)


@router.get("/conversations/{conversation_id}")
async def get_conversation(
    conversation_id: str,
    user_id: str = Depends(get_current_user_id),
):
    """특정 대화 이력 조회."""
    messages = await conversation_service.get_messages(user_id, conversation_id)
    return ApiResponse.ok(messages)


@router.delete("/conversations/{conversation_id}")
async def delete_conversation(
    conversation_id: str,
    user_id: str = Depends(get_current_user_id),
):
    """대화 삭제."""
    await conversation_service.delete_conversation(user_id, conversation_id)
    return ApiResponse.ok({"deleted": conversation_id})
