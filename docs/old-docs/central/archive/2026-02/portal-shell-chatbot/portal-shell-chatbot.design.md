# Portal Shell Chatbot - Design

## 1. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      portal-shell (:30000)                      │
│  ┌──────────────┐                                               │
│  │ ChatWidget   │──── SSE / REST ────┐                          │
│  │ (Vue 3)      │                    │                          │
│  └──────────────┘                    │                          │
└──────────────────────────────────────┼──────────────────────────┘
                                       │
                              ┌────────▼────────┐
                              │  API Gateway    │
                              │  (:8080)        │
                              │  /api/v1/chat/* │
                              └────────┬────────┘
                                       │
                              ┌────────▼────────┐
                              │ chatbot-service │
                              │ (:8086, FastAPI)│
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
           ┌───────▼───────┐  ┌───────▼───────┐  ┌──────▼───────┐
           │  RAG Engine   │  │  AI Provider  │  │  Session     │
           │  (LangChain)  │  │  (Pluggable)  │  │  Store       │
           └───────┬───────┘  └───────────────┘  │  (Redis)     │
                   │                              └──────────────┘
           ┌───────▼───────┐
           │  Vector DB    │
           │  (ChromaDB)   │
           └───────────────┘
```

## 2. 디렉토리 구조

```
services/chatbot-service/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI 엔트리포인트
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes/
│   │       ├── __init__.py
│   │       ├── chat.py            # Chat API (질문/답변, SSE)
│   │       ├── documents.py       # 문서 관리 API
│   │       └── health.py          # Health check
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py              # 환경변수 & Provider 설정
│   │   ├── security.py            # JWT 검증 (Gateway 연동)
│   │   └── logging_config.py      # 구조화 로깅
│   ├── providers/
│   │   ├── __init__.py
│   │   ├── base.py                # AI Provider 추상 인터페이스
│   │   ├── openai_provider.py     # OpenAI (GPT-4o, Ada embedding)
│   │   ├── anthropic_provider.py  # Anthropic (Claude)
│   │   ├── google_provider.py     # Google (Gemini)
│   │   ├── ollama_provider.py     # Ollama (로컬 모델)
│   │   └── factory.py             # Provider Factory
│   ├── rag/
│   │   ├── __init__.py
│   │   ├── engine.py              # RAG 파이프라인 핵심
│   │   ├── embeddings.py          # Embedding Provider 추상화
│   │   ├── vectorstore.py         # Vector DB 연동
│   │   ├── chunker.py             # 문서 chunking 전략
│   │   └── retriever.py           # 문서 검색 로직
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── chat.py                # Chat 관련 Pydantic 모델
│   │   └── document.py            # Document 관련 Pydantic 모델
│   ├── services/
│   │   ├── __init__.py
│   │   └── conversation_service.py # 대화 이력 관리 (Redis 기반)
│   └── models/
│       ├── __init__.py
│       └── conversation.py        # 대화 이력 모델
├── documents/                      # RAG용 문서 저장소
│   └── .gitkeep
├── data/                           # ChromaDB 영속 데이터
│   └── .gitkeep
├── tests/
│   ├── __init__.py
│   ├── test_chat.py
│   ├── test_documents.py
│   └── test_providers.py
├── pyproject.toml                  # 프로젝트 설정 & 의존성
├── Dockerfile
├── .env.example                    # 환경변수 예시
└── README.md
```

## 3. API 설계

### 3.1 Chat API

#### `POST /api/v1/chat/message`
질문에 대한 답변 (동기 응답)

```json
// Request
{
  "message": "배송 정책이 어떻게 되나요?",
  "conversation_id": "conv-uuid-123"  // optional, 없으면 새 대화
}

// Response (ApiResponse 호환)
{
  "success": true,
  "data": {
    "answer": "배송 정책에 따르면 주문 후 2-3일 이내 배송됩니다...",
    "sources": [
      {
        "document": "shipping-policy.md",
        "chunk": "주문 후 2-3 영업일 이내...",
        "relevance_score": 0.92
      }
    ],
    "conversation_id": "conv-uuid-123",
    "message_id": "msg-uuid-456"
  },
  "error": null
}
```

#### `POST /api/v1/chat/stream`
Streaming 응답 (SSE)

```
// Request: 동일
// Response: text/event-stream
data: {"type": "token", "content": "배송"}
data: {"type": "token", "content": " 정책에"}
data: {"type": "token", "content": " 따르면"}
data: {"type": "sources", "sources": [...]}
data: {"type": "done", "message_id": "msg-uuid-456"}
```

#### `GET /api/v1/chat/conversations`
대화 목록 조회

#### `GET /api/v1/chat/conversations/{conversation_id}`
특정 대화 이력 조회

#### `DELETE /api/v1/chat/conversations/{conversation_id}`
대화 삭제

### 3.2 Document API

#### `POST /api/v1/chat/documents/upload`
문서 업로드 & 인덱싱 (관리자 전용)

```json
// multipart/form-data
// file: 업로드 파일 (md, pdf, txt)

// Response
{
  "success": true,
  "data": {
    "document_id": "doc-uuid-789",
    "filename": "shipping-policy.md",
    "chunks": 12,
    "status": "indexed"
  }
}
```

#### `GET /api/v1/chat/documents`
인덱싱된 문서 목록

#### `DELETE /api/v1/chat/documents/{document_id}`
문서 삭제 & 벡터 제거

#### `POST /api/v1/chat/documents/reindex`
전체 문서 재인덱싱

### 3.3 Health API

#### `GET /api/v1/chat/health`
서비스 헬스체크 (public)

```json
{
  "status": "healthy",
  "provider": "openai",
  "model": "gpt-4o-mini",
  "vectorstore": "chroma",
  "documents_count": 15
}
```

## 4. Provider 추상화 설계

```python
# providers/base.py
from abc import ABC, abstractmethod
from typing import AsyncIterator

class LLMProvider(ABC):
    """AI LLM Provider 추상 인터페이스"""

    @abstractmethod
    async def generate(self, prompt: str, context: str) -> str:
        """동기 응답 생성"""
        ...

    @abstractmethod
    async def stream(self, prompt: str, context: str) -> AsyncIterator[str]:
        """스트리밍 응답 생성"""
        ...

class EmbeddingProvider(ABC):
    """Embedding Provider 추상 인터페이스"""

    @abstractmethod
    async def embed_text(self, text: str) -> list[float]:
        ...

    @abstractmethod
    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        ...
```

```python
# providers/factory.py
class ProviderFactory:
    """설정 기반 Provider 생성"""

    @staticmethod
    def create_llm(config: ProviderConfig) -> LLMProvider:
        match config.provider:
            case "openai":
                return OpenAIProvider(api_key=config.api_key, model=config.model)
            case "anthropic":
                return AnthropicProvider(api_key=config.api_key, model=config.model)
            case "google":
                return GoogleProvider(api_key=config.api_key, model=config.model)
            case "ollama":
                return OllamaProvider(base_url=config.base_url, model=config.model)
            case _:
                raise ValueError(f"Unknown provider: {config.provider}")

    @staticmethod
    def create_embedding(config: EmbeddingConfig) -> EmbeddingProvider:
        match config.provider:
            case "openai":
                return OpenAIEmbedding(api_key=config.api_key, model=config.model)
            case "sentence-transformers":
                return LocalEmbedding(model=config.model)
            case "ollama":
                return OllamaEmbedding(base_url=config.base_url, model=config.model)
            case _:
                raise ValueError(f"Unknown embedding provider: {config.provider}")
```

## 5. 설정 구조

### 5.1 환경변수 (.env)

```bash
# === AI Provider 설정 ===
# LLM Provider: openai | anthropic | google | ollama
AI_PROVIDER=openai
AI_MODEL=gpt-4o-mini
AI_API_KEY=sk-...

# Embedding Provider: openai | sentence-transformers | ollama
EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_API_KEY=sk-...  # AI_API_KEY와 다를 경우

# Ollama 전용 (local 개발용)
OLLAMA_BASE_URL=http://localhost:11434

# === Vector DB ===
VECTOR_DB_TYPE=chroma          # chroma | elasticsearch
CHROMA_PERSIST_DIR=./data/chroma
# Elasticsearch (기존 인프라 활용 시)
# ES_URL=http://localhost:9200

# === Redis (대화 이력) ===
REDIS_URL=redis://localhost:6379/1

# === 서비스 설정 ===
SERVICE_PORT=8086
LOG_LEVEL=INFO
DOCUMENTS_DIR=./documents

# === RAG 설정 ===
RAG_CHUNK_SIZE=1000
RAG_CHUNK_OVERLAP=200
RAG_TOP_K=5
RAG_SCORE_THRESHOLD=0.7
```

### 5.2 환경별 기본 설정

| 환경 | AI Provider | Embedding | Vector DB | Redis |
|------|-------------|-----------|-----------|-------|
| **Local** | ollama (llama3) | ollama (nomic-embed-text) | ChromaDB (local) | localhost:6379 |
| **Docker** | 환경변수 설정 | 환경변수 설정 | ChromaDB (volume) | redis:6379 |
| **K8s** | Secret으로 관리 | Secret으로 관리 | ChromaDB (PVC) | redis-svc:6379 |

## 6. RAG Pipeline 설계

```
[사용자 질문]
      │
      ▼
[Query Preprocessing]        ← 질문 정제 (불용어 제거, 핵심어 추출)
      │
      ▼
[Embedding]                  ← 질문을 벡터로 변환
      │
      ▼
[Vector Search]              ← ChromaDB에서 유사 문서 청크 검색 (top-k)
      │
      ▼
[Score Filtering]            ← relevance score threshold 이하 제거
      │
      ▼
[Context Assembly]           ← 검색된 청크들을 컨텍스트로 조합
      │
      ▼
[Prompt Construction]        ← System prompt + Context + Question
      │
      ▼
[LLM Generation]             ← AI Provider로 답변 생성
      │
      ▼
[Response + Sources]         ← 답변 + 출처 정보 반환
```

### System Prompt 템플릿

```
당신은 도움이 되는 Q&A 어시스턴트입니다.
아래 제공된 문서 내용만을 기반으로 질문에 답변하세요.

규칙:
1. 제공된 문서에 없는 내용은 "해당 정보를 찾을 수 없습니다"라고 답변하세요.
2. 답변할 때 어떤 문서를 참고했는지 언급하세요.
3. 추측하거나 문서 외의 지식을 사용하지 마세요.

---
[문서 컨텍스트]
{context}
---

질문: {question}
```

## 7. 인프라 통합

### 7.1 API Gateway 라우팅 추가

```yaml
# application.yml 추가
services:
  chatbot:
    url: http://localhost:8086

routes:
  # Chatbot Health - public
  - id: chatbot-service-health
    uri: ${services.chatbot.url}
    predicates:
      - Path=/api/v1/chat/health
    order: 0

  # Chatbot SSE - long-lived (no rate limit, authenticated)
  - id: chatbot-service-sse
    uri: ${services.chatbot.url}
    predicates:
      - Path=/api/v1/chat/stream
      - Method=POST
    filters:
      - name: CircuitBreaker
        args:
          name: chatbotCircuitBreaker
          fallbackUri: forward:/fallback/chatbot
    order: 1

  # Chatbot Document API - admin only
  - id: chatbot-service-documents
    uri: ${services.chatbot.url}
    predicates:
      - Path=/api/v1/chat/documents/**
    filters:
      - name: RequestRateLimiter
        args:
          rate-limiter: "#{@authenticatedRedisRateLimiter}"
          key-resolver: "#{@userKeyResolver}"
      - name: CircuitBreaker
        args:
          name: chatbotCircuitBreaker
    order: 2

  # Chatbot API - authenticated
  - id: chatbot-service-route
    uri: ${services.chatbot.url}
    predicates:
      - Path=/api/v1/chat/**
    filters:
      - name: RequestRateLimiter
        args:
          rate-limiter: "#{@authenticatedRedisRateLimiter}"
          key-resolver: "#{@userKeyResolver}"
      - name: CircuitBreaker
        args:
          name: chatbotCircuitBreaker
          fallbackUri: forward:/fallback/chatbot
    order: 3
```

### 7.2 docker-compose 추가

```yaml
chatbot-service:
  build:
    context: ./services/chatbot-service
    dockerfile: Dockerfile
  container_name: chatbot-service
  ports:
    - "8086:8086"
  environment:
    - AI_PROVIDER=${AI_PROVIDER:-ollama}
    - AI_MODEL=${AI_MODEL:-llama3}
    - AI_API_KEY=${AI_API_KEY:-}
    - OLLAMA_BASE_URL=http://ollama:11434
    - REDIS_URL=redis://redis:6379/1
    - VECTOR_DB_TYPE=chroma
    - CHROMA_PERSIST_DIR=/app/data/chroma
    - DOCUMENTS_DIR=/app/documents
  volumes:
    - chatbot-data:/app/data
    - chatbot-docs:/app/documents
  depends_on:
    - redis
  networks:
    - portal-network
```

### 7.3 Kubernetes Manifest (개요)

```yaml
# k8s/services/chatbot-service.yaml
# - Deployment (1 replica)
# - Service (ClusterIP, port 8086)
# - ConfigMap (RAG 설정)
# - Secret (AI API Keys)
# - PVC (ChromaDB data, documents)
```

## 8. Frontend Chat Widget 설계

### portal-shell 통합 위치

```
frontend/portal-shell/src/
├── components/
│   └── chat/
│       ├── ChatWidget.vue          # 메인 위젯 (플로팅 버튼 + 패널)
│       ├── ChatPanel.vue           # 대화 패널
│       ├── ChatMessage.vue         # 메시지 버블
│       ├── ChatInput.vue           # 입력 영역
│       └── ChatSourceBadge.vue     # 출처 표시 배지
├── composables/
│   └── useChat.ts                  # Chat API 호출 및 상태 관리 (Composable)
└── types/
    └── chat.ts                     # Chat 관련 타입
```

### Widget UI 동작

```
1. 화면 우하단 플로팅 채팅 버튼
2. 클릭 → 채팅 패널 슬라이드 오픈
3. 인증 확인 → 미인증 시 로그인 유도
4. 질문 입력 → SSE 스트리밍 응답 표시
5. 출처 문서 접기/펼치기 가능
6. 대화 이력 세션 관리
```

## 9. 의존성 목록

```toml
# pyproject.toml
[project]
name = "chatbot-service"
version = "0.1.0"
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.115.0",
    "uvicorn[standard]>=0.34.0",
    "pydantic>=2.10.0",
    "pydantic-settings>=2.7.0",
    # AI Providers
    "langchain>=0.3.0",
    "langchain-openai>=0.3.0",
    "langchain-anthropic>=0.3.0",
    "langchain-google-genai>=2.0.0",
    "langchain-ollama>=0.3.0",
    "langchain-chroma>=0.2.0",
    # Document processing
    "langchain-community>=0.3.0",
    "pypdf>=5.0.0",
    "unstructured>=0.16.0",
    # Vector DB
    "chromadb>=0.6.0",
    # Redis
    "redis[hiredis]>=5.2.0",
    # Utilities
    "python-multipart>=0.0.18",
    "sse-starlette>=2.2.0",
    "httpx>=0.28.0",
    "python-jose[cryptography]>=3.3.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.0.0",
    "pytest-asyncio>=0.24.0",
    "httpx>=0.28.0",
    "ruff>=0.8.0",
]
```

## 10. 구현 순서

```
Step 1: 프로젝트 초기 구조 생성
        - FastAPI 앱 세팅, 디렉토리 구조, pyproject.toml

Step 2: Provider 추상화 구현
        - base.py 인터페이스, factory.py
        - OpenAI, Anthropic, Ollama provider 구현

Step 3: RAG Pipeline 구현
        - 문서 로더, chunker, embedding, vectorstore
        - 검색 & 답변 생성 파이프라인

Step 4: API 엔드포인트 구현
        - Chat API (동기 + SSE 스트리밍)
        - Document API (업로드, 목록, 삭제)
        - Health API

Step 5: API Response 형식 통일
        - ApiResponse wrapper (기존 Java 서비스와 호환)
        - JWT 토큰 검증 (Gateway에서 전달된 헤더)

Step 6: Docker & docker-compose 통합
        - Dockerfile
        - docker-compose.yml에 서비스 추가

Step 7: API Gateway 라우팅 추가
        - 라우팅 규칙, Circuit Breaker, Rate Limiter

Step 8: Frontend Chat Widget
        - Vue 3 컴포넌트, Pinia 스토어, SSE 연동

Step 9: Kubernetes manifests
        - Deployment, Service, ConfigMap, Secret, PVC

Step 10: 테스트 & 문서화
```

## 11. 보안 고려사항

- API Key는 환경변수/Secret으로만 관리 (코드에 하드코딩 금지)
- JWT 검증은 Gateway에서 처리, chatbot-service는 `X-User-Id` 헤더 신뢰
- 문서 업로드는 관리자 권한만 허용 (RBAC 연동)
- 파일 업로드 시 확장자/크기 제한 (md, pdf, txt / 10MB)
- Rate limiting은 Gateway에서 처리
