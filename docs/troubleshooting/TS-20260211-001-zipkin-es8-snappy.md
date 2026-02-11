# TS-20260211-001: Zipkin 3.4.2 + Elasticsearch 8.18 Snappy 압축 호환 이슈

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

Zipkin 3.4.2가 Elasticsearch 8.18.5와 통신 시, ES가 HTTP 응답에 Snappy 압축을 적용하여 Zipkin의 JSON 파서가 압축된 바이너리를 파싱하지 못하는 호환성 이슈입니다.

**분석 과정**:

1. `docker logs zipkin` 확인 → 기본 로그 레벨에서는 에러 미표시
2. `curl http://localhost:9411/api/v2/services` → Snappy 바이너리와 ES root 응답 혼재 확인
3. `curl -X POST /api/v2/spans` → HTTP 202 반환 (비동기 수신이라 즉시 성공)
4. Elasticsearch 인덱스 확인 → zipkin 인덱스 미생성
5. `-e JAVA_OPTS="-Dlogging.level.zipkin2=DEBUG"` 로 재시작 후 진짜 에러 확인
6. `BaseVersion$Parser.convert()` 스택트레이스에서 Snappy magic bytes 확인
7. Zipkin 3.4.2는 Armeria HTTP 클라이언트 사용, `Accept-Encoding: snappy` advertise
8. Elasticsearch 8.18.5가 Snappy 압축 응답 반환
9. Zipkin의 `zipkin2.elasticsearch.BaseVersion$Parser`가 압축된 바이너리를 JSON으로 파싱 시도 → 실패

## 해결 방법 (Solution)

### 즉시 조치

```bash
# docker-compose-local.yml에서 Zipkin 버전 업그레이드
# openzipkin/zipkin:3.4.2 → openzipkin/zipkin:3.5.1

# 컨테이너 재시작
docker compose -f docker-compose-local.yml restart zipkin
```

### 영구 조치

- `docker-compose-local.yml`에서 Zipkin 이미지를 `openzipkin/zipkin:3.5.1`로 변경
- Zipkin 3.5.1은 Elasticsearch 7-8.x 공식 지원 및 Spring Boot 3.4.3 업데이트 포함
- 3.4.2에서 3.5.1로 업그레이드 시 breaking changes 없음 (유지보수 릴리스)
- 변경사항:
  - 3.5.1: Spring Boot 3.4.3 업데이트, 의존성 업데이트
  - 3.5.0: Apache Pulsar 지원 추가, CVE 수정
  - 3.4.4, 3.4.3: Spring Boot 및 의존성 업데이트

## 재발 방지 (Prevention)

- [x] Zipkin 버전을 3.5.1로 업그레이드
- [x] docker-compose.yml에서 명확한 버전 태그 사용 (`:3.5.1`)
- [ ] Zipkin health check 모니터링 알람 설정
- [ ] Zipkin DEBUG 로그 레벨을 기본으로 설정 (troubleshooting 용이성)
- [ ] Elasticsearch 버전 업그레이드 시 Zipkin 호환성 사전 검증 프로세스 추가
- [ ] Zipkin 릴리스 노트 모니터링 및 분기별 버전 업데이트 검토

## 학습 포인트

1. **Zipkin의 비동기 span 수신**: Zipkin은 span 수신(POST /api/v2/spans)을 비동기로 처리하므로 HTTP 202를 반환해도 실제 Elasticsearch 저장 성공을 보장하지 않습니다. span이 정상 수신되었는지 확인하려면 Elasticsearch 인덱스 또는 Zipkin UI에서 실제 데이터를 확인해야 합니다.

2. **로그 레벨의 중요성**: Zipkin 기본 로그 레벨에서는 Elasticsearch 통신 에러가 표시되지 않습니다. 문제 발생 시 `JAVA_OPTS="-Dlogging.level.zipkin2=DEBUG"` 환경변수를 설정하여 DEBUG 레벨 로그를 확인해야 합니다.

3. **압축 호환성 이슈**: Elasticsearch 버전 업그레이드 시 Snappy 등 응답 압축 방식 변경이 클라이언트(Zipkin, Logstash 등) 호환성에 영향을 줄 수 있습니다. 인프라 컴포넌트 업그레이드 시 클라이언트 라이브러리 버전 호환성을 사전에 검증해야 합니다.

4. **HTTP 클라이언트 헤더**: Armeria HTTP 클라이언트가 `Accept-Encoding: snappy`를 자동으로 advertise하므로, 서버가 Snappy 압축을 지원하면 자동으로 압축 응답을 받게 됩니다. 클라이언트가 압축을 올바르게 처리하지 못하면 파싱 에러가 발생합니다.

5. **버전 호환성 매트릭스**: Zipkin 3.x는 Elasticsearch 7-8.x 및 OpenSearch 2.x를 공식 지원합니다. 단, Elasticsearch 7.8+의 composable templates 사용 시 `ES_TEMPLATE_PRIORITY` 환경변수 설정이 필요할 수 있습니다.

## 관련 참조

- [Zipkin Releases](https://github.com/openzipkin/zipkin/releases) - 공식 릴리스 노트
- [Zipkin Elasticsearch README](https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/elasticsearch/README.md) - Elasticsearch 호환성 문서
- [GitHub Issue #3468](https://github.com/openzipkin/zipkin/issues/3468) - Elasticsearch 8.x 지원 관련 이슈
- [ADR-033: Polyglot Observability Strategy](../adr/ADR-033-polyglot-observability.md)

---

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
