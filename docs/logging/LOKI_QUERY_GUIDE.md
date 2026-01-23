# Loki 로그 조회 가이드

## 1. Grafana UI 사용법

### 접속
- URL: http://localhost:3000
- 경로: Explore → Loki

### 기본 쿼리 패턴

```logql
# 1. 전체 로그
{job="containerlogs"}

# 2. 텍스트 검색
{job="containerlogs"} |= "검색어"

# 3. 대소문자 무시 검색
{job="containerlogs"} |~ "(?i)error"

# 4. 정규식 검색
{job="containerlogs"} |~ "user.*logged in"

# 5. 여러 조건
{job="containerlogs"} |= "auth-service" |= "ERROR"
```

### JSON 로그 파싱 (Spring Boot 서비스)

백엔드 서비스들은 JSON 형식으로 로그를 출력합니다:

```logql
# 1. JSON 파싱 후 필드 검색
{job="containerlogs"}
| json
| level="ERROR"

# 2. 특정 logger만
{job="containerlogs"}
| json
| logger_name=~"com.portal.universe.*"

# 3. Trace ID로 추적
{job="containerlogs"}
| json
| trace_id="abc123"

# 4. 여러 필드 조합
{job="containerlogs"}
| json
| level="ERROR"
| logger_name=~".*AuthService.*"
```

### 로그 집계 (LogQL)

```logql
# 1. 시간당 로그 수
sum(rate({job="containerlogs"}[1h]))

# 2. 에러 로그 비율
sum(rate({job="containerlogs"} |= "ERROR"[5m]))

# 3. 서비스별 에러 수
sum by (service) (
  rate({job="containerlogs"} | json | level="ERROR"[5m])
)

# 4. Top 10 에러 메시지
topk(10,
  sum by (message) (
    rate({job="containerlogs"} |= "ERROR"[1h])
  )
)
```

---

## 2. LogCLI 명령줄 도구

### 설치

```bash
# macOS
brew install logcli

# 또는 직접 다운로드
curl -O -L "https://github.com/grafana/loki/releases/download/v2.9.0/logcli-darwin-amd64.zip"
unzip logcli-darwin-amd64.zip
chmod +x logcli-darwin-amd64
mv logcli-darwin-amd64 /usr/local/bin/logcli
```

### 환경 변수 설정

```bash
# ~/.zshrc 또는 ~/.bashrc에 추가
export LOKI_ADDR=http://localhost:3100
```

### 기본 사용법

```bash
# 1. 최근 로그 조회 (기본 30줄)
logcli query '{job="containerlogs"}'

# 2. 개수 지정
logcli query '{job="containerlogs"}' --limit=100

# 3. 시간 범위 지정
logcli query '{job="containerlogs"}' --since=1h

# 4. 텍스트 검색
logcli query '{job="containerlogs"} |= "ERROR"' --since=30m

# 5. JSON 출력
logcli query '{job="containerlogs"}' --output=jsonl

# 6. 실시간 tail (like tail -f)
logcli query '{job="containerlogs"}' --tail --since=1m
```

### 실전 예시

```bash
# Auth 서비스 에러 로그만
logcli query '{job="containerlogs"} |= "auth-service" |= "ERROR"' --since=1h

# 최근 5분간 전체 로그 실시간 모니터링
logcli query '{job="containerlogs"}' --tail --since=5m

# 특정 Trace ID 추적
logcli query '{job="containerlogs"} | json | trace_id="abc123"'

# 로그 수 집계
logcli stats '{job="containerlogs"}' --since=1h
```

---

## 3. Loki API 직접 호출

### cURL 사용

```bash
# 1. 로그 조회
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="containerlogs"}' \
  --data-urlencode 'limit=10' | jq .

# 2. 라벨 목록 조회
curl -s "http://localhost:3100/loki/api/v1/labels" | jq .

# 3. 특정 라벨의 값 조회
curl -s "http://localhost:3100/loki/api/v1/label/job/values" | jq .
```

### 스크립트 예시

