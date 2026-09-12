# My Fave 백엔드 프로젝트 설계서

## 기술 스택

| 항목 | 선택 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.4.x |
| 빌드 도구 | Gradle (Groovy) |
| DB | PostgreSQL |
| ORM | JPA (Spring Data JPA) |
| 캐시/채팅 | Redis |
| WebSocket | Spring WebSocket + STOMP |
| 인증 | JWT (Access + Refresh Token) |
| 이메일 인증 | JavaMailSender + @Async + Redis (TTL 자동 만료) |
| 소셜 로그인 | 카카오 OAuth2 |
| 파일 업로드 | AWS S3 (이미지, GIF, 영상) |
| 결제 | 토스페이먼츠 / 카카오페이 / 네이버페이 (REST API) |
| API 문서화 | Swagger (SpringDoc OpenAPI) |
| 로깅 | Logback (Spring Boot 기본) |
| 테스트 | JUnit 5 + Mockito |
| 서버 포트 | 8080 |
| API Prefix | /api/v1 |

## 프로젝트 구조

```
myfave/
├── frontend/                          ← 프론트엔드 (비워둠)
├── backend/                           ← Spring Boot 프로젝트
│   ├── build.gradle
│   ├── settings.gradle
│   ├── Dockerfile
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/myfave/api/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   │       ├── request/
│   │   │   │   │   │       └── response/
│   │   │   │   │   ├── user/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── product/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── cart/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── order/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── payment/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── shipping/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── coupon/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── content/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── saleevent/
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   └── chat/
│   │   │   │   │       ├── controller/
│   │   │   │   │       ├── service/
│   │   │   │   │       ├── repository/
│   │   │   │   │       ├── entity/
│   │   │   │   │       └── dto/
│   │   │   │   ├── global/
│   │   │   │   │   ├── config/
│   │   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   │   ├── WebSocketConfig.java
│   │   │   │   │   │   ├── SwaggerConfig.java
│   │   │   │   │   │   ├── S3Config.java
│   │   │   │   │   │   ├── AsyncConfig.java
│   │   │   │   │   │   └── WebConfig.java
│   │   │   │   │   ├── common/
│   │   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   │   └── BaseEntity.java
│   │   │   │   │   ├── error/
│   │   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   │   ├── CustomException.java
│   │   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   │   ├── security/
│   │   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │   │   └── util/
│   │   │   │   └── MyfaveApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   │       └── java/com/myfave/api/
│   │           └── MyfaveApplicationTests.java
│   └── .gitignore
├── docker-compose.yml
└── README.md
```

## 각 레이어 역할

| 레이어 | 역할 | 예시 |
|---|---|---|
| controller/ | API 엔드포인트 정의, 요청 수신 및 응답 반환 | UserController.java |
| service/ | 비즈니스 로직 처리 | UserService.java |
| repository/ | DB 접근 (JPA Repository) | UserRepository.java |
| entity/ | DB 테이블과 매핑되는 JPA 엔티티 | User.java |
| dto/request/ | 클라이언트 → 서버 요청 데이터 | SignupRequest.java |
| dto/response/ | 서버 → 클라이언트 응답 데이터 | UserResponse.java |

## 의존성 목록 (build.gradle)

```groovy
dependencies {
    // Spring Boot 핵심
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // DB
    runtimeOnly 'org.postgresql:postgresql'

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // 이메일 인증 (JavaMailSender + @Async + Redis)
    implementation 'org.springframework.boot:spring-boot-starter-mail'

    // 카카오 소셜 로그인
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'

    // AWS S3 파일 업로드 (이미지, GIF, 영상)
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3:3.1.1'

    // 외부 결제 API 호출 (Toss, 카카오페이, 네이버페이)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Swagger (SpringDoc)
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

## 설정 파일

### application.yml (공통)

```yaml
spring:
  profiles:
    active: local

  jpa:
    hibernate:
      ddl-auto: validate   # 테이블은 DDL SQL로 직접 생성. JPA는 검증만 수행
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100

server:
  port: 8080
  servlet:
    context-path: /api/v1   # 모든 API: /api/v1/users, /api/v1/products 등

