# 암호화 개념 분석

## 개요

웹 애플리케이션에서 사용되는 암호화, 해싱, 서명 기술의 기본 개념과 Portal Universe 프로젝트에서의 활용 사례를 분석합니다.

---

## 1. 암호화 vs 해싱

### 1.1 기본 개념

```
암호화 (Encryption)
    원본 데이터 → [암호화 키] → 암호문
    암호문 → [복호화 키] → 원본 데이터
    특징: 양방향 변환 가능

해싱 (Hashing)
    원본 데이터 → [해시 함수] → 해시값
    특징: 일방향 변환 (역변환 불가능)
```

### 1.2 사용 사례

| 기술 | 용도 | 예시 |
|------|------|------|
| 암호화 | 데이터 보호 및 복원 필요 | DB 암호화, 통신 암호화 |
| 해싱 | 무결성 검증, 비밀번호 저장 | 비밀번호 저장, 파일 체크섬 |

---

## 2. 대칭 암호화 (Symmetric Encryption)

### 2.1 개념

```
송신자                               수신자
   │                                    │
   │  암호화 키 K를 사전 공유           │
   │◄──────────────────────────────────►│
   │                                    │
   │  평문 P                            │
   ▼                                    │
[ K로 암호화 ]                         │
   │                                    │
   │  암호문 C                          │
   ├───────────────────────────────────►│
                                        ▼
                                   [ K로 복호화 ]
                                        │
                                        ▼
                                     평문 P
```

### 2.2 특징

**장점:**
- 빠른 처리 속도
- 작은 키 크기 (128~256 bit)
- 대용량 데이터 처리 적합

**단점:**
- 키 공유 문제 (어떻게 안전하게 전달?)
- N명 통신 시 N(N-1)/2개의 키 필요

### 2.3 AES 암호화 예시

**파일**: `services/auth-service/src/main/java/com/laze/auth/infrastructure/security/AesEncryption.java`

```java
@Component
public class AesEncryption {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private final SecretKey secretKey;

    public AesEncryption(@Value("${security.aes.key}") String key) {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    // 암호화
    public String encrypt(String plainText) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // 복호화
    public String decrypt(String encryptedText) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted);
    }
}
```

---

## 3. 비대칭 암호화 (Asymmetric Encryption)

### 3.1 개념

```
수신자가 키 쌍 생성
    ├─ 공개키 (Public Key)  : 공개 가능
    └─ 개인키 (Private Key) : 비밀 유지

송신자                               수신자
   │                                    │
   │  공개키 요청                       │
   ├───────────────────────────────────►│
   │                                    │
   │  공개키 전송 (안전하지 않은 채널 OK)│
   │◄───────────────────────────────────┤
   │                                    │
   │  평문 P                            │
   ▼                                    │
[ 공개키로 암호화 ]                    │
   │                                    │
   │  암호문 C                          │
   ├───────────────────────────────────►│
                                        ▼
                                  [ 개인키로 복호화 ]
                                        │
                                        ▼
                                     평문 P
```

### 3.2 특징

**장점:**
- 키 공유 문제 해결 (공개키는 공개 가능)
- N명 통신 시 N개의 키 쌍만 필요
- 디지털 서명 가능

**단점:**
- 느린 처리 속도 (대칭 대비 100~1000배)
- 큰 키 크기 (2048~4096 bit)

### 3.3 RSA 암호화 예시

```java
// 키 쌍 생성
KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
generator.initialize(2048);
KeyPair keyPair = generator.generateKeyPair();

PublicKey publicKey = keyPair.getPublic();
PrivateKey privateKey = keyPair.getPrivate();

// 암호화 (공개키 사용)
Cipher cipher = Cipher.getInstance("RSA");
cipher.init(Cipher.ENCRYPT_MODE, publicKey);
byte[] encrypted = cipher.doFinal(plainText.getBytes());

// 복호화 (개인키 사용)
cipher.init(Cipher.DECRYPT_MODE, privateKey);
byte[] decrypted = cipher.doFinal(encrypted);
```

