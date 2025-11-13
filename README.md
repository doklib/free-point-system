# 무료 포인트 시스템 (Free Point System)

무료 포인트 관리 시스템입니다. 사용자에게 포인트를 적립하고, 주문 시 사용하며, 적립 및 사용을 취소할 수 있는 백엔드 API를 제공합니다.

## 기술 스택

- **Java**: 21
- **Spring Boot**: 3.2.0
- **Database**: H2 (In-Memory)
- **Build Tool**: Gradle 8.5
- **ORM**: Spring Data JPA

## 주요 기능

### 핵심 기능
- **포인트 적립**: 일반 적립 및 수기 지급 지원, 만료일 설정 가능
- **포인트 적립 취소**: 사용되지 않은 포인트 전액 취소
- **포인트 사용**: 우선순위 기반 차감 (수기 지급 > 만료일 순)
- **포인트 사용 취소**: 전체/부분 취소 지원, 만료된 포인트는 신규 적립
- **포인트 잔액 조회**: 총 잔액 및 사용 가능한 포인트 상세 정보
- **포인트 이력 조회**: 페이징 지원, 모든 트랜잭션 이력 추적

### 기술적 특징
- **멱등성 보장**: Idempotency-Key 헤더를 통한 중복 요청 방지 (24시간 TTL)
- **동시성 제어**: JPA 낙관적 잠금(@Version)을 통한 안전한 동시 처리
- **명확한 오류 처리**: 구조화된 오류 코드 및 컨텍스트 정보 제공
- **추적 가능성**: 모든 포인트 변경 이력을 1원 단위까지 추적
- **설정 기반 한도 관리**: 데이터베이스 기반 동적 설정 (재시작 불필요)

## 빌드 방법

### 사전 요구사항

- Java 21 이상 설치

### 빌드 명령어

```bash
# Unix/Linux/macOS
./gradlew build
```

빌드가 완료되면 `build/libs/` 디렉토리에 JAR 파일이 생성됩니다.

## 실행 방법

### 방법 1: Gradle을 통한 실행

```bash
# Unix/Linux/macOS
./gradlew bootRun
```

### 방법 2: JAR 파일 직접 실행

```bash
# 먼저 빌드
./gradlew build

# JAR 파일 실행
java -jar build/libs/free-point-system-0.0.1-SNAPSHOT.jar
```

애플리케이션이 시작되면 `http://localhost:8080`에서 접근할 수 있습니다.

## H2 데이터베이스 콘솔

애플리케이션 실행 중 H2 데이터베이스 콘솔에 접근할 수 있습니다.

- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:pointdb`
- **Username**: `sa`
- **Password**: (비어있음)

## API 엔드포인트

### 1. 포인트 적립
```http
POST /api/v1/points/earn
Headers:
  Idempotency-Key: {UUID}
  Content-Type: application/json

Body:
{
  "userId": "user123",
  "amount": 1000,
  "isManualGrant": false,
  "expirationDays": 365,
  "description": "회원가입 축하 포인트"
}
```

### 2. 포인트 적립 취소
```http
POST /api/v1/points/cancel-earn
Headers:
  Idempotency-Key: {UUID}
  Content-Type: application/json

Body:
{
  "pointKey": "A",
  "reason": "잘못된 지급"
}
```

### 3. 포인트 사용
```http
POST /api/v1/points/use
Headers:
  Idempotency-Key: {UUID}
  Content-Type: application/json

Body:
{
  "userId": "user123",
  "orderNumber": "ORDER-2024-001",
  "amount": 500
}
```

### 4. 포인트 사용 취소
```http
POST /api/v1/points/cancel-use
Headers:
  Idempotency-Key: {UUID}
  Content-Type: application/json

