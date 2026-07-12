# Architecture Decision Records

ADR은 중요한 설계 선택과 그 근거를 남기는 문서다.

모든 Issue에 작성하지 않는다. 문서 수를 늘리는 것이 목적이 아니라, 나중에 다시 설명하거나 재검토할 가치가 있는 결정을 보존하는 것이 목적이다.

## ADR이 필요한 경우

다음 조건 중 하나에 해당할 때 작성한다.

- 둘 이상의 합리적인 대안이 존재한다.
- 선택 결과가 여러 기능, 계층, 데이터 흐름에 영향을 준다.
- 변경 비용이 크거나 되돌리기 어렵다.
- 트랜잭션, 동시성, 보안, 캐시, 메시징, 배포와 관련된다.
- 면접과 회고에서 왜 이 방식을 선택했는지 설명할 가치가 있다.

예시:

- 비관적 락과 낙관적 락
- Cache Aside와 다른 캐시 전략
- 이벤트 발행 시점
- Spring Event, RabbitMQ, Kafka 선택
- 세션과 JWT
- 주문과 결제의 트랜잭션 경계
- Read Replica 적용 방식

## ADR이 필요하지 않은 경우

- 기존 패턴을 그대로 따르는 CRUD
- DTO 필드 추가
- 단순 Validation
- 작은 버그 수정
- 테스트 누락 보완
- 선택지가 사실상 없는 구현

## 작성 시점

원칙적인 순서:

```text
Draft Issue
→ Issue Refinement
→ 중요한 선택 발견
→ ADR Proposed
→ Human + ChatGPT 검토
→ ADR Accepted
→ Issue READY
→ Codex 구현
```

구현 중 새로운 중요한 설계 선택이 발견되면 Codex가 작업을 멈추고 Troubleshooting Gate로 보고한다. Human과 ChatGPT가 ADR과 Issue를 갱신하고 Human이 재승인한 뒤 구현을 재개한다.

## 상태

- `Proposed`: 검토 중
- `Accepted`: 구현 기준으로 승인됨
- `Rejected`: 검토했지만 채택하지 않음
- `Superseded`: 다른 ADR로 대체됨

Codex는 Accepted 상태의 ADR만 구현 기준으로 사용한다.

## 파일명

```text
docs/adr/ADR-001-short-title.md
```

번호는 저장소 내에서 순차적으로 사용한다.

## 작성 원칙

- 배경과 문제를 먼저 설명한다.
- 고려한 대안을 실제로 비교한다.
- 선택 이유를 프로젝트 규모와 요구사항에 맞게 작성한다.
- 장점만 쓰지 않고 비용과 단점도 기록한다.
- 코드, DB, 테스트, 운영에 미치는 영향을 구분한다.
- 어떤 조건에서 다시 검토할지 작성한다.
- 구현 결과에 맞춰 과거 결정을 성공한 것처럼 바꾸지 않는다.

양식은 [ADR_TEMPLATE.md](ADR_TEMPLATE.md)를 사용한다.