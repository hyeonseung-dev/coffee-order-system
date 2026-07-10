# AI Review Log

AI 리뷰 결과와 반영 여부를 기록한다.

## PR #

### 작업 내용

<!-- PR 요약 -->

### ChatGPT 리뷰

| 항목 | 리뷰 내용 | 반영 여부 | 사유 |
|---|---|---|---|
|  |  |  |  |

### Codex 문맥 리뷰

| 항목 | 리뷰 내용 | 반영 여부 | 사유 |
|---|---|---|---|
|  |  |  |  |

### Codex Review 독립 리뷰

| 항목 | 리뷰 내용 | 반영 여부 | 사유 |
|---|---|---|---|
|  |  |  |  |

### 최종 반영 결과

- 반영한 항목:
- 보류한 항목:
- 보류 사유:
- 재테스트 결과:

### 회고

- AI가 도움이 된 부분:
- AI가 위험했던 부분:
- 다음 Issue에서 개선할 점:

## Issue #6 - 커피 메뉴 목록 조회 API

### 검토 구분

- 설계/구현 주체: ChatGPT 설계 + Codex 구현
- 검토 주체: Human
- 검토 단계: 구현 후 Human 검토

### Human 검토 내용

| 번호 | 검토 내용 | 문제점 | 조치 |
|---|---|---|---|
| 1 | Controller 반환 구조 | `Map<String, List<MenuResponse>>` 형태로 응답을 구성해 문자열 key에 의존하고 있었음 | `MenuListResponse` DTO를 추가하고 `ResponseEntity<MenuListResponse>`로 반환하도록 수정 요청 |
| 2 | 테스트 코드 구조 | 테스트가 Given-When-Then 흐름으로 구분되지 않아 의도와 검증 구조를 빠르게 파악하기 어려웠음 | 테스트 데이터 준비, 실행, 검증을 `given`, `when`, `then` 구간으로 명확히 분리하도록 수정 요청 |

### Human 판단 근거

#### 1. Map 응답 구조 문제

`Map.of("data", ...)` 방식은 빠르게 응답 구조를 만들 수 있지만 다음 문제가 있다.

- 문자열 key 오타를 컴파일 시점에 검증할 수 없음
- 응답 구조가 명시적 타입으로 표현되지 않음
- Swagger/OpenAPI 문서화 시 구조가 불명확할 수 있음
- 향후 필드 확장 시 일관된 응답 설계가 어려움
- 유지보수성과 협업 가독성이 떨어짐

따라서 개별 메뉴 DTO인 `MenuResponse`와 별도로,
목록 전체 응답을 감싸는 `MenuListResponse` DTO를 사용하도록 결정했다.

#### 2. Given-When-Then 미적용 문제

테스트가 동작하더라도 준비, 실행, 검증 구간이 명확하지 않으면 다음 문제가 있다.

- 테스트 의도를 빠르게 파악하기 어려움
- 실패 원인을 추적하기 어려움
- 리뷰 시 어떤 비즈니스 규칙을 검증하는지 확인하기 어려움
- 테스트 코드가 길어질수록 가독성이 빠르게 저하됨

따라서 모든 테스트를 기계적으로 주석만 추가하는 방식이 아니라,
실제 코드 흐름이 Given-When-Then으로 구분되도록 수정하도록 결정했다.

### 반영 상태

- [x] `Map` 반환 제거
- [x] `MenuListResponse` DTO 추가
- [x] `ResponseEntity<MenuListResponse>` 적용
- [x] 테스트 Given-When-Then 구조 정리
- [x] 테스트 메서드명 한글화
- [x] 개발용 초기 메뉴 데이터 추가
- [x] 테스트 재실행
- [x] 빌드 재실행
- [x] Human 재검토

### ChatGPT 1차 리뷰

| 번호 | 리뷰 내용 | 반영 여부 | 조치 |
|---|---|---|---|
| 1 | Postman Collection에 아직 구현하지 않은 포인트 충전, 주문, 인기 메뉴 API가 포함되어 Issue #6 범위를 벗어남 | 반영 | Collection에서 미구현 예정 API를 제거하고 실제 구현된 `GET /api/menus` 요청만 남김 |
| 2 | `docker-compose.yml`이 환경변수를 요구하지만 `.env`는 gitignore 처리되어 예제 파일이 없음 | 반영 | 루트에 `.env.example`을 추가하고 실제 비밀번호 대신 `change-me` 예시값 사용 |
| 3 | `application.properties`에 `coffee1234` 기본 비밀번호가 하드코딩되어 있음 | 반영 | `spring.datasource.username=${DB_USERNAME}`, `spring.datasource.password=${DB_PASSWORD}`로 변경 |
| 4 | 초기 데이터가 `count() == 0`일 때만 저장되어 일부 메뉴 누락 시 보충하지 못함 | 보류 | 현재 개인과제 초기 단계에서는 PR 본문에 한계로 명시하고 후속 개선 대상으로 둠 |

### 결론

AI가 생성한 코드를 그대로 반영하지 않고, 응답 타입 안정성과 테스트 가독성을 기준으로 직접 검토했습니다. 
Map 기반 응답을 DTO 기반으로 수정했고, 테스트를 Given-When-Then 구조로 재정비했습니다.
ChatGPT 1차 리뷰에서 지적된 Postman 범위, `.env.example`, DB 비밀번호 기본값 문제를 반영했습니다.
