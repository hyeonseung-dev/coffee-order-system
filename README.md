# Coffee Order System

다중 서버 환경을 고려한 포인트 기반 커피 주문 시스템

## 핵심 목표

- 포인트 충전/차감 정합성 보장
- 주문/결제 트랜잭션 경계 명확화
- 동일 사용자 동시 주문 상황 검증
- 최근 7일 인기 메뉴 TOP 3 조회
- 인덱싱과 Redis 캐싱 전후 비교
- Issue 기반 Human-in-the-loop AI-assisted 개발 프로세스 기록
- 테스트·검증·트러블슈팅과 설계 선택 근거 보존

## 개발 단계 요약

| 단계 | 목표 | 핵심 검증 |
| --- | --- | --- |
| v0 | 필수 API 완성 | 메뉴 조회, 포인트 충전, 주문/결제, 인기 메뉴 조회 |
| v1 | 트랜잭션 / 동시성 검증 | 잔액 정합성, 동시 주문, 락 전략 |
| v2 | 성능 개선 | 인덱스, Redis 캐시, 성능 비교 |
| v3 | 선택 도전 | MySQL Replication, Redis Master-Replica, RabbitMQ |

현재 우선순위: v0 -> v1 -> v2 -> v3

## AI-assisted 개발 원칙

```text
Issue는 작업 계약이다.
PR은 공식 인계 지점이다.
테스트와 검증 없는 완료는 완료가 아니다.
중요한 트러블슈팅은 AI가 임의로 결정하지 않는다.
Merge는 Human만 수행한다.
```

ChatGPT는 요구사항·Issue·Markdown·PR 리뷰를 지원하고, Codex는 승인된 READY Issue의 코드·테스트·로컬 검증·PR 생성을 담당한다.

## 실행 방법

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew test
```

## 문서 링크

- [01. Project Context](docs/01_PROJECT_CONTEXT.md)
- [02. Requirements](docs/02_REQUIREMENTS.md)
- [03. API Spec](docs/03_API_SPEC.md)
- [04. ERD](docs/04_ERD.md)
- [05. AI Workflow](docs/05_AI_WORKFLOW.md)
- [06. Codex Rules](docs/06_CODEX_RULES.md)
- [07. PM Mode](docs/07_PM_MODE.md)
- [08. Course Plan](docs/08_COURSE_PLAN.md)
- [09. Troubleshooting](docs/09_TROUBLESHOOTING.md)
- [10. AI Review Log](docs/10_AI_REVIEW_LOG.md)
- [11. AI Automation Experiment](docs/11_AI_AUTOMATION_EXPERIMENT.md)
- [ADR Guide](docs/adr/README.md)
- [ADR Template](docs/adr/ADR_TEMPLATE.md)
- [Harness Logs](logs/README.md)
- [Code Review Template](.github/CODE_REVIEW_TEMPLATE.md)