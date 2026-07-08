# AI 협업 규칙

이 문서는 포인트 기반 커피 주문 시스템에서 Human, ChatGPT, Codex, GitHub의 역할과 작업 흐름을 분리하기 위한 규칙이다.

핵심 원칙은 역할을 섞지 않는 것이다. Human은 최종 의사결정자이고, ChatGPT는 설계와 리뷰를 돕고, Codex는 정해진 범위 안에서 구현을 담당하며, GitHub는 작업 이력을 관리한다.

---

## 1. 역할 분리

### 1-1. Human 역할

- 최종 의사결정자
- 기능 요구사항 지시
- 설계 방향 검토
- 구현 결과 검토
- PR 최종 확인
- Merge 결정
- 과도한 구현 범위 통제
- 이해하지 못한 코드는 머지하지 않음

### 1-2. ChatGPT 역할

- PM / 설계자 / 리뷰어
- docs 확인
- README 확인
- 기존 코드 구조 확인
- Issue/PR 흐름 확인
- 구현 전 설계
- 작업 범위 분리
- 구현 우선순위 제안
- PR 리뷰
- 문서 검증
- 보안 검토
- 테스트 관점 검토
- 과도한 기술 도입 경고

### 1-3. Codex 역할

- 구현 담당자
- 설계에 따른 코드 구현
- 테스트 작성
- 빌드 실행
- 코드 정리
- 리뷰 반영
- Commit 작성
- Push
- PR 생성
- Issue 범위 밖 작업 금지

### 1-4. GitHub 역할

- Issue 관리
- Branch 관리
- Commit 기록
- Pull Request 관리
- 리뷰 기록
- 작업 이력 추적

---

## 2. 작업 흐름

1. Human 기능 지시
2. ChatGPT PM 설계
3. GitHub Issue 생성
4. Human 설계 검토
5. Codex 구현
6. Codex 자체 검증
7. Human 구현 검토
8. Commit / Push / PR
9. ChatGPT PR 리뷰
10. Codex 리뷰 반영
11. Human Merge

---

## 3. 현재 단계별 금지사항

### 3-1. v0에서 금지

- RabbitMQ 도입
- MySQL Replication 도입
- Redis Master-Replica 도입
- Kafka 도입
- 복잡한 인증/인가 도입
- 장바구니, 쿠폰, 주문 수량 기능 추가

v0의 목표는 필수 API 4개를 명확히 구현하는 것이다. 메시징, 복제, 고가용성 구성은 정합성 검증의 초점을 흐릴 수 있으므로 도전 과제로 분리한다.

### 3-2. v1에서 금지

- Redis 분산락을 먼저 도입
- 동시성 문제를 재현하지 않고 기술부터 적용
- 테스트 없이 정합성이 보장된다고 주장

v1의 목표는 동일 사용자 동시 주문 상황에서 DB 비관적 락으로 포인트 정합성을 검증하는 것이다. 보호 대상은 Redis key가 아니라 MySQL의 `point_wallet.balance`이다.

### 3-3. v2에서 금지

- Redis를 원본 랭킹 저장소로 사용
- 성능 측정 없이 캐시 적용했다고 주장
- 인덱스 근거 없이 무작정 추가
- TTL 정책 없이 캐시 적용

v2의 목표는 인기 메뉴 조회의 성능 개선이다. 정확한 원본은 MySQL `orders`이고, Redis는 조회 결과 캐시로만 사용한다.

### 3-4. v3에서 주의

- 포인트 충전/차감/잔액 검증은 Primary에서 처리
- Replica는 메뉴 조회, 인기 메뉴 조회 등 읽기 API에 제한 적용
- Redis 장애가 주문/결제에 영향을 주면 안 됨
- RabbitMQ 메시지는 DB commit 전에 발행하면 안 됨
- Outbox Pattern은 한계와 개선 방향으로 정리

v3는 제출 필수 범위가 아니라 도전 과제이다. MySQL Replication, Redis Master-Replica, RabbitMQ는 문제 상황과 검증 기준이 분명할 때만 단계적으로 적용한다.

---

## 4. 문서와 구현 기준

- README를 최상위 설계 기준으로 삼는다.
- README와 하위 문서가 충돌하면 README를 우선한다.
- Issue 범위에 없는 기능은 구현하지 않는다.
- 현재 단계에서 구현하지 않을 기술은 도전 과제 또는 제외 범위로 명확히 구분한다.
- 기술 선택 이유는 반드시 문제 상황과 연결해서 설명한다.
- "그냥 좋아서 사용했다"와 같은 근거 없는 표현은 사용하지 않는다.
- 이해하지 못한 코드는 머지하지 않는다.

---

## 5. 이번 Issue의 적용 범위

이번 Issue의 범위는 문서 작성이다.

작성 대상:

- `docs/api-spec.md`
- `docs/erd.md`
- `docs/agent-rules.md`

금지 대상:

- Entity 구현
- Controller 구현
- Service 구현
- Repository 구현
- Docker Compose 작성
- Redis 설정
- RabbitMQ 설정
- MySQL Replication 설정
- `build.gradle` 변경
- `application.properties` 변경

이번 문서 작업은 README의 설계 내용을 API 명세, ERD, AI 협업 규칙으로 분리하는 데 집중한다.
