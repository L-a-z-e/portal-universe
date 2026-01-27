# 🔒 보안 강화 학습 가이드

> Portal Universe 프로젝트에 적용된 보안 강화 기법을 단계별로 학습합니다.

## 📚 학습 목차

### Phase 1: Critical Security (완료 ✅)

1. **[Rate Limiting](./01-rate-limiting.md)** ⭐⭐⭐
   - Token Bucket 알고리즘 이해
   - Redis 기반 분산 Rate Limiting
   - Brute Force 공격 방어

2. **[JWT Key Rotation](./02-jwt-key-rotation.md)** ⭐⭐⭐⭐
   - JWT 구조와 보안 이슈
   - Kid 기반 멀티 키 관리
   - 무중단 키 교체 전략

3. **[Login Security](./03-login-security.md)** ⭐⭐
   - 로그인 시도 추적
   - 점진적 계정 잠금
   - IP/Email 기반 차단

4. **[Security Audit Logging](./04-security-audit.md)** ⭐⭐⭐
   - AOP 기반 감사 로깅
   - 보안 이벤트 추적
   - 침입 탐지 기반 마련

### Phase 2: High Priority (완료 ✅)

5. **[Password Policy](./05-password-policy.md)** ⭐⭐
   - 비밀번호 복잡도 검증
   - 비밀번호 히스토리 관리
   - 만료 정책 구현

6. **[Input Validation & XSS Defense](./06-input-validation.md)** ⭐⭐⭐⭐
   - XSS 공격 원리와 방어
   - SQL Injection 차단
   - Bean Validation 활용

7. **[Security Headers](./07-security-headers.md)** ⭐⭐⭐
   - CSP (Content Security Policy)
   - HSTS (HTTP Strict Transport Security)
   - WebFlux 비동기 처리 이슈

---

## 🎯 학습 방법

### 1단계: 개념 이해
각 문서의 "왜 필요한가?" 섹션을 읽고 보안 위협을 이해합니다.

### 2단계: 구현 분석
실제 프로젝트 코드를 읽으며 구현 방법을 학습합니다.

### 3단계: 실습
"✍️ 실습 과제" 섹션의 문제를 직접 풀어봅니다.

### 4단계: 심화
"🔍 더 알아보기" 섹션에서 고급 주제를 탐구합니다.

---

## 📊 난이도 가이드

| 별 | 난이도 | 설명 |
|---|--------|------|
| ⭐⭐ | 기초 | 개념 이해와 간단한 구현 |
| ⭐⭐⭐ | 중급 | 실전 적용과 트러블슈팅 필요 |
| ⭐⭐⭐⭐ | 고급 | 깊은 이해와 설계 역량 필요 |

---

## 🔗 관련 리소스

### 프로젝트 문서
- [보안 강화 계획](../../security/security-enhancement-plan.md)
- [ADR-010: 보안 아키텍처](../../adr/ADR-010-security-enhancement-architecture.md)
- [구현 명세](../../guides/security-implementation-spec.md)

### 외부 리소스
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

## 💡 학습 팁

### DO
✅ 각 보안 기법이 어떤 공격을 방어하는지 이해하기
✅ 실제 프로젝트 코드를 직접 읽고 실행해보기
✅ 의도적으로 공격을 시도해보며 방어 확인하기
✅ 트레이드오프와 성능 영향 고려하기

### DON'T
❌ 코드만 복사하고 원리 이해 안하기
❌ 모든 기법을 한꺼번에 학습하려 하기
❌ 보안을 "나중에" 추가할 것으로 생각하기

---

## 🎓 선수 지식

- Java/Kotlin 기본 문법
- Spring Boot 기초 (Bean, DI, AOP)
- HTTP 프로토콜 이해
- 인증/인가 기본 개념

---

## 📝 학습 체크리스트

진행하면서 체크해보세요:

- [ ] Rate Limiting 원리 이해
- [ ] JWT 구조와 서명 검증 방법 이해
- [ ] XSS 공격 시연 및 방어 확인
- [ ] SQL Injection 공격 패턴 학습
- [ ] 비밀번호 정책 필요성 이해
- [ ] CSP와 HSTS 설정 방법 습득
- [ ] 보안 감사 로그 활용 방법 학습

---

## 🤝 피드백

질문이나 개선 제안이 있다면:
- 팀 Slack #security 채널
- GitHub Issues
- 시니어 개발자 1:1 세션

---

**다음**: [Rate Limiting 학습하기](./01-rate-limiting.md) →
