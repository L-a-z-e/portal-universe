# TS-20260211-001: Elasticsearch 8.18 HTTP Snappy 압축 → Zipkin 파싱 실패

**심각도**: 🟠 High
**상태**: Resolved
**영향 서비스**: Zipkin (Distributed Tracing)

| 항목 | 내용 |
|------|------|
| **발생일시** | 2026-02-11 |
| **해결일시** | 2026-02-11 |
| **담당자** | Laze |

## 증상 (Symptoms)

Zipkin UI에서 서비스 목록이 표시되지 않으며, span 데이터가 Elasticsearch에 저장되지 않는 문제가 발생했습니다.

- 에러 메시지: `IllegalArgumentException: .version.number not found in response: sNaPpY...`
- 영향 범위: 전체 서비스의 분산 추적 기능 중단, Zipkin UI에서 서비스 목록/trace 조회 불가
- Zipkin은 span을 수신(HTTP 202)하지만 Elasticsearch에 저장 실패
- Elasticsearch에 zipkin 인덱스가 생성되지 않음
- Zipkin health check가 unhealthy 상태

## 원인 (Root Cause)

Elasticsearch 8.18.5의 `http.compression` 기본값이 `true`이며, Zipkin이 사용하는 Armeria HTTP 클라이언트가 `Accept-Encoding: snappy`를 advertise합니다. ES가 이에 응답하여 Snappy 압축 데이터를 반환하지만, Armeria의 Snappy 디코더가 ES의 Snappy 포맷을 정상 처리하지 못해 Zipkin의 JSON 파서(`BaseVersion$Parser`)가 압축된 바이너리를 파싱하지 못합니다.

**핵심**: Zipkin 버전과 무관한 ES HTTP 압축 설정 이슈. Zipkin 3.4.2 → 3.5.1 업그레이드만으로는 해결되지 않음.

**분석 과정**:

1. `docker logs zipkin` 확인 → 기본 로그 레벨에서는 에러 미표시
2. `curl http://localhost:9411/api/v2/services` → Snappy 바이너리와 ES root 응답 혼재 확인
3. `curl -X POST /api/v2/spans` → HTTP 202 반환 (비동기 수신이라 즉시 성공)
4. Elasticsearch 인덱스 확인 → zipkin 인덱스 미생성
5. `-e JAVA_OPTS="-Dlogging.level.zipkin2=DEBUG"` 로 재시작 후 진짜 에러 확인
6. `BaseVersion$Parser.convert()` 스택트레이스에서 Snappy magic bytes(`sNaPpY`) 확인
7. Zipkin의 Armeria HTTP 클라이언트가 `Accept-Encoding: snappy`를 자동 advertise
8. ES 8.18.5가 `http.compression=true`(기본값)로 Snappy 압축 응답 반환
9. Armeria의 Snappy 디코더와 ES의 Snappy 포맷 비호환 → JSON 파싱 실패
10. **Zipkin 3.5.1 업그레이드 시도** → 동일 Armeria 클라이언트 사용으로 동일 이슈 지속
11. `curl -H "Accept-Encoding: snappy" http://localhost:9200/` 로 ES의 Snappy 응답 직접 확인
12. ES의 `http.compression=false` 설정으로 근본 원인 해결 확인

## 해결 방법 (Solution)

### 근본 조치: ES HTTP 압축 비활성화

`docker-compose-local.yml` 및 `docker-compose.yml`의 Elasticsearch 설정에 `http.compression=false` 추가:

```yaml
elasticsearch:
  environment:
    - http.compression=false  # Zipkin Armeria Snappy 호환 이슈 방지
```

```bash
# ES 컨테이너 재시작 (설정 반영)
docker compose -f docker-compose-local.yml restart elasticsearch
# ES healthy 후 Zipkin 재시작
docker compose -f docker-compose-local.yml restart zipkin
```

