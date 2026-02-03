# Chatbot Service 실행 가이드

## 개요

Chatbot Service는 RAG (Retrieval-Augmented Generation) 기반 Q&A Chatbot을 제공하는 서비스입니다.
사용자가 업로드한 문서를 기반으로 질문에 답변하며, 다양한 AI Provider를 지원합니다.

### 기술 스택
- **Language**: Python 3.11+
- **Framework**: FastAPI 0.128.0+
- **AI/LLM**: LangChain (OpenAI, Anthropic, Google, Ollama)
- **Vector DB**: ChromaDB 1.4.1+
- **Cache/Session**: Redis 7.1.0+
- **Document Processing**: PyPDF, Unstructured

### 주요 기능
- 다중 AI Provider 지원 (OpenAI, Anthropic, Google Gemini, Ollama)
- 문서 업로드 및 인덱싱 (PDF, TXT, MD)
- RAG 기반 질문 답변 (동기/스트리밍)
- 대화 이력 관리 (Redis)
- Vector 검색 (ChromaDB)

---

## 사전 요구사항

### 필수 설치
- **Python 3.11+** (venv는 Python 3.11 사용)
- **Redis** (대화 이력 저장)
- **AI Provider** 중 하나:
  - **Ollama** (로컬, 권장): [https://ollama.ai](https://ollama.ai)
  - **OpenAI API Key**
  - **Anthropic API Key**
  - **Google API Key**

### Ollama 설치 및 모델 다운로드 (권장)
```bash
# Ollama 설치 (macOS)
brew install ollama

# Ollama 서비스 시작
ollama serve

# 모델 다운로드
ollama pull llama3          # LLM 모델
ollama pull nomic-embed-text  # Embedding 모델

# 모델 확인
ollama list
```

---

## Local 실행 방법

### 1. Python venv 설정

```bash
cd /Users/laze/Laze/Project/portal-universe/services/chatbot-service

# venv 생성 (Python 3.11)
python3.11 -m venv .venv

# venv 활성화
source .venv/bin/activate

# 의존성 설치
pip install -e .

# 개발 의존성 설치 (선택)
pip install -e ".[dev]"
```

### 2. 환경변수 설정

`.env` 파일 생성:
```bash
cp .env.example .env
```

`.env` 파일 편집:
```bash
# === AI Provider ===
# Provider: openai | anthropic | google | ollama
AI_PROVIDER=ollama
AI_MODEL=llama3
AI_API_KEY=  # Ollama는 불필요, 다른 Provider는 필수

# === Embedding Provider ===
EMBEDDING_PROVIDER=ollama
EMBEDDING_MODEL=nomic-embed-text
EMBEDDING_API_KEY=  # AI_API_KEY와 다를 경우만 설정

# Ollama (local)
OLLAMA_BASE_URL=http://localhost:11434

# === Vector DB ===
VECTOR_DB_TYPE=chroma
CHROMA_PERSIST_DIR=./data/chroma

# === Redis ===
REDIS_URL=redis://localhost:6379/1

# === Service ===
SERVICE_PORT=8086
LOG_LEVEL=INFO
DOCUMENTS_DIR=./documents

# === RAG ===
RAG_CHUNK_SIZE=1000
RAG_CHUNK_OVERLAP=200
RAG_TOP_K=5
RAG_SCORE_THRESHOLD=0.7
```

### 3. 디렉토리 생성

```bash
mkdir -p data/chroma
mkdir -p documents
```

### 4. 서비스 실행

```bash
# Uvicorn 직접 실행
uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload

# 또는 Python 모듈로 실행
python -m uvicorn app.main:app --host 0.0.0.0 --port 8086 --reload
```

### 5. 동작 확인

```bash
# Health Check
curl http://localhost:8086/api/v1/chat/health

# 예상 응답:
# {
#   "status": "healthy",
#   "provider": "ollama",
#   "model": "llama3",
#   "vectorstore": "chroma",
#   "documents_count": 0
# }
```

---

## Docker 실행 방법

### 1. 이미지 빌드

```bash
cd /Users/laze/Laze/Project/portal-universe/services/chatbot-service

docker build -t chatbot-service:latest .
```

### 2. Docker Compose로 전체 스택 실행

```bash
cd /Users/laze/Laze/Project/portal-universe

# Redis와 함께 실행
docker compose up -d redis chatbot-service

# 로그 확인
docker compose logs -f chatbot-service
```

### 3. 컨테이너 환경변수 (docker-compose.yml)

Docker Compose 사용 시 다음 환경변수가 자동 설정됩니다:
```yaml
environment:
  - AI_PROVIDER=ollama
  - AI_MODEL=llama3
  - AI_API_KEY=
  - EMBEDDING_PROVIDER=ollama
  - EMBEDDING_MODEL=nomic-embed-text
  - OLLAMA_BASE_URL=http://ollama:11434  # 컨테이너 네트워크
  - REDIS_URL=redis://redis:6379/1
  - CHROMA_PERSIST_DIR=/app/data/chroma
  - DOCUMENTS_DIR=/app/documents
  - SERVICE_PORT=8086
  - LOG_LEVEL=INFO
```

### 4. 볼륨 확인

```bash
# ChromaDB 데이터 볼륨
docker volume inspect portal-universe_chatbot-data

# 문서 볼륨
docker volume inspect portal-universe_chatbot-docs
```

### 5. 동작 확인

```bash
curl http://localhost:8086/api/v1/chat/health
```

---

## Kubernetes 배포 방법

### 1. Docker 이미지 빌드

```bash
cd /Users/laze/Laze/Project/portal-universe

# 이미지 빌드
docker build -t portal-universe-chatbot-service:latest -f services/chatbot-service/Dockerfile services/chatbot-service
```

### 2. ConfigMap 및 Secret 수정 (선택)

```bash
# k8s/services/chatbot-service.yaml 편집
kubectl edit configmap chatbot-config -n portal-universe

# Secret 수정 (API Key 필요 시)
# AI_API_KEY를 base64로 인코딩
echo -n "your-api-key" | base64
# 출력된 값을 Secret에 입력
kubectl edit secret chatbot-secret -n portal-universe
```

### 3. 배포

```bash
# Namespace 생성 (없는 경우)
kubectl create namespace portal-universe

# 배포
kubectl apply -f k8s/services/chatbot-service.yaml

# 배포 상태 확인
kubectl get pods -n portal-universe -l app=chatbot-service

# 로그 확인
kubectl logs -f deployment/chatbot-service -n portal-universe
```

### 4. 서비스 확인

```bash
# 서비스 정보 확인
kubectl get svc chatbot-service -n portal-universe

# Port Forward로 로컬 테스트
kubectl port-forward svc/chatbot-service 8086:8086 -n portal-universe

# Health Check
curl http://localhost:8086/api/v1/chat/health
```

### 5. PVC 확인

```bash
# PVC 상태 확인
kubectl get pvc -n portal-universe | grep chatbot

# 예상 출력:
# chatbot-data-pvc   Bound    pvc-xxx   2Gi        RWO
# chatbot-docs-pvc   Bound    pvc-xxx   1Gi        RWO
```

---

## API 엔드포인트 목록

### Health Check
```
GET /api/v1/chat/health
```
응답:
```json
{
  "status": "healthy",
  "provider": "ollama",
  "model": "llama3",
  "vectorstore": "chroma",
  "documents_count": 5
}
```

### Chat - 동기 답변
```
POST /api/v1/chat/message
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "message": "문서의 주요 내용은 무엇인가?",
  "conversation_id": "optional-uuid"
}
```
응답:
```json
{
  "success": true,
  "data": {
    "answer": "문서의 주요 내용은...",
    "sources": [
      {
        "document": "example.pdf",
        "chunk": "관련 텍스트...",
        "relevance_score": 0.85
      }
    ],
    "conversation_id": "uuid",
    "message_id": "uuid"
  }
}
```

### Chat - 스트리밍 답변
```
POST /api/v1/chat/stream
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "message": "문서의 주요 내용은 무엇인가?",
  "conversation_id": "optional-uuid"
}
```
응답: Server-Sent Events (SSE)
```
data: {"type": "token", "content": "문서"}
data: {"type": "token", "content": "의"}
data: {"type": "sources", "sources": [...]}
data: {"type": "done", "message_id": "uuid", "conversation_id": "uuid"}
```

### 대화 목록 조회
```
GET /api/v1/chat/conversations
Authorization: Bearer <JWT_TOKEN>
```

### 대화 이력 조회
```
GET /api/v1/chat/conversations/{conversation_id}
Authorization: Bearer <JWT_TOKEN>
```

### 대화 삭제
```
DELETE /api/v1/chat/conversations/{conversation_id}
Authorization: Bearer <JWT_TOKEN>
```

### 문서 업로드 (Admin Only)
```
POST /api/v1/chat/documents/upload
Authorization: Bearer <JWT_TOKEN> (Admin Role)
Content-Type: multipart/form-data

file: example.pdf
```
응답:
```json
{
  "success": true,
  "data": {
    "document_id": "example",
    "filename": "example.pdf",
    "chunks": 42,
    "status": "indexed"
  }
}
```

### 문서 목록 조회
```
GET /api/v1/chat/documents
Authorization: Bearer <JWT_TOKEN>
```

### 문서 삭제 (Admin Only)
```
DELETE /api/v1/chat/documents/{document_id}
Authorization: Bearer <JWT_TOKEN> (Admin Role)
```

### 전체 재인덱싱 (Admin Only)
```
POST /api/v1/chat/documents/reindex
Authorization: Bearer <JWT_TOKEN> (Admin Role)
```

---

## AI Provider 설정

### 1. Ollama (로컬, 권장)

```bash
# .env 설정
AI_PROVIDER=ollama
AI_MODEL=llama3
OLLAMA_BASE_URL=http://localhost:11434

EMBEDDING_PROVIDER=ollama
EMBEDDING_MODEL=nomic-embed-text
```

**장점**:
- API Key 불필요
- 데이터 외부 유출 없음
- 무료
- 빠른 응답 (로컬)

**단점**:
- 모델 크기에 따른 메모리 사용
- GPU 없으면 느림

### 2. OpenAI

```bash
# .env 설정
AI_PROVIDER=openai
AI_MODEL=gpt-4o-mini
AI_API_KEY=sk-...

EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=text-embedding-3-small
```

### 3. Anthropic

```bash
# .env 설정
AI_PROVIDER=anthropic
AI_MODEL=claude-3-5-sonnet-20241022
AI_API_KEY=sk-ant-...

# Embedding은 다른 Provider 사용 (Anthropic은 미제공)
EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_API_KEY=sk-...
```

### 4. Google Gemini

```bash
# .env 설정
AI_PROVIDER=google
AI_MODEL=gemini-1.5-flash
AI_API_KEY=...

EMBEDDING_PROVIDER=google
EMBEDDING_MODEL=models/text-embedding-004
```

---

## RAG 설정

### 문서 업로드 및 인덱싱

#### 지원 파일 형식
- `.md` (Markdown)
- `.txt` (Text)
- `.pdf` (PDF)

#### 파일 크기 제한
- 최대 10MB

#### 업로드 방법

**1. API를 통한 업로드** (권장)
```bash
# Admin JWT Token 필요
curl -X POST http://localhost:8086/api/v1/chat/documents/upload \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -F "file=@/path/to/document.pdf"
```

**2. 직접 파일 배치 후 재인덱싱**
```bash
# documents/ 디렉토리에 파일 복사
cp your-document.pdf services/chatbot-service/documents/

# 재인덱싱 API 호출
curl -X POST http://localhost:8086/api/v1/chat/documents/reindex \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN"
```

### ChromaDB 설정

ChromaDB는 벡터 데이터를 로컬 파일시스템에 저장합니다.

**Local**:
```bash
CHROMA_PERSIST_DIR=./data/chroma
```

**Docker**:
```yaml
volumes:
  - chatbot-data:/app/data  # ChromaDB 데이터 영구 저장
```

**Kubernetes**:
```yaml
volumeMounts:
  - name: chatbot-data
    mountPath: /app/data
```

### RAG 파라미터 튜닝

```bash
# .env 설정
RAG_CHUNK_SIZE=1000        # 청크 크기 (토큰 수)
RAG_CHUNK_OVERLAP=200      # 청크 오버랩 (토큰 수)
RAG_TOP_K=5                # 검색 시 반환할 상위 청크 수
RAG_SCORE_THRESHOLD=0.7    # 관련성 점수 임계값 (0.0~1.0)
```

**파라미터 가이드**:
- `RAG_CHUNK_SIZE`: 클수록 컨텍스트가 많지만 정확도 떨어질 수 있음 (권장: 500~2000)
- `RAG_CHUNK_OVERLAP`: 문맥 연속성 유지 (권장: CHUNK_SIZE의 10~20%)
- `RAG_TOP_K`: 클수록 많은 참조, 하지만 노이즈 증가 (권장: 3~10)
- `RAG_SCORE_THRESHOLD`: 높을수록 엄격한 필터링 (권장: 0.6~0.8)

---

## 트러블슈팅

### 1. Ollama 연결 실패

**증상**:
```
ERROR: Failed to connect to Ollama at http://localhost:11434
```

**해결**:
```bash
# Ollama 서비스 상태 확인
ps aux | grep ollama

# Ollama 재시작
killall ollama
ollama serve

# 포트 확인
lsof -i :11434
```

### 2. Redis 연결 실패

**증상**:
```
ERROR: Could not connect to Redis at localhost:6379
```

**해결**:
```bash
# Redis 상태 확인
redis-cli ping
# 출력: PONG

# Redis 실행 (없는 경우)
brew services start redis

# Docker Compose 사용 시
docker compose up -d redis
docker compose logs redis
```

### 3. ChromaDB 초기화 실패

**증상**:
```
ERROR: Failed to initialize ChromaDB
```

**해결**:
```bash
# 디렉토리 권한 확인
ls -la data/chroma

# 디렉토리 재생성
rm -rf data/chroma
mkdir -p data/chroma

# 서비스 재시작
```

### 4. 문서 업로드 시 401 Unauthorized

**원인**: Admin 권한 필요

**해결**:
```bash
# auth-service에서 Admin Role JWT Token 발급
# 또는 security.py의 require_admin 디펜던시 확인
```

### 5. PDF 파싱 오류

**증상**:
```
ERROR: Failed to parse PDF
```

**해결**:
```bash
# PyPDF 의존성 재설치
pip install --upgrade pypdf unstructured

# 또는 venv 재생성
rm -rf .venv
python3.11 -m venv .venv
source .venv/bin/activate
pip install -e .
```

### 6. 메모리 부족 (Ollama 사용 시)

**증상**:
- 느린 응답
- 서비스 크래시

**해결**:
```bash
# 작은 모델 사용
ollama pull llama3:8b  # 대신 llama3:70b 사용 중지

# 또는 OpenAI/Anthropic으로 전환
AI_PROVIDER=openai
AI_MODEL=gpt-4o-mini
```

### 7. CORS 에러 (프론트엔드 연동 시)

**증상**:
```
Access to fetch at 'http://localhost:8086' from origin 'http://localhost:30000' has been blocked by CORS policy
```

**해결**:
```bash
# .env 설정
CORS_ENABLED=true
CORS_ORIGINS=["http://localhost:30000", "https://portal-universe:30000"]
```

또는 `app/core/config.py` 수정:
```python
cors_origins: list[str] = ["http://localhost:30000", "https://portal-universe:30000"]
```

### 8. Python 버전 불일치

**증상**:
```
ERROR: Python 3.11+ required
```

**해결**:
```bash
# Python 3.11 설치 확인
python3.11 --version

# 없으면 설치 (macOS)
brew install python@3.11

# venv 재생성
rm -rf .venv
python3.11 -m venv .venv
source .venv/bin/activate
pip install -e .
```

### 9. 로그 레벨 변경

```bash
# .env 설정
LOG_LEVEL=DEBUG  # DEBUG | INFO | WARNING | ERROR

# 또는 실행 시 환경변수
LOG_LEVEL=DEBUG uvicorn app.main:app --port 8086
```

### 10. Health Check 실패 (Kubernetes)

**증상**:
```
Readiness probe failed: HTTP probe failed with statuscode: 500
```

**해결**:
```bash
# Pod 로그 확인
kubectl logs -f deployment/chatbot-service -n portal-universe

# Redis 연결 확인
kubectl get svc redis -n portal-universe

# ConfigMap 확인
kubectl get configmap chatbot-config -n portal-universe -o yaml
```

---

## 참고 자료

- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [LangChain 문서](https://python.langchain.com/)
- [ChromaDB 문서](https://docs.trychroma.com/)
- [Ollama 공식 사이트](https://ollama.ai/)
- [Portal Universe 프로젝트 README](../../../README.md)