```bash
#!/bin/bash
# logs-viewer.sh - 간단한 로그 조회 스크립트

LOKI_URL="http://localhost:3100"
QUERY="${1:-{job=\"containerlogs\"}}"
LIMIT="${2:-50}"

curl -G -s "$LOKI_URL/loki/api/v1/query_range" \
  --data-urlencode "query=$QUERY" \
  --data-urlencode "limit=$LIMIT" \
  | jq -r '.data.result[].values[][] | .[1]'
```

사용:
```bash
chmod +x logs-viewer.sh
./logs-viewer.sh '{job="containerlogs"} |= "ERROR"' 100
```

---

## 4. 자주 사용하는 쿼리 모음

### 서비스 상태 확인

```logql
# 1. 서비스 시작/종료 로그
{job="containerlogs"} |= "Started" or "Stopped"

# 2. 최근 에러 발생 서비스
sum by (service) (
  count_over_time({job="containerlogs"} | json | level="ERROR"[5m])
)

# 3. 응답 속도 느린 API
{job="containerlogs"}
| json
| duration > 1000
```

### 디버깅

```logql
# 1. 특정 사용자 추적
{job="containerlogs"} |= "userId=12345"

# 2. API 호출 흐름
{job="containerlogs"}
| json
| trace_id="abc123"

# 3. 데이터베이스 쿼리 에러
{job="containerlogs"} |= "SQL" |= "ERROR"
```

### 성능 모니터링

```logql
# 1. 분당 에러 발생률
rate({job="containerlogs"} |= "ERROR"[1m])

# 2. 서비스별 로그 생성 속도
sum by (service) (
  rate({job="containerlogs"}[5m])
)

# 3. 메모리 부족 경고
{job="containerlogs"} |= "OutOfMemoryError"
```

---

## 5. Grafana 대시보드 생성

### 패널 예시

#### 에러 로그 그래프
```logql
sum(rate({job="containerlogs"} | json | level="ERROR"[5m]))
```

#### 서비스별 로그 비율
```logql
sum by (service) (
  rate({job="containerlogs"}[5m])
)
```

#### 최근 에러 로그 테이블
```logql
{job="containerlogs"} | json | level="ERROR"
```

---

## 6. 유용한 팁

### 시간 범위 단축키 (Grafana)

- `Last 5 minutes`: 최근 5분
- `Last 15 minutes`: 최근 15분
- `Last 1 hour`: 최근 1시간
- `Last 6 hours`: 최근 6시간

### LogQL 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `\|=` | 포함 | `\|= "ERROR"` |
| `!=` | 미포함 | `!= "DEBUG"` |
| `\|~` | 정규식 매치 | `\|~ "user.*login"` |
| `!~` | 정규식 불일치 | `!~ "health.*check"` |

### JSON 필터링

```logql
# 중첩 JSON 필드 접근
{job="containerlogs"}
| json
| user_email="test@example.com"

# 숫자 비교
{job="containerlogs"}
| json
| response_time > 1000

# 조합
{job="containerlogs"}
| json
| level="ERROR"
| status_code=~"5.."
```

---

## 7. 문제 해결

### Loki가 로그를 수집하지 않을 때

```bash
# 1. Promtail 상태 확인
docker compose logs promtail --tail 50

# 2. Loki 상태 확인
curl -s http://localhost:3100/ready

# 3. 라벨 확인 (비어있으면 수집 안됨)
curl -s http://localhost:3100/loki/api/v1/labels | jq .
```

### 쿼리가 느릴 때

```logql
# 1. 시간 범위 줄이기
{job="containerlogs"} [5m] instead of [1h]

# 2. 라벨 필터 먼저 적용
{job="containerlogs", service="auth"} |= "ERROR"

# 3. Limit 사용
{job="containerlogs"} | limit 100
```

---

## 참고 자료

- Loki 공식 문서: https://grafana.com/docs/loki/latest/
- LogQL 문법: https://grafana.com/docs/loki/latest/logql/
- Grafana 대시보드: https://grafana.com/grafana/dashboards/
