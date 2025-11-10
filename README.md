# 무료 포인트 시스템 (Free Point System)

무료 포인트 관리 시스템입니다. 사용자에게 포인트를 적립하고, 주문 시 사용하며, 적립 및 사용을 취소할 수 있는 백엔드 API를 제공합니다.

## 기술 스택

- **Java**: 21
- **Spring Boot**: 3.2.0
- **Database**: H2 (In-Memory)
- **Build Tool**: Gradle 8.5
- **ORM**: Spring Data JPA

## 주요 기능

- 포인트 적립 (수기 지급 포함)
- 포인트 적립 취소
- 포인트 사용 (우선순위: 수기 지급 > 만료일 순)
- 포인트 사용 취소 (부분 취소 지원)
- 포인트 잔액 조회
- 포인트 이력 조회
- 멱등성 보장 (Idempotency-Key)
- 동시성 제어 (낙관적 잠금)
- 명확한 오류 메시지

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
  "pointKey": "PT1699876543210001",
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
  "usePointKey": "PT1699876543210002",
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

## API 문서

애플리케이션 실행 후 Swagger UI를 통해 상세한 API 문서를 확인할 수 있습니다.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

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
│   │   │   ├── controller/      # REST API 컨트롤러
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   ├── domain/           # JPA 엔티티
│   │   │   ├── repository/       # 데이터 접근 계층
│   │   │   ├── dto/              # 요청/응답 DTO
│   │   │   ├── exception/        # 커스텀 예외
│   │   │   └── FreePointSystemApplication.java
│   │   └── resources/
│   │       ├── application.yml   # 애플리케이션 설정
│   │       └── data.sql          # 초기 데이터
│   └── test/
│       ├── java/com/musinsa/point/
│       └── resources/
│           └── application-test.yml
├── build.gradle                  # Gradle 빌드 설정
├── settings.gradle
└── README.md
```

## 과제 요구사항 체크리스트

### 필수 요구사항
- [x] Spring Boot 3.x 프로젝트 생성
- [x] Java 21 사용
- [x] H2 데이터베이스 사용
- [x] Gradle 빌드 도구
- [ ] 포인트 적립 API 구현
- [ ] 포인트 적립 취소 API 구현
- [ ] 포인트 사용 API 구현
- [ ] 포인트 사용 취소 API 구현
- [ ] 포인트 조회 API 구현
- [ ] 멱등성 보장
- [ ] 동시성 제어
- [ ] 명확한 오류 메시지
- [ ] README.md 작성
- [ ] ERD 작성
- [ ] API 문서화 (Swagger)

### 선택 요구사항
- [ ] AWS 아키텍처 다이어그램
- [ ] 통합 테스트
- [ ] 동시성 테스트

## 개발 가이드

### 코드 스타일
- Java 21 기능 활용
- Lombok을 통한 보일러플레이트 코드 감소
- EARS 패턴 기반 요구사항 준수
- 명확한 오류 코드 및 메시지

### 커밋 메시지
- feat: 새로운 기능 추가
- fix: 버그 수정
- docs: 문서 수정
- refactor: 코드 리팩토링
- test: 테스트 코드 추가/수정

