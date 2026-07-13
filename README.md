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

현재 우선순위: v0 → v1 → v2 → v3

## AI-assisted 개발 원칙

```text
AI가 전체 Draft Issue 초안을 만든다.
담당자가 구현 전에 문제·규칙·흐름·검증 계획을 직접 작성한다.
AI가 문서와 현재 코드 기준으로 재검증한다.
Human이 READY와 위험도를 최종 승인한다.
Codex는 승인된 Issue를 구현·검증하고 Draft PR을 만든다.
위험도에 맞는 리뷰와 최신 CI를 통과한 뒤 Human만 Merge한다.
```

테스트 통과와 실제 문제 해결 검증을 구분하며, 실행하지 않은 결과를 성공으로 기록하지 않는다.

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
- [05. Team OASIS AI Workflow](docs/05_AI_WORKFLOW.md)
- [06. Codex Rules](docs/06_CODEX_RULES.md)
- [07. PM Mode](docs/07_PM_MODE.md)
- [08. Course Plan](docs/08_COURSE_PLAN.md)
- [09. Troubleshooting](docs/09_TROUBLESHOOTING.md)
- [10. AI Review Log](docs/10_AI_REVIEW_LOG.md)
- [11. AI Automation Experiment](docs/11_AI_AUTOMATION_EXPERIMENT.md)
- [12. Evidence Guide](docs/12_EVIDENCE_GUIDE.md)
- [ADR Guide](docs/adr/README.md)
- [ADR Template](docs/adr/ADR_TEMPLATE.md)
- [Harness Logs](logs/README.md)
- [Code Review Template](.github/CODE_REVIEW_TEMPLATE.md)
