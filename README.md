# Cloud Gate Service

RFID 기반 구역별 실시간 인원 추적 마이크로서비스.

ESP32 리더기로부터 NTAG 213 칩 태깅을 수신하여 입장/퇴장을 기록하고, 구역(스테이지)별 현재 인원수를 실시간으로 집계한다.

## 기술 스택

- Java 21 (Virtual Threads)
- Spring Boot 4.0.4
- PostgreSQL (RDS) — 태깅 로그 영속화
- Redis (ElastiCache) — 실시간 인원 카운트
- WebSocket (STOMP) — 실시간 인원 브로드캐스트
- gRPC — 내부 서비스 간 인원 조회
- Flyway — DB 마이그레이션

## 포트

| 프로토콜 | 포트 | 용도 |
|---------|------|------|
| HTTP | 8082 | REST API, WebSocket, Swagger |
| gRPC | 9094 | 내부 서비스 간 통신 |

## 로컬 개발 환경 설정

### 사전 요구사항

- JDK 21
- PostgreSQL
- Redis

### DB 생성

```sql
CREATE DATABASE cloudgate;
```

테이블은 Flyway가 서버 시작 시 자동 생성합니다.

### 환경변수

`.env.example`을 복사하여 `.env`를 생성하고 값을 채워 넣습니다.

```bash
cp .env.example .env
```

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/cloudgate` |
| `DB_USERNAME` | DB 사용자명 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `SERVER_PORT` | 서버 포트 | `8082` |
| `API_KEY` | MCU 인증용 API Key | |

### 실행

```bash
./gradlew bootRun
```

## API

### Swagger UI

서버 실행 후 접속: `http://localhost:8082/swagger-ui.html`

### REST 엔드포인트

#### 태깅

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/tags` | 태깅 수신 (MCU, `X-API-Key` 헤더 필요) |
| GET | `/api/v1/tags?stageId=&from=&to=&page=&size=` | 태깅 로그 조회 |

#### 구역 (Stage)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/stages` | 구역 생성 |
| GET | `/api/v1/stages` | 전체 구역 목록 |
| GET | `/api/v1/stages/{id}` | 구역 상세 |
| PUT | `/api/v1/stages/{id}` | 구역 수정 |
| DELETE | `/api/v1/stages/{id}` | 구역 삭제 |
| GET | `/api/v1/stages/{id}/occupancy` | 구역별 인원 조회 |
| GET | `/api/v1/stages/occupancy` | 전체 인원 조회 |

#### 리더기 (Reader)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/readers` | 리더기 등록 |
| GET | `/api/v1/readers` | 전체 리더기 목록 |
| GET | `/api/v1/readers/{id}` | 리더기 상세 |
| PATCH | `/api/v1/readers/{id}/activate` | 활성화 |
| PATCH | `/api/v1/readers/{id}/deactivate` | 비활성화 |
| DELETE | `/api/v1/readers/{id}` | 삭제 |

### WebSocket

| 엔드포인트 | 설명 |
|-----------|------|
| `/ws` | STOMP 연결 (SockJS) |
| `SUBSCRIBE /topic/occupancy/{stageId}` | 특정 구역 인원 실시간 수신 |
| `SUBSCRIBE /topic/occupancy` | 전체 구역 인원 변경 수신 |

### gRPC (포트 9094)

| RPC | 설명 |
|-----|------|
| `GetOccupancy(StageRequest)` | 특정 구역 인원 조회 |
| `GetAllOccupancy(Empty)` | 전체 구역 인원 조회 |

## 배포

### Docker 빌드

```bash
./gradlew bootJar
docker build -t cloud-gate-service .
```

### 개발 서버 배포

GitHub Actions `Deploy to Dev` 워크플로우를 수동 실행하면 ECR 푸시 후 EC2에 자동 배포됩니다.

## 설계 문서

상세 설계 결정사항 및 아키텍처는 [plan.md](./plan.md)를 참고하세요.
