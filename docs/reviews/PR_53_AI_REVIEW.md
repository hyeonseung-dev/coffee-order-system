# PR #53 AI Review Log

## 작업 내용

Issue #12에서 최근 7일 인기 메뉴 조회 쿼리의 인덱스를 실제 MySQL `EXPLAIN ANALYZE`와 50만 건 fixture로 비교했다.

초기 비교 대상은 다음과 같았다.

- 기준선: `(menu_id)`
- 후보 A: `(ordered_at, menu_id)`
- 후보 B: `(menu_id, status, ordered_at)`

격리된 benchmark 스키마에서 후보 B가 가장 빠른 결과를 보여 최종 인덱스로 선택됐다. 이후 Human이 구현 내용을 학습하는 과정에서 현재 도메인과 후보 구성이 충분히 맞물리는지 다시 검토했다.

## Human 지적

| 항목 | Human 지적 내용 | 현재 상태 |
|---|---|---|
| 최소 인덱스 후보 누락 | 모든 저장 주문의 상태가 `COMPLETED`인데 `(menu_id, status, ordered_at)`에서 `status`가 실제 탐색 범위를 줄이는지 의문을 제기했다. 더 작은 `(menu_id, ordered_at)` 후보를 비교하지 않고 후보 B를 최종안으로 확정한 것은 검증 공백이다. | 후속 비교 필요 |
| 실제 도메인과 fixture 일치 | 상태 선택도를 검증한다는 이유로 존재하지 않는 `FAILED`, `CANCELLED` 상태를 fixture에 임의로 넣기보다, 현재 `OrderStatus`와 주문 저장 규칙을 먼저 확인해야 한다고 지적했다. | 반영 |
| 결론 표현 과대화 | 현재 결과는 "비교한 후보 중 후보 B가 가장 빨랐다"는 의미이지, 가능한 최소·최적 인덱스 전체를 검증했다는 의미는 아니다. | 문서·PR 결론 갱신 필요 |

## AI 리뷰에서 놓친 부분

1. ChatGPT와 Codex는 후보 B가 실제 쿼리 순서와 맞고 측정값도 개선됐다는 점에 집중했다.
2. 그러나 현재 `OrderStatus`가 `COMPLETED` 하나뿐이라는 도메인 사실과, fixture 50만 건도 모두 `COMPLETED`라는 사실을 최종 후보 최소성 검토에 연결하지 못했다.
3. `(menu_id, ordered_at)`처럼 더 적은 컬럼으로 동일하거나 더 좋은 효과를 낼 수 있는 후보를 비교 목록에 포함하지 않았다.
4. Human이 상태 분포 문제를 지적한 뒤 ChatGPT는 처음에 존재하지 않는 취소·실패 상태를 fixture에 섞는 방안을 제안했다. 이후 실제 `OrderStatus` enum을 확인한 뒤 현재 도메인에는 적용하면 안 되는 제안임을 수정했다.

이 발견은 AI 자체 발견이 아니라 **Human이 학습 중 제기한 질문에서 시작됐다.** 이후 기록에서도 Human 지적을 AI 성과로 바꾸지 않는다.

## 후속 검증 방향

현재 도메인 계약을 유지한 동일한 전용 benchmark 스키마와 50만 건 `COMPLETED` fixture에서 다음 후보를 추가 비교한다.

- 후보 C: `(menu_id, ordered_at)`
- 후보 D: `(menu_id, ordered_at, status)` — 날짜 범위 탐색과 covering 효과를 구분하기 위한 보조 후보

각 후보는 다른 후보 인덱스를 제거한 상태에서 다음 동일 절차로 측정한다.

1. 대상 인덱스 하나만 생성
2. `ANALYZE TABLE orders`
3. 워밍업 3회
4. `EXPLAIN ANALYZE` 5회
5. 중앙값, 실제 사용 인덱스, covering 여부, `actual rows × loops` 기록
6. `information_schema.tables.index_length` 비교

최종 인덱스는 실행 시간만이 아니라 다음을 함께 고려해 선택한다.

- 실제 실행 계획
- 날짜 범위가 탐색 조건으로 사용되는지
- covering index 여부
- 인덱스 크기
- 더 적은 컬럼으로 동일한 효과를 내는지

## 후속 검증 결과

동일한 전용 benchmark 스키마와 50만 건 `COMPLETED` fixture에서 기준선·후보 A·B·C·D를 각각 새 스키마에서 측정했다. 각 조건은 `ANALYZE TABLE`, 워밍업 3회, `EXPLAIN ANALYZE` 5회 순서로 실행했다.

| 조건 | 중앙값 | 인덱스 크기 | covering | 실제 lookup key |
|---|---:|---:|---|---|
| 기준선 `(menu_id)` | 179.0ms | 24,215,552 bytes | 아니오 | `menu_id` |
| 후보 A `(ordered_at, menu_id)` | 184.0ms | 41,566,208 bytes | 아니오 | 기존 `menu_id` |
| 후보 B `(menu_id, status, ordered_at)` | 71.5ms | 29,458,432 bytes | 예 | `menu_id`, `status` |
| 후보 C `(menu_id, ordered_at)` | 202.0ms | 28,409,856 bytes | 아니오 | `menu_id` |
| 후보 D `(menu_id, ordered_at, status)` | 66.0ms | 29,458,432 bytes | 예 | `menu_id` |

- 모든 조건은 메뉴별 25,000행 × ACTIVE 메뉴 15개를 읽었다. 날짜 범위는 B·C·D 어느 후보에서도 실제 lookup key로 사용되지 않았다.
- 현재 모든 주문이 `COMPLETED`이므로 `status`는 선택도를 제공하지 않는다. 후보 C는 더 작지만 `status`가 없어 covering을 잃고 더 느렸다.
- B와 D는 같은 컬럼 수·크기·actual rows·loops이며 모두 covering이다. 이번 5회 표본에서 D의 중앙값이 더 낮았으므로, 현재 단일 상태 도메인에서는 `ordered_at`을 `status`보다 앞에 둔 D를 최종 인덱스로 선택한다. 이 결과만으로 D가 B보다 구조적으로 또는 운영 환경에서도 더 빠르다고 단정하지 않는다.

## 현재 판정

- Human이 지적한 최소 후보 검증 공백은 후보 C·D 비교로 해소됐다.
- 최종 선택은 `orders(menu_id, ordered_at, status)`이며, Contract Traceability의 후보 비교와 최종 인덱스 항목은 최신 측정 증거로 갱신했다.
- 향후 실패·취소 주문도 저장하도록 도메인이 바뀌면 상태 분포가 달라지므로 별도 benchmark에서 다시 검증한다.

## 재발 방지

- 복합 인덱스 후보를 설계할 때 쿼리 조건을 모두 포함한 후보뿐 아니라 불필요할 수 있는 저선택도 컬럼을 제거한 최소 후보도 함께 비교한다.
- 성능 fixture를 변경하기 전에 실제 enum, 저장 정책, 운영 데이터 모델을 먼저 확인한다.
- "테스트한 후보 중 가장 좋음"과 "가능한 대안 중 최적임"을 구분해 문서화한다.
- Human이 학습 중 발견한 설계 공백은 Human 발견으로 명시하고 AI 리뷰 로그에 보존한다.
