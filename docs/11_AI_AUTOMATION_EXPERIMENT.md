# AI Automation Experiment

## 1. 실험 배경

기존 방식에서는 Human이 ChatGPT와 Codex 사이에서 요구사항, 구현 프롬프트, 결과, 수정 지시를 반복 전달했다.

```text
Human + ChatGPT 설계
→ Codex용 긴 프롬프트
→ Codex 구현
→ Human이 결과 복사
→ ChatGPT 리뷰
→ 수정·Commit·Push·PR을 단계별 지시
```

이 구조는 통제력은 높지만 Human이 메시지 브로커가 되고 같은 내용을 여러 번 전달하는 문제가 있었다.

다른 수강생의 Agent, Skill, Hook, Log 기반 자동화 흐름을 참고해 개인 프로젝트에 다음 하네스를 적용했다.

```text
Issue
→ Preflight
→ Planner
→ Implementer
→ Verify
→ Reviewer
→ Log
→ PASS 또는 BLOCKED
```

## 2. 첫 실전 적용

Issue #8 주문·결제 트랜잭션 구현에 첫 end-to-end 실행을 적용했다.

확인한 장점:

- Issue와 문서 충돌을 구현 전에 발견함
- AI의 범위 확장을 통제함
- Rollback 통합 테스트를 수행함
- 독립 Reviewer 검토를 수행함
- 로컬과 CI 환경 차이를 발견함
- 실제 테스트와 빌드 결과를 GitHub에 남김

확인한 문제:

- 하네스 구축 비용이 큼
- Planner, Implementer, Reviewer가 같은 문맥을 반복해서 읽음
- 위험도 재판정, 로그, 상태 보고가 중복됨
- 환경변수와 GitHub CLI 의존성으로 BLOCKED가 발생함
- PR 메타데이터와 로그 형식 같은 비품질 문제도 작업을 차단함
- 기존 방식보다 시간, 토큰, Human 개입이 증가함

## 3. 첫 번째 결론

자동화 단계가 많다고 더 좋은 개발 방식이 되는 것은 아니다.

하네스는 다음 결과로 평가해야 한다.

- 구현 시간이 줄어드는가
- 토큰 사용량이 줄어드는가
- Human 반복 전달이 줄어드는가
- 결과 품질이 유지되거나 좋아지는가
- 실제 필요한 상황에서만 BLOCKED 되는가
- 개발자가 결과를 이해할 수 있는가

첫 실행에서는 품질과 통제력은 높아졌지만 시간과 토큰, Human 개입 비용도 증가했다.

## 4. 경량화한 부분

반드시 차단할 항목:

- 테스트 실패
- 빌드 실패
- Bash 문법 오류
- 필수 하네스 파일 누락
- 보호 브랜치 직접 Push
- 요구사항·정합성·보안·운영 위험

경고 또는 선택 기록으로 바꾼 항목:

- 하네스 전용 환경변수 누락
- 상세 Attempt 메타데이터 누락
- PR 형식 일부 누락
- 로그 경로와 PASS 문자열 누락
- GitHub 상태 보고 실패

현재 하네스는 모든 실수를 막는 통제 장치가 아니라 치명적인 실수를 막는 최소 안전장치로 사용한다.

## 5. 추가로 발견한 개선점

### ChatGPT가 문서를 직접 관리할 수 있음

기존에는 ChatGPT와 설계한 내용을 다시 Codex에게 프롬프트로 전달해 Markdown 파일을 작성하게 했다.

하지만 ChatGPT가 GitHub Issue, Branch, Markdown, PR을 직접 관리할 수 있으므로 다음과 같이 단순화할 수 있었다.

```text
기존
Human + ChatGPT 설계
→ Codex용 문서 프롬프트
→ Codex가 Markdown 수정

개선
Human + ChatGPT 설계
→ ChatGPT가 Issue와 Markdown 직접 반영
```

Codex는 Java/Spring 코드, 테스트, 실행, 디버깅에 집중한다.

### PR을 공식 인계 지점으로 사용할 수 있음

기존에는 Codex 구현 결과를 Human이 다시 ChatGPT에 복사했다.

```text
기존
Codex 구현
→ Human 결과 복사
→ ChatGPT 검토

개선
Codex 구현
→ Push·PR 생성
→ Human이 PR 번호 전달
→ ChatGPT가 GitHub diff·Actions 직접 검토
```

GitHub Issue는 작업 계약, PR은 AI와 Human 사이의 공식 인계 지점이 된다.

