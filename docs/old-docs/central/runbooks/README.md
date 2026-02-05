# Runbooks

Portal Universe 운영 절차서 모음입니다.

## 목록

### 배포

| 파일명 | 설명 | 소요시간 |
|--------|------|----------|
| [deploy-production.md](./deploy-production.md) | Production 배포 절차 | 30분 |
| [rollback-procedure.md](./rollback-procedure.md) | 롤백 절차 | 10분 |

### 장애 대응

| 파일명 | 설명 | 소요시간 |
|--------|------|----------|
| [incident-response.md](./incident-response.md) | 장애 대응 매뉴얼 | - |

### 스케일링

| 파일명 | 설명 | 소요시간 |
|--------|------|----------|
| [scale-out.md](./scale-out.md) | 스케일아웃 절차 | 15분 |

### 보안

| 파일명 | 설명 | 소요시간 |
|--------|------|----------|
| [secret-rotation.md](./secret-rotation.md) | Secret 로테이션 절차 (DB, JWT) | DB: 15분, JWT: 20분 |

## 작성 가이드

- [Runbook 작성 가이드](../../docs_template/guide/runbooks/how-to-write.md) 참조
- 모든 명령어는 복사해서 바로 실행 가능해야 함
- 각 Step마다 **예상 결과** 필수 기재
- 롤백/에스컬레이션 정보 필수 포함

## 에스컬레이션 연락처

| 역할 | 담당자 | 연락처 |
|------|--------|--------|
| DevOps Lead | TBD | - |
| Backend Lead | TBD | - |
| Frontend Lead | TBD | - |