# 인플루언서 설정 (서비스 운영 인플루언서 1명 고정)
influencer:
  user-id: 1

# JWT 설정
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 1800000      # 30분 (ms)
  refresh-token-expiry: 1209600000  # 14일 (ms)
```

### application-local.yml (로컬 개발)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myfave
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: create-drop   # 로컬에서는 앱 시작 시 테이블 자동 재생성
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379

  # 카카오 소셜 로그인
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            scope: profile_nickname, account_email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

  # AWS S3
  cloud:
    aws:
      s3:
        bucket: ${S3_BUCKET_NAME}
      credentials:
        access-key: ${AWS_ACCESS_KEY}
        secret-key: ${AWS_SECRET_KEY}
      region:
        static: ap-northeast-2

springdoc:
  swagger-ui:
    path: /swagger-ui
    enabled: true
```

### application-prod.yml (운영)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: none   # 운영 환경에서는 JPA가 스키마에 절대 손대지 않음
    show-sql: false

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            scope: profile_nickname, account_email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

  cloud:
    aws:
      s3:
        bucket: ${S3_BUCKET_NAME}
      credentials:
        access-key: ${AWS_ACCESS_KEY}
        secret-key: ${AWS_SECRET_KEY}
      region:
        static: ap-northeast-2

springdoc:
  swagger-ui:
    enabled: false
```

## 공통 클래스

### ApiResponse (공통 응답 구조)

```java
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(200, message, null);
    }

    // GlobalExceptionHandler에서 에러 응답 생성 시 사용
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus(), errorCode.getMessage(), null);
    }
}
```

### BaseEntity (공통 시간 필드)

```java
// 모든 엔티티가 상속받는 공통 클래스
// createdAt, updatedAt을 JPA Auditing으로 자동 저장
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// 사용 예시
@Entity
public class User extends BaseEntity {
    // createdAt, updatedAt 자동 포함됨
}
```

> MyfaveApplication.java에 `@EnableJpaAuditing` 추가 필요

### ErrorCode (에러 코드 Enum)

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통
    COMMON_INVALID_INPUT(400, "입력값 유효성 검증 실패"),
    COMMON_INTERNAL_ERROR(500, "서버 내부 오류"),
    COMMON_METHOD_NOT_ALLOWED(405, "허용되지 않은 HTTP 메서드"),

    // 인증
    AUTH_UNAUTHORIZED(401, "인증 토큰 없음 또는 만료"),
    AUTH_FORBIDDEN(403, "접근 권한 없음"),
    AUTH_INVALID_CREDENTIALS(401, "아이디 또는 비밀번호 불일치"),
    AUTH_INVALID_REFRESH_TOKEN(401, "유효하지 않은 리프레시 토큰"),
    AUTH_EXPIRED_REFRESH_TOKEN(401, "만료된 리프레시 토큰"),
    AUTH_TOO_MANY_REQUESTS(429, "인증코드 발송 요청 초과"),
    AUTH_INVALID_VERIFICATION_CODE(400, "인증코드 불일치"),
    AUTH_EXPIRED_VERIFICATION_CODE(400, "인증코드 만료"),
    AUTH_PASSWORD_MISMATCH(400, "비밀번호 확인 불일치"),
    AUTH_INVALID_RESET_TOKEN(401, "유효하지 않은 재설정 토큰"),
    AUTH_INVALID_SOCIAL_CODE(400, "유효하지 않은 인가 코드"),
    AUTH_SOCIAL_PROVIDER_ERROR(502, "소셜 제공자 서버 오류"),

    // 사용자
    USER_NOT_FOUND(404, "회원 정보 없음"),
    USER_DUPLICATE_LOGIN_ID(409, "이미 존재하는 아이디"),
    USER_DUPLICATE_NICKNAME(409, "이미 존재하는 닉네임"),
    USER_DUPLICATE_PHONE(409, "이미 등록된 전화번호"),
    USER_DUPLICATE_EMAIL(409, "이미 등록된 이메일"),

    // 상품
    PRODUCT_NOT_FOUND(404, "존재하지 않는 상품"),
    PRODUCT_SOLD_OUT(409, "품절된 상품"),

    // 장바구니
    CART_ALREADY_EXISTS(409, "이미 장바구니에 있는 상품"),
    CART_ITEM_NOT_FOUND(404, "존재하지 않는 장바구니 항목"),

    // 주문
    ORDER_NOT_FOUND(404, "존재하지 않는 주문"),
    ORDER_INVALID_STATUS(409, "주문 상태 변경 불가"),
    ORDER_INVALID_ORDER_TYPE(400, "유효하지 않은 주문 유형"),

    // 결제
    PAYMENT_NOT_FOUND(404, "존재하지 않는 결제"),
    PAYMENT_ALREADY_DONE(409, "이미 완료된 결제"),
    PAYMENT_CANCELLED(409, "취소된 결제"),
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액 불일치"),
    PAYMENT_FAILED(502, "외부 결제 서비스 오류"),

    // 배송지
    SHIPPING_ADDRESS_NOT_FOUND(404, "존재하지 않는 배송지"),

    // 쿠폰
    COUPON_NOT_FOUND(404, "존재하지 않는 쿠폰"),
    COUPON_ALREADY_USED(409, "이미 사용된 쿠폰"),
    COUPON_EXPIRED(409, "만료된 쿠폰"),
    COUPON_TYPE_MISMATCH(409, "쿠폰 타입 불일치"),
    COUPON_MASTER_NOT_FOUND(404, "존재하지 않는 마스터 쿠폰"),
    COUPON_MASTER_INACTIVE(409, "비활성화된 마스터 쿠폰"),

    // 콘텐츠
    CONTENT_NOT_FOUND(404, "존재하지 않는 콘텐츠"),

    // 판매 이벤트
    SALE_EVENT_NOT_FOUND(404, "예정된 판매 이벤트 없음"),

    // 채팅
    CHAT_ROOM_NOT_FOUND(404, "현재 활성화된 채팅방 없음"),
    CHAT_ROOM_ALREADY_CLOSED(409, "이미 종료된 채팅방"),
    CHAT_RATE_LIMITED(429, "도배 방지 제한"),
    CHAT_INVALID_MESSAGE(400, "메시지 형식 오류"),
    CHAT_MESSAGE_TOO_LONG(400, "메시지 길이 초과"),

    // 파일 업로드
    FILE_UPLOAD_FAILED(500, "파일 업로드 실패"),
    FILE_INVALID_TYPE(400, "허용되지 않은 파일 형식"),
    FILE_SIZE_EXCEEDED(400, "파일 크기 초과"),
    ;

    private final int httpStatus;
    private final String message;
}
```

