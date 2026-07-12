---
name: Feature
about: 기능 구현 Issue
labels: feature
assignees: ''
---

# [Feature] 작업명

## 상태

- [ ] DRAFT
- [ ] READY
- [ ] IN_PROGRESS
- [ ] REVIEW
- [ ] BLOCKED
- [ ] DONE

> DRAFT는 변경 가능한 백로그이고, READY는 Human이 승인한 구현 계약입니다. Codex는 READY Issue만 구현합니다.

## 작업 목적

<!-- 어떤 사용자 문제를 왜 해결하는지 작성 -->

## 요구사항

-

## 완료 조건

- [ ]
- [ ]

## 제외 범위

이번 Issue에서는 다음 작업을 하지 않습니다.

-

## 선행 Issue와 의존성

- 선행 Issue:
- 관련 Issue:
- 의존 서비스·인프라:

## 위험도

- 위험도: `LOW | MEDIUM | HIGH`
- 판단 근거:

### 위험도별 기본 행동

- LOW: 기존 패턴을 따르는 명확한 작업. 별도 계획 승인 없이 구현부터 PR까지 진행
- MEDIUM: 짧은 계획과 영향 범위를 Human에게 보고하고 승인 후 구현
- HIGH: 필요한 ADR과 명시적 Human 승인 후 구현

## 관련 설계와 ADR

- API 영향:
- ERD 영향:
- 설정 영향:
- 관련 ADR:
- ADR 필요 여부와 이유:

> ADR은 모든 Issue에 작성하지 않습니다. 합리적인 대안이 여러 개이고 변경 영향이나 비용이 큰 결정에만 작성합니다.

## 구현 범위

- [ ] Controller
- [ ] Service
- [ ] Repository
- [ ] Domain / Entity
- [ ] DTO
- [ ] Exception
- [ ] Test
- [ ] Configuration
- [ ] Docs

## 수정 가능 파일

-

## 수정 금지 파일

-

명시적으로 허용되지 않은 공통 설정, README, 작업 규칙과 설계 문서는 보호 파일로 취급합니다.

## 테스트 계획

### 핵심 불변식 또는 비즈니스 규칙

-

### 필요한 테스트

- 단위 테스트:
- 통합 테스트:
- 동시성 테스트 필요 여부:
- 성능 테스트 필요 여부:
- 실패·롤백 시나리오:

### 테스트가 보장해야 하는 것

-

### 테스트하지 않는 범위

-

## 검증 계획

- 관련 테스트 명령:
- 전체 테스트 명령: `./gradlew test`
- 빌드 명령: `./gradlew build`
- 직접 API 검증:
- DB 상태 검증:
- 로그 검증:
- 성공 기준:
- 허용 가능한 미검증 범위:

## 예상 트러블슈팅

- 예상 실패 지점:
- 확인할 로그·데이터·상태:
- AI가 자체 해결할 수 있는 범위:
- Human에게 공유해야 하는 조건:

## Troubleshooting Gate

다음 상황에서는 AI가 임의로 구현을 계속하지 않고 Human에게 보고합니다.

- [ ] 요구사항 변경 없이는 테스트 통과 불가
- [ ] API 계약 또는 DB 스키마 변경 필요
- [ ] 트랜잭션 경계 또는 권한 정책 변경 필요
- [ ] 동시성 정합성, 데이터 손실, 중복 처리 가능성 발견
- [ ] 테스트 삭제·비활성화·약화 필요
- [ ] 성능 목표 미달 또는 운영 장애 가능성 발견
- [ ] ADR과 실제 구현 충돌
- [ ] 임시 우회와 근본 해결 중 선택 필요
- [ ] Issue 범위 밖 또는 보호 파일 변경 필요

## 문서 영향

- [ ] API 명세
- [ ] ERD
- [ ] README
- [ ] ADR
- [ ] Troubleshooting
- [ ] AI Review Log
- [ ] 문서 변경 없음

## Ready Check

- [ ] 목적과 요구사항이 명확하다.
- [ ] 완료 조건을 테스트하거나 검증할 수 있다.
- [ ] 제외 범위와 수정 경계가 명확하다.
- [ ] 선행 Issue가 완료됐다.
- [ ] API·ERD·설정 영향이 확인됐다.
- [ ] 위험도와 Human Gate가 결정됐다.
- [ ] 테스트가 보장할 핵심 규칙이 정의됐다.
- [ ] 검증 방법과 성공 기준이 있다.
- [ ] 예상 트러블슈팅과 중단 조건이 있다.
- [ ] 필요한 ADR이 Accepted 상태다.
- [ ] Human이 구현 시작을 승인했다.

## Human 승인

- 승인자:
- 승인일:
- 상태 전환: `DRAFT → READY`

## Merge 원칙

- ChatGPT Merge 금지
- Codex Merge 금지
- 자동 Merge 금지
- Human만 Merge 가능