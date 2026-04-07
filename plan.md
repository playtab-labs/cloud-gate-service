# Cloud Gate Service - 설계 계획서

## 1. 서비스 개요

RFID 기반 구역별 실시간 인원 추적 마이크로서비스.
외부 RFID 리더기(ESP32)로부터 NTAG 213 칩의 7바이트 UID를 수신하여 태깅 로그를 기록하고,
구역(스테이지)별 현재 인원수를 실시간으로 집계·제공한다.

---

## 2. 핵심 설계 결정사항

### 2.1 입장/퇴장 판별 방식

**입구/출구 분리 방식 채택** — 입구·출구에 각각 역할이 지정된 리더기를 배치한다.

| 방식                | 설명                                   | 장점               | 단점            |
|-------------------|--------------------------------------|------------------|---------------|
| 토글 방식             | 같은 리더기에서 첫 태그=입장, 재태그=퇴장             | 리더기 1대로 운영 가능    | 태그 누락 시 상태 꼬임 |
| **입구/출구 분리 (채택)** | 입구·출구에 각각 리더기 배치, 리더기에 방향(IN/OUT) 지정 | 정확도 높음, 서버 로직 단순 | 리더기 2배 필요     |

- Reader 엔티티에 `direction(IN/OUT)` 필드 추가하여 입장/퇴장을 명확히 구분
- 태그 수신 시 리더기의 direction으로 즉시 ENTER/EXIT 결정 → 상태 추적 불필요
- 타임아웃 자동 퇴장 없음 (공연 특성상 장시간 체류가 정상)

### 2.2 RFID 칩 및 일련번호 포맷

- **NFC 칩**: NTAG 213 (NFC Forum Type 2 Tag)
- **UID**: 7바이트 (56bit) → `String` hex 표현 (예: `"04A1B2C3D4E5F6"`, 14자리)
- DB 저장 시 `CHAR(14)`
- **MCU**: ESP32 DevKit V1

### 2.3 MCU 인증

- **API Key 방식 채택** — `X-API-Key` 헤더로 전송
- 전체 리더기 공용 키 1개 운영 (리더기 10대 이하 소규모 환경)
- API Key는 환경변수로 관리, 서버 측에서 검증

### 2.4 디바운싱

- ESP32 리더기 자체에서 3초 디바운싱 처리
- 백엔드에서는 별도 디바운싱 없음 (현장 이슈 발생 시 추가 검토)

### 2.5 통신 프로토콜

| 구간         | 프로토콜                  | 이유                       |
|------------|-----------------------|--------------------------|
| MCU → 서버   | **HTTP POST (REST)**  | MCU 구현 단순, Wi-Fi 모듈 호환성  |
| 서버 → 프론트엔드 | **WebSocket (STOMP)** | 실시간 인원수 push             |
| 서버 → 타 서비스 | **gRPC**              | 낮은 지연시간, Protobuf 타입 안정성 |

### 2.6 데이터 저장소

| 용도         | 저장소                     | 이유                                   |
|------------|-------------------------|--------------------------------------|
| 태깅 로그 (영구) | **PostgreSQL (RDS)**    | 정형 데이터, 쿼리 유연성. 서비스 전용 인스턴스          |
| 실시간 인원 카운트 | **Redis (ElastiCache)** | 빠른 읽기/쓰기, 원자적 증감 연산. 다른 서비스와 클러스터 공유 |

---

## 3. 기술 스택

- **Java 21 (LTS)** — Virtual Threads 활용, Record Patterns 등 최신 기능
- **Spring Boot 3.2+** — Virtual Threads 공식 지원
- **Spring Data JPA** — 태깅 로그 영속화
- **Spring Data Redis** — 실시간 카운트 캐시
- **Spring WebSocket (STOMP)** — 실시간 인원수 브로드캐스트
- **grpc-spring-boot-starter** — 내부 서비스 간 gRPC 통신
- **PostgreSQL (RDS)** — 메인 DB (서비스 전용)
- **Redis (ElastiCache)** — 인메모리 카운트 저장 (공유 클러스터)
- **Flyway** — DB 마이그레이션
- **Gradle (Kotlin DSL)** — 빌드 도구
- **Docker** — 컨테이너화, ECS 배포