## Docker 설정

### Dockerfile

```dockerfile
# Stage 1: Build
FROM gradle:8.11-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
services:
  app:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: local
      DB_USERNAME: ${DB_USERNAME:-postgres}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/myfave
      SPRING_DATA_REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET}
      KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID}
      KAKAO_CLIENT_SECRET: ${KAKAO_CLIENT_SECRET}
      AWS_ACCESS_KEY: ${AWS_ACCESS_KEY}
      AWS_SECRET_KEY: ${AWS_SECRET_KEY}
      S3_BUCKET_NAME: ${S3_BUCKET_NAME}
      TOSS_SECRET_KEY: ${TOSS_SECRET_KEY}
      KAKAO_PAY_CID: ${KAKAO_PAY_CID}
      NAVER_PAY_CLIENT_ID: ${NAVER_PAY_CLIENT_ID}
      NAVER_PAY_CLIENT_SECRET: ${NAVER_PAY_CLIENT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - myfave-network

  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: myfave
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - myfave-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - myfave-network

volumes:
  postgres-data:
  redis-data:

networks:
  myfave-network:
    driver: bridge
```

### 환경변수 (.env.example)

```env
# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT (최소 32자 이상)
JWT_SECRET=your-secret-key-here-minimum-32-characters

# 카카오 소셜 로그인 (https://developers.kakao.com)
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret

# AWS S3 파일 업로드
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
S3_BUCKET_NAME=your-s3-bucket-name

# 결제 - 토스페이먼츠 (https://developers.tosspayments.com)
TOSS_SECRET_KEY=your-toss-secret-key

# 결제 - 카카오페이
KAKAO_PAY_CID=TC0ONETIME

# 결제 - 네이버페이
NAVER_PAY_CLIENT_ID=your-naverpay-client-id
NAVER_PAY_CLIENT_SECRET=your-naverpay-client-secret
```

