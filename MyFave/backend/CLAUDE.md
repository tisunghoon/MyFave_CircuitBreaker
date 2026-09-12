# MyFave Backend Guide (Token-Optimized)

## 1. Backend Core Rules
- **Structure**: Controller-Service-Repository (Domain-driven)
- **Persistence**: PostgreSQL (JPA/Hibernate)
- **Validation**: `@Valid` (Request DTO), `ErrorCode` Enum (Business logic)

## 2. Critical Implementation Standards (Strict)
- **Timezone**: DB `TIMESTAMPTZ`, Java `OffsetDateTime` 필수 (`LocalDateTime` 금지).
- **Audit**: UPDATE 시 `updated_at` 수동 갱신 (JPA `@PreUpdate` 또는 Service 레이어).
- **Inheritance**: `CartItem`, `ChatRoom`, `OrderItem`, `ShortForm`, `StyleFeed`는 `BaseEntity`를 상속하지 않음 (`updated_at` 필드 없음).
- **Format**: 모든 응답은 `global/common/ApiResponse.java` 사용.

## 3. Build & Test Commands
- **Build**: `./gradlew build -x test`
- **Run**: `./gradlew bootRun`
- **Test**: `./gradlew test`
- **Clean**: `./gradlew clean`

## 4. Token Efficiency Strategy
- **File Discovery**: "관련 파일 찾아줘" 대신 `grep_search`를 사용하여 핵심 심볼을 먼저 검색한 뒤 `read_file` 하세요.
- **Directory Scope**: `domain/{도메인명}` 폴더 내부에서 작업 범위를 좁히세요.
- **Direct Reference**: `global/error/ErrorCode.java`, `global/common/ApiResponse.java`는 코드 생성 시 빈번하게 참조되므로 위치를 기억하세요.

## 5. Advanced Pattern: Saga (Payments)
- **Context**: 주문/결제 순환 참조 구조
- **Pattern**: 결제 실패 시 보상 트랜잭션(주문 상태 복구) 로직을 반드시 포함하세요.
- **Constraint**: `final_payment_id`는 결제 성공 완료 시점에만 UPDATE 하세요.