---

## 4. 패키지 구조

```
com.playtab.cloudgate
├── config/                  # 설정 클래스
│   ├── WebSocketConfig      # STOMP WebSocket 설정
│   ├── RedisConfig          # Redis 연결 설정
│   ├── GrpcConfig           # gRPC 서버 설정
│   └── SecurityConfig       # (선택) API 인증 설정
│
├── domain/
│   ├── tag/                 # 태깅 도메인
│   │   ├── TagEvent         # 엔티티: 태깅 이벤트 로그
│   │   ├── TagEventType     # enum: ENTER, EXIT
│   │   └── TagEventRepository
│   │
│   ├── reader/              # 리더기 도메인
│   │   ├── Reader           # 엔티티: 리더기 정보
│   │   ├── ReaderDirection  # enum: IN, OUT
│   │   ├── ReaderRepository
│   │   └── ReaderStatus     # enum: ACTIVE, INACTIVE
│   │
│   └── stage/               # 스테이지(구역) 도메인
│       ├── Stage            # 엔티티: 구역 정보
│       └── StageRepository
│
├── api/
│   ├── TagController        # MCU → 서버 태깅 수신 REST API
│   ├── StageController      # 구역 관리 및 인원 조회 REST API
│   └── ReaderController     # 리더기 관리 REST API
│
├── grpc/
│   └── OccupancyGrpcService # 타 서비스용 gRPC 인원 조회
│
├── service/
│   ├── TagService           # 태깅 처리 비즈니스 로직
│   ├── OccupancyService     # 실시간 인원 집계 (Redis)
│
├── websocket/
│   └── OccupancyBroadcaster # 인원수 변경 시 WebSocket push
│
└── dto/
    ├── TagRequest           # MCU 요청 DTO
    ├── TagResponse
    ├── OccupancyResponse    # 구역별 인원수 응답
    └── StageRequest/Response
```

---

## 5. 핵심 엔티티 설계

### TagEvent (태깅 로그)

| 컬럼          | 타입          | 설명                    |
|-------------|-------------|-----------------------|
| id          | BIGINT (PK) | 자동 증가                 |
| chip_serial | CHAR(14)    | RFID 칩 UID (7바이트 hex) |
| reader_id   | BIGINT (FK) | 리더기 ID                |
| event_type  | VARCHAR     | ENTER / EXIT          |
| tagged_at   | TIMESTAMP   | 태깅 시각                 |

### Reader (리더기)

| 컬럼            | 타입          | 설명                |
|---------------|-------------|-------------------|
| id            | BIGINT (PK) | 자동 증가             |
| serial_number | VARCHAR     | 리더기 일련번호          |
| stage_id      | BIGINT (FK) | 소속 구역             |
| direction     | VARCHAR     | IN / OUT          |
| status        | VARCHAR     | ACTIVE / INACTIVE |

### Stage (구역/스테이지)

| 컬럼           | 타입          | 설명                    |
|--------------|-------------|-----------------------|
| id           | BIGINT (PK) | 자동 증가                 |
| name         | VARCHAR     | 구역명 (예: "Main Stage") |
| max_capacity | INT         | 최대 수용 인원 (선택)         |

---

## 6. API 설계

### 6.1 태깅 수신 (MCU → 서버)

```
POST /api/v1/tags
Content-Type: application/json

{
  "readerSerial": "R001",
  "chipSerial": "04A1B2C3D4E5F6"
}
```

- 리더기 일련번호로 리더기 조회 → `direction(IN/OUT)`으로 입장/퇴장 즉시 결정
- TagEvent 저장 (로그)
- Redis 인원 카운트 증감 (IN → INCR, OUT → DECR, 최소 0 보장)
- WebSocket으로 해당 구역 인원수 브로드캐스트

### 6.2 구역별 인원 조회

```
GET /api/v1/stages/{stageId}/occupancy

→ { "stageId": 1, "stageName": "Main Stage", "currentCount": 42, "maxCapacity": 100 }
```

```
GET /api/v1/stages/occupancy

→ [ { "stageId": 1, "stageName": "Main Stage", "currentCount": 42 }, ... ]
```

### 6.3 태깅 로그 조회

