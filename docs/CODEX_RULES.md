# Codex Rules

Codex는 이 프로젝트에서 Issue 단위 구현 보조 역할만 수행한다.

## 기본 원칙

- Issue 범위 안에서만 작업한다.
- Issue에 없는 기능을 추가하지 않는다.
- 불필요한 리팩토링을 하지 않는다.
- 패키지 구조를 임의로 변경하지 않는다.
- 새 브랜치를 임의로 생성하지 않는다.
- 현재 브랜치에서만 작업한다.
- Human 승인 전에는 파일을 수정하지 않는다.

## 작업 시작 전 필수 확인

작업 시작 전 반드시 다음을 확인하고 보고한다.

```bash
git branch --show-current
git status
git log --oneline -5
```

보고 내용:

- 현재 브랜치
- 작업 트리 상태
- 수정 예정 파일 목록
- 각 파일을 수정하는 이유
- 테스트 계획
- Issue 범위 밖 변경 없음 여부

## 수정 금지 파일

명시 지시가 없으면 다음 파일은 수정하지 않는다.

- `build.gradle`
- `settings.gradle`
- `application.yml`
- `application.properties`
- `SecurityConfig`
- `GlobalExceptionHandler`
- 공통 응답 DTO
- `README.md`
- 패키지 루트 구조

## 작업 중단 조건

다음 상황에서는 작업을 중단하고 Human에게 보고한다.

- 현재 브랜치가 지정 브랜치와 다를 때
- Issue 범위 밖 파일 수정이 필요할 때
- 공통 설정 파일 수정이 필요할 때
- 테스트 실패 원인이 현재 Issue와 무관할 때
- 기존 구조를 크게 바꿔야 해결 가능할 때
- 요구사항이 모호할 때
- 새 브랜치 생성이 필요하다고 판단될 때

## 구현 후 필수 보고

구현 후 반드시 다음을 실행하고 결과를 보고한다.

```bash
git status
git diff --stat
git diff
./gradlew test
```

보고 내용:

- 변경 파일 목록
- git diff 요약
- 테스트 결과
- 빌드 결과
- Issue 범위 이탈 여부
