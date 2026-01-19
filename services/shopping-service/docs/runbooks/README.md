# Shopping Service Runbooks

> 운영 절차서 목록 및 개요

---

## 📋 개요

이 디렉토리는 Shopping Service의 운영 작업 절차를 문서화합니다.

---

## 📚 Runbook 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [deployment.md](./deployment.md) | Shopping Service 배포 절차 | ✅ Current |
| [rollback.md](./rollback.md) | Shopping Service 롤백 절차 | ✅ Current |

---

## 🎯 사용 방법

### 배포 작업 시
1. [deployment.md](./deployment.md) 참조
2. 사전 조건 확인
3. 절차 순서대로 실행
4. 완료 확인 수행

### 롤백 필요 시
1. [rollback.md](./rollback.md) 참조
2. 롤백 사유 기록
3. 절차 순서대로 실행
4. 서비스 정상 동작 확인

---

## ⚠️ 주의사항

- **테스트 환경 먼저**: 가능하면 staging에서 먼저 테스트
- **백업 확인**: 배포 전 데이터베이스 백업 확인
- **롤백 준비**: 롤백 절차를 미리 숙지
- **문서 업데이트**: 절차 변경 시 문서 즉시 업데이트

---

## 📞 긴급 연락처

| 상황 | 담당자 | 연락처 |
|------|--------|--------|
| 배포 관련 문제 | DevOps Team | devops@example.com |
| 서비스 장애 | Backend Team | backend@example.com |
| 데이터베이스 이슈 | DBA Team | dba@example.com |

---

## 🔗 관련 문서

- [Architecture Overview](../architecture/README.md)
- [API Documentation](../api/README.md)
- [Troubleshooting Guide](../../../docs/troubleshooting/README.md)

---

**최종 업데이트**: 2026-01-18
