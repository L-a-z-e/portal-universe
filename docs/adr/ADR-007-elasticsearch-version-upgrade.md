# ADR-007: Elasticsearch 8.18.5 버전 업그레이드

**Status**: Accepted
**Date**: 2026-01-19

## Context

Spring Boot 3.5.5가 관리하는 Elasticsearch Java 클라이언트(8.18.5)와 로컬 ES 서버(8.11.0) 버전 불일치로 런타임 오류가 발생했습니다. `NoSuchMethodError: activeShardsPercentAsNumber()` 및 `MissingRequiredPropertyException`이 발생하며 Actuator Health Check가 실패했습니다. build.gradle에서 명시적으로 `elasticsearch-java:8.11.0`을 지정했지만, Spring Boot Actuator는 8.18.x API를 기대하여 호환성 문제가 발생했습니다.

## Decision

**Elasticsearch 서버를 8.18.5로 업그레이드하고, build.gradle의 명시적 버전 지정을 제거하여 Spring Boot BOM 관리 버전을 따릅니다.**

## Rationale

- **호환성**: Spring Boot 관리 버전과 일치하여 Actuator Health Check 정상 작동
- **유지보수성**: 명시적 버전 고정 제거로 Spring Boot 업그레이드 시 자동으로 호환 버전 사용
- **일관성**: Local, Docker, Kubernetes 모든 환경에서 동일 버전 사용
- **최신 기능**: 보안 패치 및 버그 수정 자동 적용

## Trade-offs

✅ **장점**:
- Spring Boot BOM 관리로 의존성 충돌 방지
- Actuator ES Health Check 정상 작동
- 최신 보안 패치 적용
- Kubernetes StatefulSet 배포 파일 추가 (프로덕션 준비)

⚠️ **단점 및 완화**:
- 개발 환경 ES 데이터 재생성 필요 → (완화: 볼륨 삭제 후 재시작으로 간단히 해결)
- RangeQuery API 변경으로 코드 수정 필요 → (완화: 타입 안전성 향상된 API로 개선)

## Implementation

**주요 파일**:
- `docker-compose.yml` - ES/Kibana 이미지 8.11.0 → 8.18.5
- `integration-tests/docker-compose.test.yml` - ES-test 이미지 8.11.0 → 8.18.5
- `services/shopping-service/build.gradle` - `elasticsearch-java:8.11.0` 의존성 제거
- `application.yml` - ES Health Check 비활성화 설정 제거
- `k8s/infrastructure/elasticsearch.yaml` - **신규 생성** (K8s StatefulSet)

**API 변경 (RangeQuery)**:
```java
// Before (8.11.0)
.range(r -> r.field("price").gte(JsonData.of(minPrice)))

// After (8.18.x)
.range(r -> r.number(n -> n.field("price").gte(minPrice.doubleValue())))
```

**검증**:
```bash
curl http://localhost:9200  # "version.number": "8.18.5"
curl http://localhost:8083/actuator/health  # "elasticsearch": "UP"
```

## References

- [Elasticsearch Java Client Changelog](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/release-notes.html)
- [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/docs/3.5.5/reference/html/dependency-versions.html)

---

📂 상세: [old-docs/central/adr/ADR-007-elasticsearch-version-upgrade.md](../old-docs/central/adr/ADR-007-elasticsearch-version-upgrade.md)