Body:
{
  "usePointKey": "B",
  "amount": 500,
  "reason": "주문 취소"
}
```

### 5. 포인트 잔액 조회
```http
GET /api/v1/points/balance/{userId}
```

### 6. 포인트 이력 조회
```http
GET /api/v1/points/history/{userId}?page=0&size=20
```

### 응답 예시

#### 성공 응답 (포인트 적립)
```json
{
  "pointKey": "A",
  "userId": "user123",
  "amount": 1000,
  "availableBalance": 1000,
  "totalBalance": 1000,
  "expirationDate": "2025-11-10T12:00:00",
  "isManualGrant": false,
  "createdAt": "2024-11-10T12:00:00"
}
```

#### 오류 응답 예시
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "errorCode": "INSUFFICIENT_POINT_BALANCE",
  "message": "사용 가능한 포인트가 부족합니다. 현재 잔액: 500, 요청 금액: 1000",
  "details": {
    "availableBalance": 500,
    "requestedAmount": 1000
  },
  "timestamp": "2024-11-10T12:00:00"
}
```

### 주요 오류 코드

| 오류 코드 | HTTP 상태 | 설명 |
|---------|---------|------|
| EXCEED_MAX_EARN_LIMIT | 400 | 1회 최대 적립 한도 초과 |
| EXCEED_USER_MAX_BALANCE | 400 | 개인별 최대 보유 한도 초과 |
| INSUFFICIENT_POINT_BALANCE | 400 | 사용 가능 포인트 부족 |
| POINT_KEY_NOT_FOUND | 404 | 포인트 키를 찾을 수 없음 |
| CANNOT_CANCEL_USED_POINT | 400 | 사용된 포인트는 취소 불가 |
| EXCEED_ORIGINAL_USE_AMOUNT | 400 | 원래 사용 금액 초과 |
| INVALID_EXPIRATION_DAYS | 400 | 유효하지 않은 만료일 |
| CONCURRENCY_CONFLICT | 409 | 동시성 충돌 발생 (재시도 가능) |
| INVALID_AMOUNT | 400 | 유효하지 않은 금액 |

## API 문서

애플리케이션 실행 후 Swagger UI를 통해 상세한 API 문서를 확인할 수 있습니다.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- https://www.uuidgenerator.net/

Swagger UI에서는 다음 기능을 제공합니다:
- 모든 API 엔드포인트 목록 및 상세 설명
- 요청/응답 스키마 확인
- 직접 API 테스트 (Try it out)
- 오류 코드 및 응답 예시

## 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests FreePointSystemApplicationTests