---

## 4. 하이브리드 암호화 (실무)

### 4.1 개념

```
실제 웹 통신 (TLS/HTTPS)의 방식

1단계: 대칭키 전달 (비대칭 암호화)
   Client                            Server
      │                                 │
      │  공개키 요청                    │
      ├────────────────────────────────►│
      │                                 │
      │  공개키 전송                    │
      │◄────────────────────────────────┤
      │                                 │
      │  대칭키 K 생성                  │
      ▼                                 │
 [ K를 공개키로 암호화 ]                │
      │                                 │
      │  암호화된 K 전송                │
      ├────────────────────────────────►│
                                        ▼
                                 [ 개인키로 K 복호화 ]

2단계: 데이터 전송 (대칭 암호화)
   Client                            Server
      │                                 │
      │  ◄─────── K로 암호화 ─────►    │
      │                                 │
      │  빠른 데이터 전송                │
      │◄───────────────────────────────►│
```

### 4.2 장점

1. **비대칭 암호화의 보안성** + **대칭 암호화의 속도**
2. 키 교환 안전성 + 대용량 데이터 처리
3. TLS/HTTPS의 표준 방식

---

## 5. BCrypt 해싱 알고리즘

### 5.1 해싱 vs 암호화

```
암호화 (Encryption)
    평문 → [키] → 암호문 → [키] → 평문
    복호화 가능 ✓

해싱 (Hashing)
    평문 → [해시 함수] → 해시값
    복호화 불가능 ✗

비밀번호는 왜 해싱?
    - 서버도 원본 비밀번호를 알 필요 없음
    - DB 유출 시에도 원본 노출 방지
    - 검증만 가능하면 충분 (입력값 해싱 → 저장된 해시와 비교)
```

### 5.2 BCrypt 동작 원리

**파일**: `services/auth-service/src/main/java/com/laze/auth/infrastructure/security/PasswordEncoderConfig.java`

```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);  // Cost Factor = 10
    }
}
```

### 5.3 BCrypt 특징

```
1. Salt 자동 생성
   평문: "password123"

   첫 번째 해싱:
   Salt: $2a$10$N9qo8uLOickgx2ZMRZoMye  (랜덤)
   결과: $2a$10$N9qo8uLOickgx2ZMRZoMye7.1s1Yb7ZzQ...

   두 번째 해싱:
   Salt: $2a$10$xK4jF9qp3LmN8vYqW6tAje  (다른 랜덤)
   결과: $2a$10$xK4jF9qp3LmN8vYqW6tAje8.2t2Zc8aZr...

   → 같은 비밀번호도 다른 해시값!

2. Cost Factor (작업량)
   Cost = 10 → 2^10 = 1024번 해싱 반복
   Cost = 12 → 2^12 = 4096번 해싱 반복

   → 브루트포스 공격 방어
   → 값이 클수록 안전하지만 느림
```

### 5.4 BCrypt 해시 구조

```
$2a$10$N9qo8uLOickgx2ZMRZoMye7.1s1Yb7ZzQ5vafBhQpEoF.y6P2.Bq
└┬┘└┬┘└──────────┬──────────┘└──────────┬──────────┘
 │  │          Salt (22자)         Hash (31자)
 │  │
 │  Cost Factor (10 = 2^10번 반복)
 │
 알고리즘 버전 (2a = BCrypt)
```

### 5.5 비밀번호 검증 흐름

**파일**: `services/auth-service/src/main/java/com/laze/auth/service/AuthServiceImpl.java`

```java
@Service
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;

    // 회원가입: 비밀번호 해싱
    public void signUp(SignUpRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
            .username(request.getUsername())
            .password(hashedPassword)  // 해시값 저장
            .build();

        userRepository.save(user);
    }

    // 로그인: 비밀번호 검증
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 입력 비밀번호를 해싱하여 비교
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 로그인 성공
        return generateToken(user);
    }
}
```

