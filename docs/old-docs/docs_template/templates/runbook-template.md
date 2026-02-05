# [서비스/작업명] Runbook

**서비스**: [서비스명 또는 ALL]
**실행 주기**: Daily | Weekly | Monthly | On-Demand | Incident
**소요 시간**: ~[시간]
**권한**: Admin | DevOps | Developer

**실행 시점**:
- [정기: 매주 월요일 오전 9시]
- [트리거: 특정 알람 발생 시]
- [요청: 특정 요청이 들어왔을 때]

---

## 사전 확인사항

### 필수 권한
- [ ] [AWS/GCP 권한]
- [ ] [K8s 클러스터 접근]
- [ ] [데이터베이스 접근]

### 필수 도구
```bash
# 버전 확인
kubectl version
aws --version
```

### 백업 확인
- [ ] 최근 백업 존재 확인: `[명령어]`
- [ ] 롤백 계획 준비

---

## 실행 절차

### Phase 1: 준비
[무엇을 준비하는가]

```bash
# 현재 상태 확인
kubectl get pods -n [namespace]
```

**체크포인트**:
- [ ] 모든 Pod가 Running 상태
- [ ] 에러 로그 없음

---

### Phase 2: 메인 작업
[주요 작업 설명]

**⚠️ 주의**: [위험 요소 또는 주의사항]

```bash
# 단계 1
command1

# 단계 2
command2

# 단계 3
command3
```

**각 단계별 예상 결과**:
1. 단계 1: `[예상 출력]`
2. 단계 2: `[예상 출력]`
3. 단계 3: `[예상 출력]`

---

### Phase 3: 검증
[작업이 제대로 완료되었는지 확인]

```bash
# 상태 검증
verify-command
```

**성공 기준**:
- [ ] [기준 1]
- [ ] [기준 2]
- [ ] [기준 3]

---

### Phase 4: 모니터링
[작업 후 일정 시간 모니터링]

```bash
# 로그 확인 (실시간)
kubectl logs -f [pod-name] -n [namespace]
```

**모니터링 대상**:
- CPU/Memory 사용량
- 에러 로그
- API 응답 시간

**모니터링 기간**: [10분 | 30분 | 1시간]

---

## 롤백 절차

### 즉시 롤백이 필요한 경우
- [증상 1]: [예: 5xx 에러율 5% 초과]
- [증상 2]: [예: Pod 재시작 반복]

### 롤백 명령어
```bash
# 이전 버전으로 복구
kubectl rollout undo deployment/[name] -n [namespace]

# 또는 특정 리비전으로
kubectl rollout undo deployment/[name] --to-revision=[N]
```

**롤백 후 확인**:
```bash
kubectl rollout status deployment/[name] -n [namespace]
```

---

## 문제 상황 대응

### 시나리오 1: [문제 상황]
**증상**: [구체적 증상]

**즉시 조치**:
```bash
emergency-command
```

**근본 원인 확인**:
1. [확인 단계]
2. [확인 단계]

**영구 조치**: [Troubleshooting 링크](../troubleshooting/TS-XXX.md)

---

## 체크리스트

### 실행 전
- [ ] 백업 확인
- [ ] 권한 확인
- [ ] 동료에게 작업 공지 (Slack #ops 채널)
- [ ] 모니터링 대시보드 열어두기

### 실행 중
- [ ] 각 Phase 완료 후 체크포인트 확인
- [ ] 이상 징후 발견 시 즉시 중단

### 실행 후
- [ ] 검증 완료
- [ ] 모니터링 기간 대기
- [ ] 작업 완료 공지 (Slack #ops 채널)
- [ ] 작업 로그 기록 (문제 발생 시)

---

## 에스컬레이션

### 연락처
| 역할 | 이름 | Slack | 대응 시간 |
|------|------|-------|-----------|
| Primary | [이름] | @username | 평일 9-18시 |
| Secondary | [이름] | @username | 24/7 |
| Manager | [이름] | @username | 긴급 시 |

### 에스컬레이션 기준
- 🟡 **Warning**: [기준] → Primary 담당자
- 🟠 **Critical**: [기준] → Secondary 담당자
- 🔴 **Emergency**: [기준] → Manager + 전체 팀

---

## 관련 문서
- [서비스 아키텍처](../architecture/[service]/README.md)
- [배포 가이드](../guides/deployment/[service]-deploy.md)
- [Troubleshooting](../troubleshooting/)
- [Monitoring Dashboard](URL)
