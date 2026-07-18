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
AI가 현재 문서와 코드를 근거로 상세 Draft Issue를 만든다.
담당자가 자신의 흐름·대안·트레이드오프·누락 정책을 작성한다.
AI가 한 번 재검증하고 Human과 대화로 쟁점을 협의한다.
AI가 최종 합의 Issue를 정리하고 Human이 READY를 승인한다.
Codex는 승인된 Issue를 구현·검증하고 Draft PR을 만든다.
위험도에 맞는 리뷰와 최신 CI를 수행한다.
담당자가 실제 Diff를 설명하고 AI가 코드와 대조한다.
이해도 검증을 통과한 뒤 Human만 Merge한다.
```

테스트 통과와 실제 문제 해결 검증을 구분하며, 실행하지 않은 결과를 성공으로 기록하지 않는다.

## 실행 방법

```bash
./gradlew bootRun
```

### v3 MySQL Primary-Replica 실행

```bash
docker compose up -d mysql-primary mysql-replica redis
docker compose exec mysql-replica mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW REPLICA STATUS\\G"
```

- Primary는 `localhost:3307`, Replica는 `localhost:3308`을 사용한다.
- 쓰기 트랜잭션과 비관적 락 조회는 Primary에 고정된다. `@Transactional(readOnly = true)`인 메뉴·인기 메뉴 조회만 Replica 후보이며, 트랜잭션 밖의 조회는 안전하게 Primary를 사용한다.
- Replica 장애 시 자동 Primary fallback은 구현하지 않는다. 메뉴·인기 메뉴 조회는 연결 오류로 실패하며, 주문·포인트 기능은 Primary만 사용하므로 계속 처리된다.
- 초기화 스크립트는 빈 Docker volume에서만 실행된다. 복제 구성을 처음부터 다시 만들려면 데이터가 삭제되는 `docker compose down -v`가 필요하므로, 기존 로컬 데이터가 있으면 실행 전에 백업 여부를 판단한다.

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
- [13. AI Workflow Evolution](docs/13_AI_WORKFLOW_EVOLUTION.md)
- [Feature Refinement Issue Template](.github/ISSUE_TEMPLATE/feature-refinement.md)
- [Pull Request & Understanding Gate Template](.github/PULL_REQUEST_TEMPLATE.md)
- [ADR Guide](docs/adr/README.md)
- [ADR Template](docs/adr/ADR_TEMPLATE.md)
- [ADR-001. 인기 메뉴 캐시 전략](docs/adr/ADR-001-popular-menu-cache-strategy.md)
- [Issue #45 인기 메뉴 K6 성능 비교](docs/benchmarks/issue-45-popular-menu-k6.md)
- [Harness Logs](logs/README.md)
- [Code Review Template](.github/CODE_REVIEW_TEMPLATE.md)

## 인기 메뉴 API 성능 비교

Issue #45의 Human 로컬 K6 측정에서 Redis Cache Hit는 MySQL 직접 조회 기준선 대비 평균 응답시간 약 16.4% 감소, p95 약 18.4% 감소, 측정 구간 RPS 약 19.6% 증가를 기록했다. 이는 로컬 단일 장비·소규모 데이터 결과이며 운영 TPS로 일반화하지 않는다. 상세 조건과 Cache Miss 결과는 [벤치마크 문서](docs/benchmarks/issue-45-popular-menu-k6.md)를 따른다.