# 테스트 리포트 확인
# build/reports/tests/test/index.html
```

## 프로젝트 구조

```
free-point-system/
├── src/
│   ├── main/
│   │   ├── java/com/musinsa/point/
│   │   │   ├── config/           # 설정 클래스 (OpenAPI 등)
│   │   │   ├── controller/       # REST API 컨트롤러
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   │   ├── PointService.java
│   │   │   │   ├── IdempotencyService.java
│   │   │   │   └── ConfigService.java
│   │   │   ├── domain/           # JPA 엔티티
│   │   │   │   ├── PointTransaction.java
│   │   │   │   ├── PointLedger.java
│   │   │   │   ├── IdempotencyRecord.java
│   │   │   │   ├── SystemConfig.java
│   │   │   │   └── UserPointSummary.java
│   │   │   ├── repository/       # 데이터 접근 계층
│   │   │   ├── dto/              # 요청/응답 DTO
│   │   │   ├── exception/        # 커스텀 예외
│   │   │   ├── filter/           # 필터 (RequestIdFilter)
│   │   │   ├── util/             # 유틸리티 (PointKeyGenerator)
│   │   │   └── FreePointSystemApplication.java
│   │   └── resources/
│   │       ├── application.yml   # 애플리케이션 설정
│   │       └── data.sql          # 초기 데이터
│   └── test/
│       ├── java/com/musinsa/point/
│       │   └── integration/      # 통합 테스트
│       │       ├── PointScenarioIntegrationTest.java
│       │       ├── IdempotencyIntegrationTest.java
│       │       ├── ConcurrencyIntegrationTest.java
│       │       ├── ErrorScenarioIntegrationTest.java
│       │       └── PointPriorityIntegrationTest.java
│       └── resources/
│           └── application-test.yml
├── build.gradle                  # Gradle 빌드 설정
├── settings.gradle
└── README.md
```

## 요구사항 체크리스트

- [x] Spring Boot 3.x 프로젝트 생성
- [x] Java 21 사용
- [x] H2 데이터베이스 사용
- [x] Gradle 빌드 도구
- [x] 포인트 적립 API 구현
- [x] 포인트 적립 취소 API 구현
- [x] 포인트 사용 API 구현
- [x] 포인트 사용 취소 API 구현
- [x] 포인트 조회 API 구현 (잔액 조회, 이력 조회)
- [x] 멱등성 보장 (Idempotency-Key 헤더)
- [x] 동시성 제어 (낙관적 잠금 @Version)
- [x] 명확한 오류 메시지 (구조화된 ErrorResponse)
- [x] README.md 작성
- [x] ERD 작성 (src/main/resources/ERD.md, ERD_ko.md)
- [x] API 문서화 (Swagger/OpenAPI)
- [x] AWS 아키텍처 다이어그램 (src/main/resources/AWS-Architecture.md)
- [x] 통합 테스트 (PointScenarioIntegrationTest 등)
- [x] 동시성 테스트 (ConcurrencyIntegrationTest)
- [x] 멱등성 테스트 (IdempotencyIntegrationTest)
- [x] 오류 시나리오 테스트 (ErrorScenarioIntegrationTest)
- [x] 포인트 우선순위 테스트 (PointPriorityIntegrationTest)

## 설계 및 아키텍처

### 데이터베이스 스키마

시스템은 5개의 주요 테이블로 구성됩니다:

1. **point_transactions**: 모든 포인트 변경 이력 (적립, 사용, 취소)
2. **point_accounts**: 포인트 사용 시 어떤 적립에서 얼마씩 차감되었는지 추적
3. **idempotency_records**: 멱등성 키 관리 (24시간 TTL)
4. **system_configs**: 시스템 설정 (한도, 만료일 등)
5. **user_point_summaries**: 사용자별 포인트 잔액 집계



### 포인트 사용 우선순위

포인트 사용 시 다음 우선순위로 차감됩니다:

1. **수기 지급 포인트 우선** (isManualGrant = true)
2. **만료일이 짧은 순서**
3. **적립일이 빠른 순서**

### 포인트 사용 취소 로직

사용 취소 시 원래 사용된 적립을 역순으로 복구합니다:

- **만료되지 않은 포인트**: 원래 적립의 availableBalance 증가
- **만료된 포인트**: 신규 적립으로 처리 (365일 만료일)

### 시스템 설정

다음 설정은 `system_configs` 테이블에서 관리됩니다:

- `point.max.earn.per.transaction`: 1회 최대 적립 한도 (기본값: 100,000)
- `point.max.balance.per.user`: 개인별 최대 보유 한도 (기본값: 10,000,000)
- `point.default.expiration.days`: 기본 만료일 (기본값: 365일)
- `point.min.expiration.days`: 최소 만료일 (기본값: 1일)
- `point.max.expiration.days`: 최대 만료일 (기본값: 1,825일 / 5년)

## 개발 가이드

### 코드 스타일
- Java 21 기능 활용
- Lombok을 통한 보일러플레이트 코드 감소
- EARS 패턴 기반 요구사항 준수
- 명확한 오류 코드 및 메시지
- 레이어드 아키텍처 (Controller → Service → Repository)

### 커밋 메시지
- feat: 새로운 기능 추가
- fix: 버그 수정
- docs: 문서 수정
- refactor: 코드 리팩토링
- test: 테스트 코드 추가/수정


