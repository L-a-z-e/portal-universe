import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import chat, documents, health
from app.core.config import settings
from app.core.logging_config import setup_logging
from app.rag.engine import rag_engine

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging()
    logger.info(
        "chatbot-service starting: provider=%s, model=%s",
        settings.ai_provider,
        settings.ai_model,
    )
    await rag_engine.initialize()
    logger.info("RAG engine initialized")
    yield
    logger.info("chatbot-service shutting down")


app = FastAPI(
    title="Portal Universe Chatbot Service",
    version="0.1.0",
    lifespan=lifespan,
)

if settings.cors_enabled:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

app.include_router(health.router, prefix="/api/v1/chat", tags=["health"])
app.include_router(chat.router, prefix="/api/v1/chat", tags=["chat"])
app.include_router(documents.router, prefix="/api/v1/chat/documents", tags=["documents"])