### 5.6 검증 과정 상세

```
DB 저장된 해시: $2a$10$N9qo8u...7.1s1Yb7ZzQ5vaf...
                       └─┬─┘
                       Salt 추출

사용자 입력: "password123"
    │
    ▼
Salt를 사용하여 동일한 방식으로 해싱
    │
    ▼
생성된 해시: $2a$10$N9qo8u...7.1s1Yb7ZzQ5vaf...
    │
    ▼
문자열 비교 (완전 일치?)
    │
    ├─ 일치 → 로그인 성공
    └─ 불일치 → 로그인 실패
```

---

## 6. JWT와 디지털 서명

### 6.1 JWT 구조

```
JWT = Header.Payload.Signature

eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.NHVaYe26MbtOYhSKkoKYdFVomg4i8ZJd8_-RU8VNbftc4TSMb4bXP3l3YlNWACwyXPGffz5aXHc6lty1Y2t4SWRqGteragsVdZufDn5BlnJl9pdR_kdVFUsra2rWKEofkZeIC4yWytE58sMIihvo9H1ScmmVwBcQP6XETqYd0aSHp1gOa9RdUPDvoXQ5oqygTqVtxaDr6wUFKrKItgBMzWIdNZ6y7O9E0DhEPTbE9rfBo6KTFsHAZnMg4k68CDp2woYIaXbmYTWcvbzIuHO7_37GT79XdIwkm95QJ7hYC9RiwrV7mesbY4PAahERJawntho0my942XheVLmGwLMBkQ
└────────────────┬────────────────┘ └───────────────────────────────────────────┬───────────────────────────────────────────┘ └──────────────────────────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────┘
            Header                                                    Payload                                                                                          Signature
```

### 6.2 Header (헤더)

```json
{
  "alg": "RS256",     // 서명 알고리즘
  "typ": "JWT"        // 토큰 타입
}
```

**Base64Url 인코딩**: `eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9`

### 6.3 Payload (페이로드)

```json
{
  "sub": "1234567890",        // Subject (사용자 ID)
  "name": "John Doe",
  "admin": true,
  "iat": 1516239022,          // Issued At (발급 시각)
  "exp": 1516242622           // Expiration (만료 시각)
}
```

**Base64Url 인코딩**: `eyJzdWIiOiIxMjM0NTY3ODkwIi...`

### 6.4 Signature (서명)

```
Signature = RSA_SHA256(
    Base64Url(Header) + "." + Base64Url(Payload),
    PrivateKey
)
```

---

## 7. RS256 서명 알고리즘

### 7.1 개념

```
RS256 = RSA (비대칭 암호화) + SHA-256 (해시)

서명 생성 (Auth Service)
    Header.Payload
         │
         ▼
    [ SHA-256 해싱 ]
         │
         ▼
      해시값
         │
         ▼
    [ RSA 개인키로 암호화 ]
         │
         ▼
      Signature

서명 검증 (Blog Service, Shopping Service)
    Signature                     Header.Payload
         │                              │
         ▼                              ▼
    [ RSA 공개키로 복호화 ]      [ SHA-256 해싱 ]
         │                              │
         ▼                              ▼
      해시값 A                        해시값 B
         │                              │
         └─────── 비교 (A == B?) ───────┘
                      │
                      ├─ 일치 → 서명 유효
                      └─ 불일치 → 서명 위조
```

### 7.2 RS256 구현

**파일**: `services/auth-service/src/main/java/com/laze/auth/infrastructure/security/JwtTokenProvider.java`