## 6. 현재 운영 결론: Human Gate가 있는 반자동화

완전 자동화도, 모든 단계에 Human 승인을 요구하는 방식도 사용하지 않는다.

```text
Draft Issue
→ Issue Refinement
→ 필요한 ADR
→ Human READY 승인
→ Codex 구현·테스트·검증
→ Commit·Push·PR
→ ChatGPT GitHub 리뷰
→ Human 코드 이해와 최종 판단
→ Human 직접 Merge
```

### Human이 집중할 영역

- 목적과 사용자 문제
- 요구사항과 도메인 정책
- 설계, 로직, 알고리즘, 아키텍처
- 기술 선택과 효율성
- 테스트 기준과 검증 해석
- 중요한 트러블슈팅 결정
- 코드 이해
- 최종 선택과 결과 책임
- Merge

### AI가 보조할 영역

- 요구사항과 문서 구조화
- 코드와 테스트 초안
- 반복 구현과 일반 디버깅
- diff와 로그 분석
- 리뷰 후보 제시
- 문서 보완

## 7. 위험도 운영

별도 실행 모드는 만들지 않고 Issue 위험도 하나로 Human Gate 수준을 결정한다.

| 위험도 | 예시 | 행동 |
| --- | --- | --- |
| LOW | DTO, Validation, 단순 조회, 테스트 보완, 작은 버그 | Codex가 구현부터 PR까지 진행 |
| MEDIUM | API 계약, Entity 관계, 트랜잭션, 인덱스, 캐시, 이벤트 | 짧은 계획과 영향 범위 Human 승인 후 진행 |
| HIGH | 인증·인가, 금액·포인트, 동시성, Migration, 메시지 보장, 운영 설정 | ADR과 명시적 Human 승인 후 진행 |

위험도는 불필요한 문서나 리뷰를 늘리기 위한 값이 아니라 AI가 언제 멈춰야 하는지를 정하기 위한 값이다.

## 8. Issue와 ADR

주요 기능은 Draft Issue로 미리 작성할 수 있다. Draft는 변경 가능한 백로그다.

구현 직전에는 현재 코드와 문서를 기준으로 Refinement와 Ready Check를 수행한다. Human이 승인한 READY Issue만 구현 계약으로 사용한다.

ADR은 모든 Issue에 작성하지 않는다. 합리적인 대안이 여러 개이고 결정의 영향과 변경 비용이 클 때만 작성한다.

예시:

- 비관적 락과 낙관적 락
- 캐시 전략
- 이벤트 발행 시점
- 메시징 선택
- 인증 방식
- 트랜잭션 경계

## 9. 테스트·검증·트러블슈팅

테스트는 개수가 아니라 보장 내용을 기록한다.

- 테스트가 보장하는 것
- 테스트가 보장하지 않는 것
- 직접 검증한 내용
- 수행하지 못한 검증
- 미검증 범위

컴파일 오류, import 누락, 단순 테스트 데이터와 Mock 오류는 Codex가 자체 해결한다.

다음은 Troubleshooting Gate에서 멈춘다.

- 요구사항 변경 필요
- API·DB·트랜잭션·권한 정책 변경 필요
- 동시성 정합성 미보장
- 데이터 손실 또는 중복 가능성
- 테스트 약화 필요
- 성능 목표 미달
- ADR 충돌
- 임시 우회와 근본 해결 중 선택 필요
- Issue 범위 밖 수정 필요

문제 발생 과정과 실패한 접근은 의미가 있을 때 핵심 산출물로 기록한다.

## 10. Merge 경계

실험 초기부터 Merge는 자동화 대상이 아니었고, 현재 운영에서도 예외 없이 Human 전용이다.

```text
ChatGPT Merge 금지
Codex Merge 금지
자동 Merge 금지
Human만 Merge 가능
```

AI의 PASS, 승인 가능, 리뷰 완료 판단은 실제 Merge 권한을 의미하지 않는다.

## 11. 최종 평가 기준

현재 워크플로우의 목표는 가장 복잡한 하네스를 만드는 것이 아니다.

- 반복 전달과 불필요한 토큰을 줄임
- 테스트와 검증 품질을 유지함
- 중요한 문제를 숨기지 않음
- Human이 설계와 결과를 이해함
- 실패와 선택 근거를 설명할 수 있음
- 취준생 프로젝트에서 운영 비용이 과도하지 않음

이 실험은 정답인 하네스를 구현한 과정이 아니라 프로젝트 규모와 현재 역량에 맞는 AI 활용 방식을 찾은 과정이다.