**검증:**
```bash
# ES가 더 이상 Snappy 압축을 반환하지 않는지 확인
curl -H "Accept-Encoding: snappy" http://localhost:9200/
# → plain JSON 응답이면 성공

# Zipkin 서비스 목록 확인
curl http://localhost:9411/api/v2/services
# → ["api-gateway","auth-service",...] JSON 배열이면 성공
```

### 부수 조치: Zipkin 버전 업그레이드

Zipkin 3.4.2 → 3.5.1로 업그레이드 (CVE 수정, 의존성 업데이트 목적. 단, 이 업그레이드만으로는 Snappy 이슈 미해결).

### 시도했으나 효과 없었던 방법

| 시도 | 결과 | 이유 |
|------|------|------|
| Zipkin 3.4.2 → 3.5.1 업그레이드 | 동일 에러 지속 | 동일 Armeria HTTP 클라이언트 사용 |
| ES `ES_HTTP_LOGGING=BASIC` 추가 | 근본 해결 아님 | 로그 레벨만 변경, 압축은 여전히 활성 |

## 재발 방지 (Prevention)

- [x] ES `http.compression=false` 설정 (docker-compose-local.yml, docker-compose.yml 모두)
- [x] Zipkin 버전을 3.5.1로 업그레이드
- [ ] ES 버전 업그레이드 시 `http.compression` 설정 재검증 (향후 Armeria 패치로 해결되면 `true`로 복원 검토)
- [ ] Zipkin health check 모니터링 알람 설정
- [ ] 인프라 컴포넌트 업그레이드 시 HTTP 압축 호환성 사전 검증 프로세스 추가

## 학습 포인트

1. **버전 업그레이드가 해결책이 아닐 수 있다**: Zipkin 3.5.1로 업그레이드했지만 동일한 Armeria HTTP 클라이언트를 사용하므로 문제가 지속되었습니다. 증상의 근본 원인(ES HTTP 압축)을 정확히 파악해야 올바른 해결책을 찾을 수 있습니다.

2. **`Accept-Encoding` 협상 함정**: Armeria HTTP 클라이언트가 `Accept-Encoding: snappy`를 자동 advertise합니다. ES가 이를 존중하여 Snappy 압축 응답을 보내지만, Armeria의 Snappy 디코더가 ES의 Snappy 구현과 호환되지 않습니다. 클라이언트-서버 간 HTTP content negotiation이 양쪽 모두 지원한다고 해서 실제 호환되는 것은 아닙니다.

3. **Zipkin의 비동기 span 수신**: POST /api/v2/spans는 HTTP 202를 반환해도 ES 저장 성공을 보장하지 않습니다. `curl -s http://localhost:9411/api/v2/services` 또는 Zipkin UI에서 실제 데이터를 확인해야 합니다.

4. **로그 레벨의 중요성**: Zipkin 기본 로그 레벨에서는 ES 통신 에러가 표시되지 않습니다. `JAVA_OPTS="-Dlogging.level.zipkin2=DEBUG"`를 설정해야 `BaseVersion$Parser` 에러를 확인할 수 있습니다.

5. **`http.compression=false`의 트레이드오프**: ES HTTP 압축을 비활성화하면 네트워크 대역폭 사용이 증가하지만, 로컬/Docker 환경에서는 무시할 수준입니다. 프로덕션에서 대량 데이터 전송이 필요한 경우 gzip만 허용하는 별도 설정을 검토할 수 있습니다.

## 관련 참조

- [Zipkin Releases](https://github.com/openzipkin/zipkin/releases) - 공식 릴리스 노트
- [Zipkin Elasticsearch README](https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/elasticsearch/README.md) - Elasticsearch 호환성 문서
- [GitHub Issue #3468](https://github.com/openzipkin/zipkin/issues/3468) - Elasticsearch 8.x 지원 관련 이슈
- [ADR-033: Polyglot Observability Strategy](../adr/ADR-033-polyglot-observability.md)

---

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
| 2026-02-11 | root cause 수정: Zipkin 버전 → ES http.compression 설정, 시도했으나 효과 없었던 방법 추가 | Laze |