```java
@Component
public class JwtTokenProvider {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtTokenProvider(
        @Value("${jwt.private-key}") String privateKeyContent,
        @Value("${jwt.public-key}") String publicKeyContent
    ) throws Exception {
        this.privateKey = loadPrivateKey(privateKeyContent);
        this.publicKey = loadPublicKey(publicKeyContent);
    }

    // Access Token 생성 (개인키로 서명)
    public String generateAccessToken(String userId, List<String> roles) {
        return Jwts.builder()
            .setSubject(userId)
            .claim("roles", roles)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))  // 1시간
            .signWith(privateKey, SignatureAlgorithm.RS256)  // RS256 서명
            .compact();
    }

    // Token 검증 (공개키로 검증)
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(publicKey)  // 공개키로 검증
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
```

### 7.3 마이크로서비스 환경에서의 활용

```
┌────────────────────────────────────────────────────────────┐
│                     Auth Service                           │
│                                                            │
│  Private Key 보관 (절대 외부 노출 X)                      │
│  │                                                         │
│  │  사용자 로그인                                          │
│  ▼                                                         │
│ [JWT 생성 및 서명]                                         │
│  │                                                         │
│  └─► Access Token (signed with Private Key)               │
└────────────────────────────┬───────────────────────────────┘
                             │
                             │ Token 전달
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Blog Service  │    │Shopping Svc  │    │Notif. Service│
│              │    │              │    │              │
│Public Key 보관│    │Public Key 보관│    │Public Key 보관│
│              │    │              │    │              │
│[서명 검증]   │    │[서명 검증]   │    │[서명 검증]   │
│ ✓ 유효       │    │ ✓ 유효       │    │ ✓ 유효       │
└──────────────┘    └──────────────┘    └──────────────┘

장점:
1. 각 서비스는 Auth Service 호출 없이 독립적으로 검증
2. Private Key는 Auth Service만 보관 → 보안성 향상
3. Public Key 유출되어도 위조 불가능 (서명 생성 불가)
```

---

## 8. HS256 vs RS256 비교

### 8.1 HS256 (대칭키)

```
서명 생성                      서명 검증
    │                              │
    │   Secret Key (공유)          │
    │◄────────────────────────────►│
    │                              │
 [ HMAC-SHA256 ]              [ HMAC-SHA256 ]
    │                              │
    ▼                              ▼
 Signature                    Signature 검증
```

**특징:**
- 같은 키로 서명 생성/검증
- 빠른 처리 속도
- 키 공유 필요 (마이크로서비스에서 모든 서비스가 같은 키 보유)
- **위험**: 한 서비스가 해킹되면 전체 시스템 위조 가능

### 8.2 RS256 (비대칭키)

```
서명 생성                      서명 검증
    │                              │
Private Key                   Public Key
    │                              │
 [ RSA-SHA256 ]               [ RSA-SHA256 ]
    │                              │
    ▼                              ▼
 Signature                    Signature 검증
```

**특징:**
- Private Key로 서명, Public Key로 검증
- HS256보다 느림 (하지만 검증은 1회만)
- 키 공유 불필요 (Public Key만 배포)
- **안전**: 서비스 해킹되어도 위조 불가능 (Private Key 없음)

### 8.3 비교표

| 항목 | HS256 | RS256 |
|------|-------|-------|
| 키 타입 | 대칭키 (Secret Key) | 비대칭키 (Private/Public Key) |
| 서명 속도 | 빠름 | 느림 |
| 검증 속도 | 빠름 | 보통 |
| 키 관리 | 모든 서비스가 같은 키 보유 | Auth Service만 Private Key 보유 |
| 보안성 | 낮음 (키 유출 시 전체 위조) | 높음 (Public Key 유출해도 안전) |
| 적합한 환경 | 단일 애플리케이션 | 마이크로서비스, 분산 시스템 |

### 8.4 Portal Universe의 선택: RS256

```
이유:
1. 마이크로서비스 아키텍처 (Auth, Blog, Shopping, Notification)
2. Auth Service만 토큰 발급 권한 필요
3. 다른 서비스는 검증만 필요 (Public Key로 충분)
4. 보안성 우선 (속도 차이는 검증 시 1회만 발생)
```

---

