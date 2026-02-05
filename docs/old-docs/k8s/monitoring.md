# Monitoring Stack Documentation

## 모니터링 스택 개요

Portal Universe 프로젝트는 다음 3가지 핵심 모니터링 도구를 사용합니다:

- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 메트릭 시각화 대시보드
- **Zipkin**: 분산 추적(Distributed Tracing)

---

## 1. Prometheus 설정

### 배포 구성

```yaml
image: prom/prometheus:v2.53.5
port: 9090
```

**접속 URL**: http://portal-universe:8080/prometheus

### 메트릭 수집 설정

Prometheus는 다음 간격으로 메트릭을 수집합니다:

- **scrape_interval**: 15초 (메트릭 수집 주기)
- **evaluation_interval**: 15초 (Rule 평가 주기)
- **kubernetes_sd_configs**를 통한 Pod 자동 발견

### 어노테이션 기반 수집

Pod에 아래 어노테이션을 추가하면 Prometheus가 자동으로 메트릭을 수집합니다:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "8081"
```

**예시**: Spring Boot Actuator의 Prometheus 엔드포인트를 노출하는 경우

---

## 2. Grafana 설정

### 배포 구성

```yaml
image: grafana/grafana-oss:11.4.0
port: 3000
```

**접속 URL**: http://portal-universe:8080/grafana

### 기본 계정

- **Username**: admin
- **Password**: admin (첫 로그인 후 변경 권장)

### 기본 플러그인

- `grafana-piechart-panel`: 파이 차트 시각화

### 데이터소스 연결

Prometheus를 데이터소스로 추가:

1. Grafana 로그인
2. Configuration > Data Sources
3. Add data source > Prometheus 선택
4. **URL**: `http://prometheus:9090`
5. Save & Test

---

## 3. Zipkin 설정

### 배포 구성

```yaml
image: openzipkin/zipkin:3.4.2
port: 9411
```

**접속 URL**: http://portal-universe:8080/zipkin

### 저장소

- **개발 환경**: In-memory (기본값)
- **운영 환경**: Elasticsearch 권장 (장기 데이터 보관)

### Health Check

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 9411
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /health
    port: 9411
  initialDelaySeconds: 20
  periodSeconds: 5
```

### 리소스 설정

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

---

## 서비스 연동

### Spring Boot 서비스에서 Zipkin 연동

`application.yml` 또는 `application.properties`에 다음 설정 추가:

```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0  # 100% 샘플링 (개발 환경)
```

**운영 환경**: `probability`를 0.1 ~ 0.2 (10~20%)로 조정하여 성능 영향 최소화

---

## 접속 정보 요약

| 도구 | 포트 | 접속 URL | 용도 |
|------|------|----------|------|
| Prometheus | 9090 | http://portal-universe:8080/prometheus | 메트릭 쿼리 |
| Grafana | 3000 | http://portal-universe:8080/grafana | 대시보드 시각화 |
| Zipkin | 9411 | http://portal-universe:8080/zipkin | 분산 추적 |

---

## 모니터링 대시보드 활용

### Grafana 대시보드 추천

1. **JVM (Micrometer)**: Spring Boot 애플리케이션 메트릭
2. **Kubernetes Cluster**: 클러스터 리소스 사용량
3. **Pod Monitoring**: 개별 Pod CPU/Memory 사용량

### Prometheus 쿼리 예시

```promql
# HTTP 요청 수
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# JVM 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# Pod CPU 사용률
sum(rate(container_cpu_usage_seconds_total[5m])) by (pod)
```

---

## 트러블슈팅

### Prometheus가 메트릭을 수집하지 못할 때

1. Pod 어노테이션 확인
2. Actuator 엔드포인트 활성화 확인: `/actuator/prometheus`
3. NetworkPolicy로 인한 접근 차단 확인

### Zipkin에 Trace가 표시되지 않을 때

1. Spring Cloud Sleuth 의존성 확인
2. `spring.zipkin.base-url` 설정 확인
3. Zipkin 서비스 Health Check 확인

---

## 참고 문서

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Zipkin Documentation](https://zipkin.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
