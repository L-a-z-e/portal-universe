# Config Service Runbooks

> Config Service 운영 절차서 목록

---

## 📚 Runbook 목록

| ID | 제목 | 설명 | 상태 |
|----|------|------|------|
| [runbook-config-deployment](deployment.md) | Config Service 배포 절차 | 로컬/Docker/K8s 환경별 배포 가이드 | current |
| [runbook-config-incident-response](incident-response.md) | Config Service 장애 대응 절차 | 장애 진단 및 복구 가이드 | current |
| [runbook-config-refresh](config-refresh.md) | Config Service 설정 갱신 절차 | 무중단 설정 변경 가이드 | current |

---

## 🎯 Runbook 사용 가이드

### 1. 배포 시
👉 [deployment.md](deployment.md)
- 새로운 버전 배포
- 환경별 실행 방법
- 빌드 및 헬스체크

### 2. 장애 발생 시
👉 [incident-response.md](incident-response.md)
- 신속 진단 절차
- 장애 유형별 대응
- 에스컬레이션 연락처

### 3. 설정 변경 시
👉 [config-refresh.md](config-refresh.md)
- Git 저장소 설정 수정
- 무중단 설정 갱신 (Spring Cloud Bus)
- 단일/전체 서비스 갱신

---

## 📞 긴급 연락처

| 역할 | 담당 | 연락처 |
|------|------|--------|
| On-Call | DevOps 팀 | [Slack: #oncall] |
| Infrastructure | Infra 팀 | infra@example.com |
| Platform | Platform 팀 | platform@example.com |

---

## 🔗 관련 문서

- [Architecture](../architecture/README.md)
- [API Documentation](../api/README.md)
- [Troubleshooting](../troubleshooting/README.md)

---

**최종 업데이트**: 2026-01-18