```
GET /api/v1/tags?stageId=1&from=2026-03-26T00:00:00&to=2026-03-26T23:59:59&page=0&size=20
```

### 6.4 WebSocket 구독 (실시간)

```
SUBSCRIBE /topic/occupancy/{stageId}

→ 메시지: { "stageId": 1, "currentCount": 43, "updatedAt": "..." }
```

```
SUBSCRIBE /topic/occupancy

→ 전체 구역 인원 변경 브로드캐스트
```

### 6.5 gRPC 서비스 (내부 서비스 간)

```protobuf
service OccupancyService {
  rpc GetOccupancy(StageRequest) returns (OccupancyResponse);
  rpc StreamOccupancy(StageRequest) returns (stream OccupancyResponse);
}
```

---

## 7. 핵심 비즈니스 로직 흐름

```
MCU 태그 수신
    │
    ▼
TagController.receiveTag(TagRequest)
    │
    ▼
TagService.processTag()
    ├── Reader 조회 (readerSerial → Reader)
    ├── Reader.direction으로 ENTER/EXIT 결정
    ├── TagEvent 저장 (로그)
    └── OccupancyService.update(stageId, eventType)
            ├── IN  → Redis INCR (cloudgate:stage:{id}:count)
            ├── OUT → Redis DECR (cloudgate:stage:{id}:count, 최소 0 보장)
            └── OccupancyBroadcaster.broadcast(stageId, newCount)
                    └── WebSocket STOMP → /topic/occupancy/{stageId}
```

## 8. Redis 키 설계

| 키 패턴                         | 타입          | 설명        |
|------------------------------|-------------|-----------|
| `cloudgate:stage:{id}:count` | String (숫자) | 구역 현재 인원수 |

- **네임스페이스 `cloudgate:` 접두사** 사용 — ElastiCache 공유 환경에서 다른 서비스 키와 충돌 방지
- 입구/출구 분리 방식이므로 칩별 상태 추적 키 불필요 (리더기 direction으로 판별)
- 서버 재시작 시 DB 기반으로 Redis 카운트 복구 (warm-up)

---

## 9. 인프라 구성

| 컴포넌트   | 서비스                   | 비고                         |
|--------|-----------------------|----------------------------|
| 애플리케이션 | **ECS (Docker)**      | Fargate 또는 EC2 launch type |
| 데이터베이스 | **RDS PostgreSQL**    | 서비스 전용 인스턴스                |
| 캐시     | **ElastiCache Redis** | 다른 서비스와 클러스터 공유            |

### 네트워크 (MCU → 서버)

- ESP32는 서강대 Wi-Fi 또는 별도 라우터를 통해 인터넷 접속
- **HTTPS 필수** — 공용/학교 Wi-Fi는 같은 네트워크의 다른 기기가 트래픽을 볼 수 있음 (패킷 스니핑). HTTP 평문이면 API Key와 요청 내용이 노출됨
- ESP32는 TLS 1.2를 지원하므로 HTTPS 통신에 문제 없음
- 별도 라우터 사용 시에도 WPA2 이상 설정 + HTTPS 유지 권장

---

## 10. 태깅 로그 보관 정책

- 서비스 레벨에서 자동 삭제/아카이빙 없음
- 행사 종료 후 수동 아카이빙 처리

---

## 11. 구현 순서 (권장)

1. **프로젝트 초기화** — Spring Initializr, Gradle 설정, 의존성 추가
2. **엔티티 & DB 마이그레이션** — Stage, Reader, TagEvent + Flyway 스크립트
3. **태깅 수신 API** — `POST /api/v1/tags` + TagService 핵심 로직
4. **Redis 인원 집계** — OccupancyService + 카운트 증감
5. **인원 조회 API** — `GET /api/v1/stages/occupancy`
6. **WebSocket 실시간 push** — STOMP 설정 + OccupancyBroadcaster
7. **리더기/구역 관리 API** — CRUD
8. **gRPC 서비스** — 내부 서비스용 인원 조회
9. **Redis warm-up** — 서버 시작 시 DB → Redis 동기화
10. **테스트 & 문서화** — 단위/통합 테스트, API 문서 (Swagger)

---

## 12. 고려사항 & 확장 포인트

