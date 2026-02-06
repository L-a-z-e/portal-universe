# Portal Universe - Sign-Up Flow Analysis & Design

## 1. 개요
본 문서는 Portal Universe의 회원가입(Sign-Up) 프로세스에 대한 분석 및 상세 설계를 다룹니다.
현재 Backend(`auth-service`)는 이미 회원가입 API가 구현되어 있으며, Frontend(`portal-shell`)에 UI 및 연동 로직을 추가해야 합니다.

## 2. 현황 분석

### 2.1 Backend (`auth-service`)
- **API Endpoint**: `POST /api/users/signup`
- **Controller**: `com.portal.universe.authservice.controller.UserController`
- **Entity**: `User` (Core Identity), `UserProfile` (Attribute) 분리되어 구현됨.
- **Entity Details**: `UserProfile` 엔티티에는 `phoneNumber`, `profileImageUrl` 필드가 이미 설계(`auth-system-design.md`)대로 구현되어 있으나, 가입 장벽을 낮추기 위해 회원가입 API에서는 제외됨 (Progressive Profiling 전략).
- **DTO**: `UserSignupRequest` (email, password, nickname, realName, marketingAgree)
- **Status**: ✅ 구현 완료 (설계 정합성 확인됨)

### 2.2 Frontend (`portal-shell`)
- **Routing**: `src/router/index.ts`에 회원가입 라우트 부재.
- **UI**: 회원가입 페이지(`SignupPage.vue`) 부재.
- **API Client**: `auth-service` 연동을 위한 API 호출 로직 필요.
- **Status**: 🚧 구현 필요

---

## 3. 상세 설계

### 3.1 User Interface (Frontend)
- **Path**: `/signup`
- **Component**: `views/SignupPage.vue`
- **Fields**:
  - 이메일 (Email) - 유효성 검사 필요
  - 비밀번호 (Password) - 복잡도 검사 필요
  - 닉네임 (Nickname)
  - 실명 (Real Name)
  - 마케팅 수신 동의 (Marketing Agreement) - Checkbox

### 3.2 Interaction Flow
1. 사용자가 `/signup` 페이지 접속
2. 정보 입력 및 "가입하기" 버튼 클릭
3. Frontend Validation 수행
4. API 호출: `POST /api/users/signup`
   - **Gateway URL**: `http://localhost:8080/api/users/signup` (또는 환경 변수 기반)
   - **Payload**:
     ```json
     {
       "email": "user@example.com",
       "password": "securePassword123!",
       "nickname": "Portaler",
       "realName": "Hong Gil Dong",
       "marketingAgree": true
     }
     ```
5. 성공 시: 로그인 페이지(`/login` 또는 `auth-service` 로그인 화면)로 리다이렉트
6. 실패 시: 에러 메시지 표시

### 3.3 Security Considerations
- 비밀번호는 HTTPS를 통해 전송되어야 함 (Production).
- Frontend에서 기본적인 유효성 검사 수행.
- Backend에서 2차 유효성 검사 및 중복 체크 수행.

---

## 4. Implementation Plan

### 4.1 Frontend (`portal-shell`)
1. **API Client 추가**: `src/api/auth.ts` 작성 (Axios 사용)
2. **View 생성**: `src/views/SignupPage.vue` 작성 (Tailwind CSS 활용)
3. **Route 등록**: `src/router/index.ts`에 `/signup` 추가

### 4.2 Backend (`auth-service`)
- 현행 유지 (이미 구현됨)
