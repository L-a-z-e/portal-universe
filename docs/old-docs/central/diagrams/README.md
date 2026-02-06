# Diagrams

Portal Universe 시스템 다이어그램 모음입니다.

## 구조

```
diagrams/
├── source/      # Mermaid, PlantUML 원본
└── exported/    # PNG, SVG 이미지
```

## 다이어그램 목록

### 시스템 아키텍처
| 파일명 | 설명 | 형식 |
|--------|------|------|
| [system-overview.md](./source/system-overview.md) | 전체 시스템 아키텍처 | Mermaid |
| [service-communication.md](./source/service-communication.md) | 서비스 간 통신 흐름 | Mermaid |

### ERD
| 파일명 | 설명 | 형식 |
|--------|------|------|
| [auth-erd.md](./source/auth-erd.md) | 인증 시스템 ERD | Mermaid |

### 플로우 다이어그램
| 파일명 | 설명 | 형식 |
|--------|------|------|
| [admin-flow.md](./source/admin-flow.md) | Admin 인가 흐름 | Mermaid |

## 작성 가이드

- [Diagrams 작성 가이드](../../docs_template/guide/diagrams/how-to-write.md) 참조
- Mermaid 사용 권장 (Markdown 내 렌더링 가능)
- 복잡한 다이어그램은 Draw.io 또는 PlantUML 사용