- **리더기 헬스체크**: MCU에서 주기적 heartbeat → Reader 상태 갱신, 비활성 리더기 알림
- **모니터링**: Actuator + Prometheus + Grafana로 태깅 처리량, 지연시간 모니터링
- **이벤트 소싱**: 향후 Kafka 도입 시 태깅 이벤트를 토픽으로 발행하여 다른 서비스에서 구독 가능

---

## 13. 팔찌(Wristband) 관리 기능

### 13.1 기능 개요

행사용 RFID 팔찌를 관리하는 기능. 크게 3가지로 구성된다.

1. **팔찌 목록 벌크 등록** — CSV 파일로 전체 팔찌(RFID + 사용 가능 날짜) 일괄 등록
2. **팔찌 소유 연결** — BFF(GraphQL)에서 gRPC로 호출하여 identity id ↔ 팔찌 RFID 연결
3. **팔찌 조회** — 특정 identity id의 소유 팔찌 목록 반환 (gRPC)

### 13.2 엔티티 설계

#### Wristband (팔찌)

| 컬럼           | 타입            | 설명                              |
|--------------|---------------|---------------------------------|
| id           | BIGINT (PK)   | 자동 증가                           |
| rfid         | VARCHAR(14)   | RFID 시리얼 (NTAG 213, 7바이트 hex) UNIQUE |
| active_date  | DATE          | 사용 가능 날짜 (예: 2026-04-10)        |
| created_at   | TIMESTAMP     | 등록 시각                           |

#### WristbandOwnership (팔찌 소유 기록)

| 컬럼           | 타입            | 설명                                |
|--------------|---------------|-----------------------------------|
| id           | BIGINT (PK)   | 자동 증가                             |
| identity_id  | UUID          | 소유자 identity ID                   |
| wristband_id | BIGINT (FK)   | 팔찌 ID → wristband(id)             |
| linked_at    | TIMESTAMP     | 연결 시각                             |

**제약 조건:**
- `wristband_id` UNIQUE — 하나의 팔찌는 한 명만 소유 가능
- 인당 동일 날짜 팔찌 최대 1개
- 인당 총 팔찌 최대 2개
- 위 2가지 소유 제한은 애플리케이션 레벨에서 검증

**인덱스:**
- `idx_wristband_rfid` ON wristband(rfid)
- `idx_wristband_active_date` ON wristband(active_date)
- `idx_ownership_identity_id` ON wristband_ownership(identity_id)

### 13.3 DB 마이그레이션

`V2__wristband_schema.sql`:

```sql
CREATE TABLE wristband (
    id          BIGSERIAL PRIMARY KEY,
    rfid        VARCHAR(14) NOT NULL UNIQUE,
    active_date DATE        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE wristband_ownership (
    id           BIGSERIAL PRIMARY KEY,
    identity_id  UUID      NOT NULL,
    wristband_id BIGINT    NOT NULL UNIQUE REFERENCES wristband(id),
    linked_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_wristband_rfid ON wristband(rfid);
CREATE INDEX idx_wristband_active_date ON wristband(active_date);
CREATE INDEX idx_ownership_identity_id ON wristband_ownership(identity_id);
```

### 13.4 gRPC 서비스 설계

새 proto 파일 `wristband.proto`:

```protobuf
syntax = "proto3";

option java_package = "com.playtab.cloudgateservice.grpc";
option java_multiple_files = true;

package cloudgate;

service WristbandService {
  // 팔찌 RFID와 identity 연결 (BFF mutation → gRPC)
  rpc LinkWristband(LinkWristbandRequest) returns (LinkWristbandResponse);

  // identity의 소유 팔찌 목록 조회
  rpc GetMyWristbands(GetMyWristbandsRequest) returns (GetMyWristbandsResponse);
}

message LinkWristbandRequest {
  string rfid = 1;  // 팔찌 RFID 시리얼
  // identity_id는 gRPC 메타데이터 x-identity-id로 전달
}

message LinkWristbandResponse {
  string rfid = 1;
  string active_date = 2;       // yyyy-MM-dd
  string linked_at = 3;         // ISO 8601
}

message GetMyWristbandsRequest {
  // identity_id는 gRPC 메타데이터 x-identity-id로 전달
}

message WristbandInfo {
  string rfid = 1;
  string active_date = 2;       // yyyy-MM-dd
  string linked_at = 3;         // ISO 8601
}

message GetMyWristbandsResponse {
  repeated WristbandInfo wristbands = 1;
}
```