## 9. OAuth2 Authorization Code + PKCE

### 9.1 기본 Authorization Code Flow

```
User                Browser               Frontend            Auth Server
 │                     │                     │                     │
 │  로그인 버튼 클릭   │                     │                     │
 │────────────────────►│                     │                     │
                       │  GET /authorize     │                     │
                       │────────────────────►│                     │
                       │                     │  GET /oauth/authorize?
                       │                     │    client_id=...    │
                       │                     │    redirect_uri=... │
                       │                     │    response_type=code
                       │                     ├────────────────────►│
                       │                     │                     │
                       │                     │  로그인 페이지      │
                       │◄────────────────────┼─────────────────────┤
 │  ID/PW 입력         │                     │                     │
 │────────────────────►│                     │                     │
                       │  POST /login        │                     │
                       ├─────────────────────┼────────────────────►│
                       │                     │                     │
                       │  Redirect: callback?code=ABC123          │
                       │◄────────────────────┼─────────────────────┤
                       │                     │                     │
                       │  GET /callback?code=ABC123               │
                       ├────────────────────►│                     │
                       │                     │  POST /oauth/token  │
                       │                     │    code=ABC123      │
                       │                     │    client_secret=...│
                       │                     ├────────────────────►│
                       │                     │                     │
                       │                     │  Access Token       │
                       │                     │◄────────────────────┤
```

**문제점:**
- Authorization Code를 가로채면 공격자가 토큰 발급 가능
- Public Client (SPA, Mobile)는 `client_secret` 안전하게 보관 불가

### 9.2 PKCE (Proof Key for Code Exchange)

```
PKCE = Authorization Code Flow + 동적 검증 키

Frontend                                     Auth Server
    │                                            │
    │  1. code_verifier 생성 (랜덤 문자열)       │
    ▼                                            │
code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    │                                            │
    │  2. code_challenge 생성                    │
    ▼                                            │
code_challenge = BASE64URL(SHA256(code_verifier))
               = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
    │                                            │
    │  3. Authorization 요청                     │
    │  GET /oauth/authorize?                     │
    │    code_challenge=E9Mel...                 │
    │    code_challenge_method=S256              │
    ├───────────────────────────────────────────►│
    │                                            │
    │                                      [code_challenge 저장]
    │                                            │
    │  4. Authorization Code 발급                │
    │  Redirect: callback?code=AUTH_CODE         │
    │◄───────────────────────────────────────────┤
    │                                            │
    │  5. Token 요청                             │
    │  POST /oauth/token                         │
    │    code=AUTH_CODE                          │
    │    code_verifier=dBjftJ...  (원본 전송)   │
    ├───────────────────────────────────────────►│
    │                                            │
    │                                      [검증]
    │                                      SHA256(code_verifier)
    │                                      == code_challenge?
    │                                            │
    │  6. Access Token 발급                      │
    │◄───────────────────────────────────────────┤
```

### 9.3 PKCE 핵심 개념

```
code_verifier (검증자)
    - 43~128자 랜덤 문자열
    - Frontend에만 보관 (절대 외부 노출 X)
    - 예: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

code_challenge (챌린지)
    - code_verifier를 SHA-256 해싱 후 Base64Url 인코딩
    - Authorization 요청 시 전송 (공개 가능)
    - 예: "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

검증 과정:
    1. Auth Server가 code_challenge 저장
    2. Token 요청 시 code_verifier 수신
    3. SHA256(code_verifier) == 저장된 code_challenge 비교
    4. 일치하면 Token 발급
```

### 9.4 공격 시나리오와 방어

#### 공격 시나리오: Authorization Code Interception

