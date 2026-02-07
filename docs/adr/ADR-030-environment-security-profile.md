# ADR-030: Shopping Service 환경별 보안 프로파일 정책

**Status**: Proposed
**Date**: 2026-02-07
**Author**: Laze

## Context

Shopping Service의 환경별 설정 파일에서 보안 관련 이슈가 발견되었습니다.

1. **DB SSL 미사용 (S1)**: `application-local.yml`과 `application-kubernetes.yml` 모두 `useSSL=false`로 설정되어 있습니다. 로컬 개발 환경에서는 수용 가능하지만, Kubernetes(프로덕션 또는 스테이징) 환경에서 DB 통신이 평문으로 이루어지는 것은 보안 리스크입니다.

2. **allowPublicKeyRetrieval=true (S2)**: MySQL 커넥터의 RSA 공개키 자동 검색이 활성화되어 있습니다. MITM(Man-in-the-Middle) 공격 시 위조된 공개키를 받을 수 있으며, 이는 `useSSL=false`와 결합 시 비밀번호 노출로 이어질 수 있습니다.

3. **기본 비밀번호 사용 (S3)**: `${DB_PASSWORD:password}`로 환경변수 미설정 시 `password`가 기본값으로 사용됩니다. Kubernetes 환경에서도 동일한 패턴이 적용되어, Secret 미설정 시 기본 비밀번호로 접속됩니다.

이 세 가지 이슈는 모두 환경별 보안 수준 차등화가 없어 발생합니다. 모든 환경에서 동일한 (낮은) 보안 설정이 적용되고 있습니다.

## Decision

**환경별 3단계 보안 프로파일을 적용합니다: local(완화) / docker(중간) / k8s(강화).**

| 설정 | local | docker | k8s |
|------|-------|--------|-----|
| useSSL | false | false | true |
| allowPublicKeyRetrieval | true | true | false |
| DB password | 기본값 허용 | 환경변수 필수 | Secret 필수 |
| 비밀번호 기본값 | `password` | (없음, 실패) | (없음, 실패) |

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① local만 완화, 나머지 강화 (채택) | 개발 편의와 보안 균형 | docker도 약간의 설정 필요 |
| ② 전 환경 동일 보안 | 보안 정책 단순 | 로컬 개발 시 SSL 인증서 세팅 부담 |
| ③ k8s만 Vault 연동 | 최고 수준 보안 | Vault 인프라 추가 필요, 현 규모 대비 과도 |

## Rationale

- **보안 원칙 준수**: 프로덕션(k8s) 환경에서 평문 DB 통신과 기본 비밀번호는 기본적인 보안 원칙에 위배됩니다. OWASP Top 10의 A07(Identification and Authentication Failures)에 해당합니다.
- **개발 편의 유지**: 로컬 환경에서 SSL 인증서를 설정하는 것은 개발 생산성을 저해합니다. 로컬은 신뢰할 수 있는 네트워크이므로 완화 정책이 적합합니다.
- **점진적 강화**: Vault는 현재 인프라에 없으므로, 먼저 환경변수/Secret 기반으로 강화한 후, 필요 시 Vault를 추가 도입합니다.
- **실패 안전(Fail-secure)**: docker/k8s에서 환경변수 미설정 시 기본값 없이 실패하면, 잘못된 설정으로 기동되는 것을 방지합니다.

## Trade-offs

✅ **장점**:
- 프로덕션 환경의 DB 통신이 암호화되어 기본 보안 확보
- 기본 비밀번호로 프로덕션 기동 방지 (fail-secure)
- 환경별 명확한 보안 수준 문서화

⚠️ **단점 및 완화**:
- k8s 환경에서 SSL 인증서 관리 필요 → (완화: MySQL 서버 측 SSL 설정 + JDBC `sslMode=REQUIRED`로 서버 인증서만 사용)
- docker 환경도 환경변수 필수 → (완화: `docker-compose.yml`의 `environment` 섹션에 이미 설정 가능)
- 기존 k8s 매니페스트 수정 필요 → (완화: Kubernetes Secret 생성 + 환경변수 참조 추가)

## Implementation

### 1. application-kubernetes.yml 수정
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/shopping_db?useSSL=true&requireSSL=true&allowPublicKeyRetrieval=false
    username: ${DB_USERNAME}       # 기본값 제거
    password: ${DB_PASSWORD}       # 기본값 제거
```

### 2. application-docker.yml 수정
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/shopping_db?useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME}       # 기본값 제거
    password: ${DB_PASSWORD}       # 기본값 제거
```

### 3. application-local.yml (변경 없음)
- 기존 `useSSL=false`, `allowPublicKeyRetrieval=true`, 기본값 `password` 유지
- 로컬 개발 편의 우선

### 4. Kubernetes Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: shopping-db-credentials
type: Opaque
data:
  DB_USERNAME: <base64>
  DB_PASSWORD: <base64>
```

### 적용 범위
- 이 정책은 Shopping Service뿐 아니라 **모든 Spring Boot 서비스에 일괄 적용**합니다.
- 서비스별 `application-kubernetes.yml`을 동일 패턴으로 수정

### 코드 참조
- `application-local.yml:24` (useSSL=false, allowPublicKeyRetrieval=true)
- `application-kubernetes.yml:20` (useSSL=false, allowPublicKeyRetrieval=true)

## References

- [ADR-005: 민감 데이터 관리 전략](./ADR-005-sensitive-data-management.md)
- [ADR-010: 보안 강화 아키텍처](./ADR-010-security-enhancement-architecture.md)
- [MySQL Connector/J SSL Configuration](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-using-ssl.html)
- [OWASP A07:2021 - Identification and Authentication Failures](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
