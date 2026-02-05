# Portal Shell Chatbot - Plan

## 개요
RAG(Retrieval-Augmented Generation) 기반 Q&A Chatbot 서비스.
지정된 문서 내에서만 답변하며, 다양한 AI Provider를 API Key 설정만으로 교체 가능.

## 핵심 요구사항

### 1. RAG 기반 Q&A
- 사전 정의된 문서(Markdown, PDF, TXT 등)를 벡터 DB에 인덱싱
- 사용자 질문 → 관련 문서 검색 → AI가 문서 기반으로 답변 생성
- 문서 외 내용은 "해당 정보를 찾을 수 없습니다" 응답
- 답변 시 출처(source) 문서 참조 표시

### 2. Multi-Provider AI 지원
- 서버 설정(환경변수/config)으로 AI Provider 교체
- API Key만 변경하면 다른 AI로 전환 가능
- 지원 Provider 목록:
  - **OpenAI**: GPT-4o, GPT-4o-mini
  - **Anthropic**: Claude 3.5 Sonnet, Claude 3 Haiku
  - **Google**: Gemini Pro
  - **Ollama**: 로컬 모델 (llama3, mistral 등)
- Embedding 모델도 Provider별 교체 가능

### 3. 환경별 배포
- **Local**: 개발자 로컬 환경 (Ollama 로컬 모델 또는 외부 API)
- **Docker**: docker-compose 통합
- **Kubernetes**: k8s manifests 제공

### 4. portal-shell 통합
- portal-shell에 Chat Widget UI 추가
- 인증된 사용자만 사용 가능 (JWT 토큰 활용)
- 대화 이력 관리

## 기술 스택

| 영역 | 기술 | 사유 |
|------|------|------|
| Backend | Python 3.12 / FastAPI | AI/ML 생태계 최적, LangChain 등 라이브러리 풍부 |
| AI Framework | LangChain | Multi-provider 추상화, RAG 파이프라인 내장 |
| Vector DB | ChromaDB (내장) / Elasticsearch (기존 활용) | 소규모: Chroma, 확장 시: 기존 ES 활용 |
| Embedding | OpenAI Ada / Sentence-Transformers | Provider별 교체 가능 |
| Frontend | Vue 3 (portal-shell 내) | 기존 Host 앱에 Widget 추가 |
| API Gateway | 기존 Spring Cloud Gateway 라우팅 추가 | 일관된 API 경로 |

## 서비스 포트

| 환경 | Port |
|------|------|
| chatbot-service | **8086** |

## 아키텍처 개요

```
[portal-shell] → [API Gateway :8080] → [chatbot-service :8086 (FastAPI)]
                                              ↓
                                    [LangChain RAG Pipeline]
                                         ↓           ↓
                                  [Vector DB]    [AI Provider]
                                  (Chroma/ES)   (OpenAI/Claude/Gemini/Ollama)
```

## 구현 범위

### Phase 1 (MVP)
- [ ] Python/FastAPI chatbot-service 프로젝트 구조
- [ ] LangChain 기반 RAG 파이프라인
- [ ] Multi-provider 설정 (OpenAI, Anthropic, Ollama)
- [ ] 문서 업로드 및 인덱싱 API
- [ ] Chat API (질문 → 답변)
- [ ] API Gateway 라우팅 추가
- [ ] Docker / docker-compose 통합

### Phase 2 (Frontend 통합)
- [ ] portal-shell Chat Widget UI
- [ ] 대화 이력 관리 (세션 기반)
- [ ] Streaming 응답 (SSE)

### Phase 3 (고도화)
- [ ] Kubernetes manifests
- [ ] 문서 자동 동기화 (파일 시스템 watch)
- [ ] 관리자 문서 관리 UI
- [ ] 답변 피드백 (좋아요/싫어요)

## 리스크 & 고려사항

| 리스크 | 대응 |
|--------|------|
| AI Provider 비용 | Ollama 로컬 모델로 개발, 프로덕션만 유료 API |
| 벡터 DB 선택 | ChromaDB로 시작, 필요 시 ES 마이그레이션 |
| 응답 지연 | Streaming(SSE) 적용, Gateway timeout 조정 |
| 문서 품질 | 문서 chunking 전략 최적화 필요 |
| Polyglot 통합 | FastAPI ↔ Spring Gateway 간 ApiResponse 형식 통일 |

## 참조
- `.claude/rules/python.md` - Python/FastAPI 패턴
- `services/api-gateway/` - Gateway 라우팅 설정
- `frontend/portal-shell/` - Host 앱 구조