**gRPC 메타데이터 처리:**
- `x-identity-id` 키로 UUID 수신
- gRPC `ServerInterceptor`로 메타데이터에서 identity id를 추출하여 `Context`에 전파
- 서비스 메서드에서 `Context`를 통해 identity id 접근

### 13.5 팔찌 벌크 등록 (CSV)

**REST API:**

```
POST /api/v1/wristbands/bulk
Content-Type: multipart/form-data

file: wristbands.csv
```

**CSV 형식:**

```csv
rfid,active_date
04A1B2C3D4E5F6,2026-04-10
04B2C3D4E5F6A7,2026-04-10
04C3D4E5F6A7B8,2026-04-11
```

**처리 로직:**
1. CSV 파싱 → (rfid, active_date) 리스트 생성
2. RFID 형식 검증 (14자리 hex)
3. `saveAll`로 벌크 저장
4. 중복 RFID는 에러 반환

### 13.6 팔찌 소유 연결 비즈니스 로직

```
BFF (GraphQL mutation) → gRPC LinkWristband
    │
    ▼
WristbandGrpcService.linkWristband()
    ├── 1. 메타데이터에서 identity_id (UUID) 추출
    ├── 2. rfid로 Wristband 조회 → 없으면 NOT_FOUND
    ├── 3. 해당 팔찌가 이미 소유되었는지 확인 → 이미 소유됨이면 ALREADY_CLAIMED
    ├── 4. identity_id의 현재 소유 팔찌 수 확인 → 2개 이상이면 LIMIT_EXCEEDED
    ├── 5. identity_id가 같은 active_date 팔찌를 이미 소유하는지 확인 → 있으면 DUPLICATE_DATE
    ├── 6. WristbandOwnership 생성 & 저장
    └── 7. LinkWristbandResponse 반환
```

**gRPC 에러 코드:**

| 상황                    | gRPC Status       | 설명                    |
|-----------------------|-------------------|-----------------------|
| 팔찌 RFID 미존재           | NOT_FOUND         | 등록되지 않은 팔찌            |
| 팔찌 이미 다른 사람 소유        | ALREADY_EXISTS    | 이미 연결된 팔찌             |
| 인당 총 2개 초과            | FAILED_PRECONDITION | 최대 소유 수 초과            |
| 인당 동일 날짜 팔찌 중복        | FAILED_PRECONDITION | 같은 날짜 팔찌 이미 보유        |
| x-identity-id 누락      | UNAUTHENTICATED   | 메타데이터에 identity 없음    |

### 13.7 패키지 구조 (추가분)

```
com.playtab.cloudgateservice
├── domain/
│   └── wristband/
│       ├── Wristband.java                # 엔티티
│       ├── WristbandRepository.java
│       ├── WristbandOwnership.java       # 엔티티
│       └── WristbandOwnershipRepository.java
│
├── api/
│   └── WristbandController.java          # CSV 벌크 등록 REST API
│
├── grpc/
│   ├── WristbandGrpcService.java         # gRPC 서비스 구현
│   └── IdentityInterceptor.java          # x-identity-id 메타데이터 추출 인터셉터
│
└── service/
    └── WristbandService.java             # 비즈니스 로직
```

### 13.8 구현 순서

1. **DB 마이그레이션** — `V2__wristband_schema.sql` (wristband, wristband_ownership 테이블)
2. **엔티티 & 레포지토리** — Wristband, WristbandOwnership + JPA Repository
3. **CSV 벌크 등록 API** — `POST /api/v1/wristbands/bulk` + CSV 파싱
4. **Proto 정의** — `wristband.proto` (LinkWristband, GetMyWristbands)
5. **gRPC 인터셉터** — `IdentityInterceptor` (x-identity-id → Context 전파)
6. **gRPC 서비스** — `WristbandGrpcService` (연결 + 조회)
7. **비즈니스 로직** — `WristbandService` (소유 제한 검증 포함)