## 인증 흐름

### 회원가입 / 로그인
```
이메일 + 비밀번호 → JWT (Access Token 30분 + Refresh Token 14일) 발급
```

### 비밀번호 재설정
```
1. 이메일 + 전화번호 교차 검증
2. 인증코드 이메일 발송 (JavaMailSender + @Async)
3. 인증코드 Redis 저장 (TTL: 5분 자동 만료)
4. 인증코드 확인 → 일회성 재설정 토큰 발급 (Redis 저장, TTL: 10분)
5. 재설정 토큰으로 비밀번호 변경
```

### 이메일 인증 구현 방식
- `JavaMailSender`: SMTP로 인증 메일 발송
- `@Async` + `AsyncConfig`: 메일 발송을 별도 스레드에서 처리 (응답 블로킹 방지)
- `Redis`: 인증코드 및 재설정 토큰 저장, TTL로 자동 만료

```java
// AsyncConfig.java — 비동기 스레드풀 설정
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}
```

> `MyfaveApplication.java`에 `@EnableAsync` 추가 필요 (또는 AsyncConfig에서 담당)

### application-local.yml 메일 설정
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}          # Gmail 앱 비밀번호 사용
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## Payment 도메인 연동 준비 사항

| PG사 | 연동 방식 | 주요 API |
|---|---|---|
| 토스페이먼츠 | REST API (WebClient) | 결제 승인: `POST /v1/payments/confirm` |
| 카카오페이 | REST API (WebClient) | 결제 준비: `POST /v1/payment/ready`, 승인: `POST /v1/payment/approve` |
| 네이버페이 | REST API (WebClient) | 결제 준비: `POST /naverpay/payments/v2.2/apply/payment` |

공통 흐름:
1. 프론트엔드 → 결제 준비 요청 → 백엔드 → PG사 API 호출
2. 사용자 결제 완료 → PG사 → 프론트엔드 콜백
3. 프론트엔드 → 결제 승인 요청 → 백엔드 → PG사 승인 API 호출
4. 결제 결과 DB 저장

## DB 스키마 관리

- 테이블은 **DDL SQL 파일로 직접 생성** (Flyway 미사용)
- `application-local.yml`: `ddl-auto: create-drop` → 로컬 개발 시 JPA가 테이블 자동 생성
- `application-prod.yml`: `ddl-auto: none` → 운영 서버에는 DDL SQL로 수동 적용
- 스키마 변경 시 DDL 파일과 Entity 클래스를 함께 수정

## API 버전 관리

- `server.servlet.context-path: /api/v1` 설정으로 모든 API에 자동 prefix 적용
- 예시: `GET /api/v1/products`, `POST /api/v1/orders`
- v2 필요 시: context-path를 `/api/v2`로 변경하거나 별도 브랜치로 운영

## Git 브랜치 전략

```
main          ← 배포용 (안정 버전)
├── develop   ← 개발 통합 브랜치
│   ├── feature/auth       ← 인증 기능 개발
│   ├── feature/user       ← 회원 기능 개발
│   ├── feature/product    ← 상품 기능 개발
│   ├── feature/cart       ← 장바구니 기능 개발
│   ├── feature/order      ← 주문 기능 개발
│   ├── feature/payment    ← 결제 기능 개발
│   ├── feature/shipping   ← 배송지 기능 개발
│   ├── feature/coupon     ← 쿠폰 기능 개발
│   ├── feature/content    ← 콘텐츠 기능 개발
│   ├── feature/saleevent  ← 판매 이벤트 개발
│   └── feature/chat       ← 채팅 기능 개발
```

각 팀원이 feature/도메인 브랜치에서 작업하고, 완료되면 develop에 PR(Pull Request)로 머지.
