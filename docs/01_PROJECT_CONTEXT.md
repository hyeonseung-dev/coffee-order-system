# Project Context

## 프로젝트명

다중 서버 환경을 고려한 포인트 기반 커피 주문 시스템

## 프로젝트 목적

이 프로젝트는 단순 CRUD 구현이 아니라, Java/Spring 백엔드 신입 포트폴리오에서 다음 문제를 실제 구현과 검증 근거로 설명하는 개인과제 프로젝트다.

- 데이터 정합성·트랜잭션
- 동시성 문제 재현과 제어
- SQL·인덱스·캐시 성능 개선
- 비동기 후속 처리와 전달 신뢰성
- MySQL 읽기·쓰기 분리와 복제 지연
- Redis 장애 전환과 DB fallback
- 테스트·장애 주입·문서화
- Human-in-the-loop AI-assisted 개발 프로세스

최종 설계 진입점은 [Software Architecture](00_SOFTWARE_ARCHITECTURE.md), 핵심 결과 요약은 [README](../README.md)를 따른다.

## 개발 단계와 최종 상태

### v0. 필수 API·트랜잭션 — 완료

- ACTIVE 메뉴 조회
- 포인트 충전과 `CHARGE` 이력
- 포인트 주문·결제와 `USE` 이력
- 최근 7일 인기 메뉴 TOP 3
- 주문 실패 시 잔액·이력·주문 전체 Rollback
- 주문 완료 이벤트의 초기 동기 처리 문제 재현

### v1. 동시성·정합성 — 완료

- #10에서 동일 사용자 동시 주문의 Lost Update 재현
- #11에서 Primary `point_wallet` 행에 `PESSIMISTIC_WRITE` 적용
- 10,000P·3,000P 메뉴·10개 동시 요청 결과 검증
  - 성공 3
  - 잔액 부족 7
  - 주문·USE 이력 각 3
  - 최종 잔액 1,000P

### v2. 인기 메뉴 성능 — 완료

- 실제 JPQL·LEFT JOIN·정렬 정책 검증
- 50만 건 격리 Fixture로 인덱스 후보 비교
- 최종 `(menu_id, ordered_at, status)` covering index 적용
- Redis Cache-Aside, KST 날짜 Key, TTL, Hit·Miss·장애 fallback
- K6로 MySQL 직접 조회와 Cache Hit를 상태 분리해 비교

### v3. 후속 처리·복제·가용성 — 도전 범위 구현 완료

#### Transactional Outbox

동기 호출 → Spring Event → AFTER_COMMIT → `@Async` 실험을 거쳐 현재 production 경로는 Transactional Outbox다.

- 주문과 Outbox `PENDING` 이벤트를 같은 DB 트랜잭션으로 저장
- 별도 Publisher의 성공·실패·재시도 상태 관리
- 재시도 시 동일 `eventId` 전달
- Exactly Once와 Consumer 멱등 저장소는 미구현

#### MySQL Primary·Replica

- 활성 readOnly 트랜잭션만 Replica
- 쓰기·비관적 락·트랜잭션 밖 조회는 Primary
- 실제 JPA Service·Repository 경로 라우팅 검증
- 비동기 복제 지연으로 stale read 재현
- 자동 DB Failover와 Replica 장애 HTTP 계약은 제외

#### Redis Master·Replica·Sentinel

- Master 1, Replica 1, Sentinel 3, quorum 2
- Master 장애 감지·Replica 승격·애플리케이션 재연결 검증
- Redis 전체 장애 중 인기 메뉴 MySQL fallback
- Redis 장애와 주문·포인트 기능 격리
- 무중단·데이터 무손실 보장은 하지 않음

RabbitMQ는 실제 구현하지 않았다. Outbox의 저장·재시도 한계를 해결할 후속 선택지로만 남긴다.

## 우선순위 원칙

1. 필수 기능과 데이터 의미를 먼저 확정한다.
2. 문제를 실제로 재현한다.
3. 최소 기술을 선택한다.
4. 테스트·DB·로그·장애 주입으로 검증한다.
5. 수치와 제한을 함께 문서화한다.
6. 도전 기술은 v0~v2가 완료된 뒤 확장한다.

## 기술 선택 기준

- 실무 필요성
- 유지보수성
- 협업 효율
- 금융권·대기업 환경에서 설명 가능한 정합성·감사 추적
- 취업 시장에서의 설명 가치
- 개인과제 시간 대비 효율

## 하지 않은 것

- MSA·Kubernetes
- Kafka·RabbitMQ 우선 도입
- Redis를 주문·랭킹 원본으로 사용
- Redis 분산락을 기본 해법으로 사용
- MySQL 자동 Failover·ProxySQL·HAProxy
- Redis Cluster·샤딩
- 과도한 CI/CD·모니터링·멀티에이전트 자동화
- Issue 범위 밖 리팩터링
- 검증하지 않은 성능·무중단·Exactly Once 주장