```
Without PKCE:

User                  Attacker              Frontend            Auth Server
 │                       │                     │                     │
 │                       │  [중간자 공격]      │                     │
 │                       │  Authorization Code │                     │
 │                       │  가로채기 ◄─────────┤                     │
 │                       │                     │                     │
 │                       │  POST /oauth/token  │                     │
 │                       │    code=ABC123      │                     │
 │                       │    client_secret=...│                     │
 │                       ├─────────────────────┼────────────────────►│
 │                       │                     │                     │
 │                       │  Access Token 획득! │                     │
 │                       │◄────────────────────┼─────────────────────┤
                         │
                         ▼
                    사용자 계정 탈취
```

#### PKCE로 방어

```
With PKCE:

User                  Attacker              Frontend            Auth Server
 │                       │                     │                     │
 │                       │  [중간자 공격]      │                     │
 │                       │  Authorization Code │                     │
 │                       │  가로채기 ◄─────────┤                     │
 │                       │                     │                     │
 │                       │  POST /oauth/token  │                     │
 │                       │    code=ABC123      │                     │
 │                       │    code_verifier=??? (모름!)              │
 │                       ├─────────────────────┼────────────────────►│
 │                       │                     │                     │
 │                       │                     │              [검증 실패]
 │                       │  401 Unauthorized   │                     │
 │                       │◄────────────────────┼─────────────────────┤
                         │
                         ▼
                    공격 실패!

이유:
- code_verifier는 Frontend 메모리에만 존재
- Attacker는 code_verifier를 모름
- Authorization Code만으로는 Token 발급 불가
```

### 9.5 PKCE 구현 예시

**파일**: `services/portal-shell/src/utils/pkce.ts`

```typescript
// 1. Code Verifier 생성
function generateCodeVerifier(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return base64UrlEncode(array);
}

// 2. Code Challenge 생성
async function generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(new Uint8Array(hash));
}

// 3. Authorization 요청
async function startAuthFlow() {
    const verifier = generateCodeVerifier();
    const challenge = await generateCodeChallenge(verifier);

    // 세션 스토리지에 verifier 저장
    sessionStorage.setItem('code_verifier', verifier);

    // Authorization 요청
    const authUrl = `${AUTH_SERVER}/oauth/authorize?` +
        `client_id=${CLIENT_ID}&` +
        `redirect_uri=${REDIRECT_URI}&` +
        `response_type=code&` +
        `code_challenge=${challenge}&` +
        `code_challenge_method=S256`;

    window.location.href = authUrl;
}

// 4. Token 요청 (Callback)
async function handleCallback(code: string) {
    const verifier = sessionStorage.getItem('code_verifier');

    const response = await fetch(`${AUTH_SERVER}/oauth/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: REDIRECT_URI,
            client_id: CLIENT_ID,
            code_verifier: verifier  // 원본 verifier 전송
        })
    });

    const { access_token } = await response.json();

    // 사용 후 verifier 삭제
    sessionStorage.removeItem('code_verifier');

    return access_token;
}
```

---

## 10. OAuth2 토큰 종류

### 10.1 Access Token

```
역할: API 리소스 접근 권한
수명: 짧음 (15분~1시간)
저장: 메모리 or sessionStorage
갱신: Refresh Token으로 재발급

예시:
{
  "sub": "user123",
  "roles": ["USER"],
  "iat": 1516239022,
  "exp": 1516242622  // 1시간 후
}
```

### 10.2 Refresh Token

```
역할: Access Token 재발급
수명: 길음 (7일~30일)
저장: httpOnly Cookie (XSS 방지)
갱신: 재로그인 필요

보안 특징:
- Frontend JavaScript에서 접근 불가
- CSRF 방어 필요
- Rotation 권장 (사용 시 새 Refresh Token 발급)
```

### 10.3 ID Token (OIDC)

```
역할: 사용자 신원 정보
프로토콜: OpenID Connect (OAuth2 확장)
형식: JWT

예시:
{
  "sub": "user123",
  "name": "John Doe",
  "email": "john@example.com",
  "email_verified": true,
  "picture": "https://...",
  "iat": 1516239022,
  "exp": 1516242622
}

