# ADR-006: Config Service 및 Discovery Service 제거 - 로컬 설정 전환

## 상태
Accepted

## 날짜
2026-01-20

---

## 컨텍스트

Portal Universe 프로젝트는 Spring Cloud 기반으로 다음 서비스들을 사용하고 있었습니다:
- **Config Service**: 중앙화된 설정 관리
- **Discovery Service (Eureka)**: 서비스 디스커버리

### 기존 아키텍처

```
[Config Repo (Git)] <-- [Config Service] <-- [All Microservices]
                                                    |
                                                    v
                                          [Discovery Service (Eureka)]
```

### 문제점

**Config Service:**
1. **테스트 복잡성**: 설정 변경 시 매번 원격 Git 저장소에 push해야 테스트 가능
2. **로컬 개발 불편**: Config Service가 실행되어야만 다른 서비스 실행 가능
3. **배포 지연**: Config Service가 가장 먼저 준비되어야 함 (Init Container 대기)
4. **Kubernetes 중복**: ConfigMap과 Config Service가 동일한 역할 수행
5. **불필요한 네트워크 호출**: 서비스 시작 시 Config Service에서 설정 fetch
6. **Single Point of Failure**: Config Service 장애 시 전체 서비스 영향

**Discovery Service (Eureka):**
1. **Kubernetes DNS 중복**: Kubernetes는 자체 DNS 기반 서비스 디스커버리 제공
2. **로컬 환경 불필요**: 로컬 개발 시 직접 URL로 통신 가능
3. **추가 인프라 부담**: 별도 서비스 운영 필요
4. **헬스체크 중복**: Kubernetes가 자체적으로 헬스체크 수행

### 영향받는 서비스

- `api-gateway`
- `auth-service`
- `blog-service`
- `shopping-service`
- `notification-service`

## 결정

**Spring Cloud Config Service와 Discovery Service(Eureka)를 제거하고:**
1. 각 서비스에 설정 파일을 직접 포함
2. Kubernetes DNS를 사용하여 서비스 간 통신

### 새로운 아키텍처

```
[Service] --> [Internal application-{profile}.yml]
         --> [K8s ConfigMap] (환경 변수 오버라이드)
         --> [K8s Secret] (민감 정보)
         --> [K8s DNS] (서비스 디스커버리)
```

### 변경 사항

1. **설정 파일 구조** (각 서비스)
   ```
   src/main/resources/
   ├── application.yml              # 공통 기본 설정
   ├── application-local.yml        # 로컬 개발 환경
   ├── application-docker.yml       # Docker Compose 환경
   └── application-kubernetes.yml   # Kubernetes 환경
   ```

2. **의존성 제거**
   - `spring-cloud-starter-config` 제거 (5개 서비스)
   - `spring-cloud-starter-netflix-eureka-client` 제거 (5개 서비스)
   - `spring.config.import: configserver:...` 제거
   - Eureka 클라이언트 설정 제거

3. **Docker 이미지 단순화**
   - Config Service 대기 로직 제거
   - `ENTRYPOINT ["java", "-jar", "app.jar"]` 직접 실행

4. **Kubernetes 매니페스트**
   - `config-service.yaml` 삭제
   - `discovery-service.yaml` 삭제
   - Init Container (`wait-for-config`) 제거 (5개 서비스)
   - Kubernetes DNS 기반 서비스 URL 사용

5. **CI/CD 파이프라인**
   - GitHub Actions에서 config-service, discovery-service 빌드/배포 제거

## 대안 검토

### Config Service 대안

| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| **Config Service 유지** | 중앙화된 관리 | 테스트 복잡, 배포 지연, K8s 중복 | ❌ |
| **로컬 설정 파일** | 독립적 실행, 테스트 용이, K8s 호환 | 설정 분산 (완화 가능) | ✅ 채택 |
| **HashiCorp Consul** | 동적 설정, 서비스 디스커버리 통합 | 인프라 복잡도 증가 | ❌ |
| **Spring Cloud Kubernetes ConfigMap** | K8s 네이티브 | Spring Cloud 의존성 유지 필요 | 🟡 |

### Discovery Service 대안

| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| **Eureka 유지** | 클라이언트 사이드 로드밸런싱 | K8s DNS와 중복, 추가 인프라 | ❌ |
| **Kubernetes DNS** | K8s 네이티브, 추가 인프라 불필요 | 클라이언트 로드밸런싱 없음 | ✅ 채택 |
| **Istio Service Mesh** | 고급 트래픽 관리 | 복잡도 증가 | 🟡 향후 검토 |

## 결과

### 긍정적 영향

1. **테스트 용이성**: 로컬에서 즉시 설정 변경 및 테스트 가능
2. **독립적 실행**: 각 서비스가 Config Service 없이 독립적으로 실행
3. **빠른 배포**: Init Container 대기 시간 제거 (약 30-60초 단축)
4. **단순화된 아키텍처**: 불필요한 서비스 및 네트워크 호출 제거
5. **Kubernetes 호환성**: ConfigMap + Secret 방식으로 일관된 설정 관리
6. **장애 격리**: Config Service Single Point of Failure 제거

### 부정적 영향

1. **설정 분산**: 설정이 여러 서비스에 분산됨
   - **완화**: 프로필별 파일 구조로 일관성 유지
   - **완화**: ConfigMap으로 공통 환경 변수 중앙 관리

2. **동적 설정 새로고침 기능 상실**
   - **완화**: Kubernetes Rolling Update로 설정 변경 적용
   - **완화**: 대부분의 설정 변경은 재배포가 필요한 수준

### 수정된 파일

| 카테고리 | 파일 수 | 설명 |
|----------|---------|------|
| 설정 파일 생성 | 20 | 5개 서비스 × 4개 프로필 |
| Gradle 파일 | 6 | 의존성 제거 + settings.gradle |
| Dockerfile | 5 | 대기 로직 제거 |
| Docker Compose | 1 | config-service 정의 제거 |
| K8s 매니페스트 | 6 | config-service.yaml 삭제 + initContainers 제거 |
| 스크립트 | 4 | build, deploy, delete 스크립트 |
| GitHub Actions | 2 | ci.yml, docker.yml |
| 문서 | 5 | README, guides |

## 다음 단계

1. ✅ 각 서비스에 프로필별 설정 파일 마이그레이션
2. ✅ Gradle 의존성 제거
3. ✅ Dockerfile 단순화
4. ✅ Kubernetes 매니페스트 업데이트
5. ✅ CI/CD 파이프라인 업데이트
6. ✅ 문서 업데이트
7. 🔜 프로덕션 배포 및 모니터링

## 참고

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Kubernetes ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [12-Factor App: Config](https://12factor.net/config)

---

**작성자**: Claude Code
**검토자**: -
**최종 업데이트**: 2026-01-20