용도:
- 사용자 프로필 표시
- SSO (Single Sign-On)
- 신원 확인
```

### 10.4 토큰 저장 위치 비교

| 토큰 | 저장 위치 | XSS 방어 | CSRF 방어 | 권장 사용 |
|------|----------|----------|-----------|-----------|
| Access Token | Memory | ✓ | ✓ | 단기 인증 |
| Access Token | sessionStorage | ✗ | ✓ | 새로고침 유지 |
| Refresh Token | httpOnly Cookie | ✓ | 추가 필요 | 장기 인증 |
| ID Token | Memory or sessionStorage | ✗ | ✓ | 사용자 정보 |

---

## 11. OAuth2 Scope

### 11.1 개념

```
Scope = 권한의 범위

예시:
    scope: "read:posts write:posts read:profile"

    read:posts    → 게시글 읽기 가능
    write:posts   → 게시글 작성 가능
    read:profile  → 프로필 읽기 가능
```

### 11.2 Access Token에 Scope 포함

```json
{
  "sub": "user123",
  "roles": ["USER"],
  "scope": "read:posts write:posts read:profile",
  "iat": 1516239022,
  "exp": 1516242622
}
```

### 11.3 API에서 Scope 검증

```java
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_write:posts')")  // Scope 검증
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request) {
        // write:posts 권한이 있는 경우만 실행
        return ResponseEntity.ok(postService.createPost(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read:posts')")
    public ResponseEntity<List<PostResponse>> getPosts() {
        // read:posts 권한이 있는 경우만 실행
        return ResponseEntity.ok(postService.getAllPosts());
    }
}
```

---

## 12. 실전 시나리오: Portal Universe

### 12.1 인증 흐름 전체 다이어그램

```
사용자                 Frontend              Gateway             Auth Service        Blog Service
  │                      │                      │                      │                     │
  │  1. 로그인 시도      │                      │                      │                     │
  ├─────────────────────►│                      │                      │                     │
  │                      │  2. Authorization    │                      │                     │
  │                      │     (PKCE)           │                      │                     │
  │                      ├──────────────────────┼─────────────────────►│                     │
  │                      │                      │                      │                     │
  │  3. 로그인 페이지    │                      │                      │                     │
  │◄─────────────────────┼──────────────────────┼──────────────────────┤                     │
  │                      │                      │                      │                     │
  │  4. ID/PW 입력       │                      │                      │                     │
  ├─────────────────────►│                      │                      │                     │
  │                      │  5. Token 요청       │                      │                     │
  │                      │     (code_verifier)  │                      │                     │
  │                      ├──────────────────────┼─────────────────────►│                     │
  │                      │                      │                      │  [BCrypt 검증]      │
  │                      │                      │                      │  [RS256 서명]       │
  │                      │                      │                      │                     │
  │  6. Access Token     │                      │                      │                     │
  │◄─────────────────────┼──────────────────────┼──────────────────────┤                     │
  │  (JWT with RS256)    │                      │                      │                     │
  │                      │                      │                      │                     │
  │  7. 블로그 글 작성   │                      │                      │                     │
  ├─────────────────────►│                      │                      │                     │
  │                      │  8. POST /api/posts  │                      │                     │
  │                      │     Authorization:   │                      │                     │
  │                      │     Bearer <token>   │                      │                     │
  │                      ├─────────────────────►│                      │                     │
  │                      │                      │  [Gateway Filter]    │                     │
  │                      │                      │  - JWT 검증          │                     │
  │                      │                      │  - RS256 공개키 사용 │                     │
  │                      │                      │                      │                     │
  │                      │                      │  9. 요청 전달         │                     │
  │                      │                      │     (User ID 포함)   │                     │
  │                      │                      ├──────────────────────┼────────────────────►│
  │                      │                      │                      │                     │
  │                      │                      │                      │              [비즈니스 로직]
  │                      │                      │                      │                     │
  │  10. 응답            │                      │                      │                     │
  │◄─────────────────────┼──────────────────────┼──────────────────────┼─────────────────────┤
```

### 12.2 사용된 보안 기술

| 단계 | 기술 | 목적 |
|------|------|------|
| 로그인 | PKCE | Authorization Code 가로채기 방지 |
| 비밀번호 저장 | BCrypt | 비밀번호 안전 저장 (Salt + Cost Factor) |
| 토큰 서명 | RS256 | 분산 환경에서 안전한 JWT 서명/검증 |
| 토큰 검증 | Public Key | Auth Service 호출 없이 독립적 검증 |
| API 보호 | Gateway Filter | 중앙 집중식 인증 처리 |

---

## 13. 핵심 요약

### 13.1 암호화 vs 해싱

| 항목 | 암호화 (Encryption) | 해싱 (Hashing) |
|------|---------------------|----------------|
| 방향성 | 양방향 (복호화 가능) | 일방향 (복호화 불가) |
| 키 | 필요 | 불필요 |
| 용도 | 데이터 보호 후 복원 | 무결성 검증, 비밀번호 |
| 예시 | AES, RSA | SHA-256, BCrypt |

### 13.2 대칭 vs 비대칭 암호화

| 항목 | 대칭 (AES) | 비대칭 (RSA) |
|------|-----------|--------------|
| 키 개수 | 1개 (공유) | 2개 (공개키/개인키) |
| 속도 | 빠름 | 느림 |
| 키 공유 | 어려움 | 쉬움 (공개키만 공개) |
| 용도 | 대용량 데이터 | 키 교환, 디지털 서명 |

### 13.3 BCrypt 핵심

1. **Salt 자동 생성**: 같은 비밀번호도 다른 해시
2. **Cost Factor**: 2^N번 반복으로 브루트포스 방어
3. **일방향**: 원본 비밀번호 복구 불가능
4. **검증**: 입력값 해싱 → 저장된 해시와 비교

### 13.4 JWT + RS256

```
장점:
1. Stateless: 서버에 세션 저장 불필요
2. 분산 환경: 공개키만 배포하면 독립적 검증
3. 보안: Private Key는 Auth Service만 보유

구조:
Header.Payload.Signature
└─ RS256 = RSA(Private Key) + SHA-256
```

### 13.5 PKCE 핵심

```
문제: Authorization Code 가로채기 공격
해결: code_verifier (비밀) + code_challenge (공개) 조합

흐름:
1. Frontend: verifier 생성 → challenge 생성 (SHA-256)
2. Auth Server: challenge 저장
3. Frontend: verifier 전송
4. Auth Server: SHA-256(verifier) == challenge 검증
```

### 13.6 마이크로서비스 보안 체크리스트

- [x] **비밀번호**: BCrypt (Cost 10 이상)
- [x] **JWT 서명**: RS256 (Private Key는 Auth만 보유)
- [x] **Public Client**: PKCE 적용
- [x] **Token 저장**: Access Token (메모리), Refresh Token (httpOnly Cookie)
- [x] **Gateway**: 중앙 집중식 인증 필터
- [x] **Scope**: API별 권한 세분화

---

## 14. 참고 자료

### 관련 파일

| 파일 | 경로 | 설명 |
|------|------|------|
| JwtTokenProvider | `auth-service/...security/JwtTokenProvider.java` | RS256 JWT 생성/검증 |
| PasswordEncoderConfig | `auth-service/...security/PasswordEncoderConfig.java` | BCrypt 설정 |
| AuthServiceImpl | `auth-service/service/AuthServiceImpl.java` | 로그인/회원가입 로직 |
| Gateway Filter | `gateway/filter/AuthenticationFilter.java` | JWT 검증 필터 |

### 관련 문서

- [ADR-005: JWT 인증 전략](../adr/ADR-005-jwt-strategy.md)
- [Architecture: 인증 시스템](../architecture/auth-system.md)
- [API: Auth Service](../api/auth-service-api.md)
