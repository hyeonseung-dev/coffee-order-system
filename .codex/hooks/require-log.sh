#!/usr/bin/env bash

set -euo pipefail

MAX_ATTEMPTS=2
issue=""
attempt=""
status=""

print_context() {
  printf 'Issue: %s\n' "${issue:-<missing>}"
  printf 'Attempt: %s\n' "${attempt:-<missing>}"
  printf 'Status: %s\n' "${status:-<missing>}"
}

fail() {
  message=$1
  missing=${2:-}
  path=${3:-}

  printf '%s\n' "$message"
  print_context
  if [ -n "$missing" ]; then
    printf 'Missing or invalid: %s\n' "$missing"
  fi
  if [ -n "$path" ]; then
    printf 'Check: %s\n' "$path"
  fi
  exit 1
}

require_pattern() {
  file=$1
  pattern=$2
  label=$3
  error=$4

  if ! grep -Eiq -- "$pattern" "$file"; then
    fail "$error" "$label" "$file"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --issue)
      [ "$#" -ge 2 ] || fail 'ERROR: valid issue number is required' '--issue value'
      issue=$2
      shift 2
      ;;
    --attempt)
      [ "$#" -ge 2 ] || fail 'ERROR: attempt must be 1 or 2' '--attempt value'
      attempt=$2
      shift 2
      ;;
    --status)
      [ "$#" -ge 2 ] || fail 'ERROR: status must be PASS, FAIL, or BLOCKED' '--status value'
      status=$2
      shift 2
      ;;
    *)
      fail 'ERROR: unknown argument' "$1"
      ;;
  esac
done

if ! printf '%s' "$issue" | grep -Eq '^[1-9][0-9]*$'; then
  fail 'ERROR: valid issue number is required' '--issue'
fi

if printf '%s' "$attempt" | grep -Eq '^[0-9]+$' && [ "$attempt" -gt "$MAX_ATTEMPTS" ]; then
  fail 'BLOCKED: RETRY LIMIT' "attempt exceeds ${MAX_ATTEMPTS}"
fi

case "$attempt" in
  1|2) ;;
  *) fail 'ERROR: attempt must be 1 or 2' '--attempt' ;;
esac

case "$status" in
  PASS|FAIL|BLOCKED) ;;
  *) fail 'ERROR: status must be PASS, FAIL, or BLOCKED' '--status' ;;
esac

if [ "$attempt" -eq 2 ] && [ "$status" = 'FAIL' ]; then
  fail 'BLOCKED: RETRY LIMIT' 'Attempt 2 cannot return FAIL'
fi

if ! repo_root=$(git rev-parse --show-toplevel 2>/dev/null); then
  fail 'ERROR: git repository root not found' 'repository root'
fi

attempt_rel="logs/issues/issue-${issue}/attempt-log.md"
verification_rel='logs/verification-log.md'
attempt_log="${repo_root}/${attempt_rel}"
verification_log="${repo_root}/${verification_rel}"

if [ ! -f "$attempt_log" ]; then
  printf 'BLOCKED: ATTEMPT LOG REQUIRED\n'
  print_context
  printf 'Expected: %s\n' "$attempt_rel"
  exit 1
fi

if [ ! -f "$verification_log" ]; then
  printf 'BLOCKED: VERIFICATION LOG REQUIRED\n'
  print_context
  printf 'Expected: %s\n' "$verification_rel"
  exit 1
fi

require_pattern "$attempt_log" "(Issue|대상 Issue)[[:space:]]*:[[:space:]]*#?${issue}([^0-9]|$)|Issue[[:space:]]*#${issue}([^0-9]|$)" '대상 Issue 번호' 'BLOCKED: WORK CONTEXT REQUIRED'
require_pattern "$attempt_log" "(현재[[:space:]]*Attempt|Attempt)[[:space:]]*:[[:space:]]*${attempt}(/2)?([^0-9]|$)|##[[:space:]]*Attempt[[:space:]]+${attempt}([^0-9]|$)" '현재 Attempt' 'BLOCKED: WORK CONTEXT REQUIRED'

if [ "$attempt" -eq 2 ] && grep -Eiq -- '이전 실패와 동일 여부[[:space:]]*:[[:space:]]*YES([^A-Z]|$)' "$attempt_log"; then
  fail 'BLOCKED: REPEATED FAILURE' '이전 실패와 동일 여부: YES' "$attempt_rel"
fi

case "$status" in
  PASS)
    require_pattern "$attempt_log" '최종 상태[[:space:]]*:[[:space:]]*PASS([^A-Z]|$)' '최종 상태: PASS' 'BLOCKED: PASS EVIDENCE REQUIRED'
    require_pattern "$attempt_log" 'Reviewer 최종 판정[[:space:]]*:[[:space:]]*PASS([^A-Z]|$)' 'Reviewer 최종 판정: PASS' 'BLOCKED: PASS EVIDENCE REQUIRED'
    require_pattern "$attempt_log" '최종 검증 결과[[:space:]]*:[[:space:]]*[^[:space:]]' '최종 검증 결과' 'BLOCKED: PASS EVIDENCE REQUIRED'
    require_pattern "$attempt_log" 'Human 확인 필요 사항[[:space:]]*:[[:space:]]*[^[:space:]]' 'Human 확인 필요 사항' 'BLOCKED: PASS EVIDENCE REQUIRED'
    require_pattern "$verification_log" "Issue[[:space:]]*#${issue}([^0-9]|$)" 'Verification Log의 Issue 기록' 'BLOCKED: PASS EVIDENCE REQUIRED'
    require_pattern "$verification_log" '최종 상태[[:space:]]*:[[:space:]]*PASS([^A-Z]|$)' 'Verification Log의 PASS 상태' 'BLOCKED: PASS EVIDENCE REQUIRED'
    ;;
  FAIL)
    require_pattern "$attempt_log" '실패 원인[[:space:]]*:[[:space:]]*[^[:space:]]' '실패 원인' 'BLOCKED: FIX PACKET REQUIRED'
    require_pattern "$attempt_log" '수정 대상[[:space:]]*:[[:space:]]*[^[:space:]]' '수정 대상' 'BLOCKED: FIX PACKET REQUIRED'
    require_pattern "$attempt_log" '다음 시도 지침[[:space:]]*:[[:space:]]*[^[:space:]]' '다음 시도 지침' 'BLOCKED: FIX PACKET REQUIRED'
    require_pattern "$attempt_log" '수정하지 말아야 할 범위[[:space:]]*:[[:space:]]*[^[:space:]]' '수정하지 말아야 할 범위' 'BLOCKED: FIX PACKET REQUIRED'
    require_pattern "$attempt_log" '재검증 명령[[:space:]]*:[[:space:]]*[^[:space:]]' '재검증 명령' 'BLOCKED: FIX PACKET REQUIRED'
    ;;
  BLOCKED)
    require_pattern "$attempt_log" '최종 상태[[:space:]]*:[[:space:]]*BLOCKED([^A-Z]|$)' 'BLOCKED 상태' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" '(총 Attempt 수|Attempt 횟수)[[:space:]]*:[[:space:]]*[12](/2)?([^0-9]|$)' 'Attempt 횟수' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" '마지막 실패 원인[[:space:]]*:[[:space:]]*[^[:space:]]' '마지막 실패 원인' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" '마지막 (테스트|빌드) 결과[[:space:]]*:[[:space:]]*[^[:space:]]' '마지막 테스트 또는 빌드 결과' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" '(수정된 파일|최종 변경 파일)[[:space:]]*:[[:space:]]*[^[:space:]]' '수정된 파일' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" '미검증 항목[[:space:]]*:[[:space:]]*[^[:space:]]' '미검증 항목' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    require_pattern "$attempt_log" 'Human(이)? 결정해야 할 사항[[:space:]]*:[[:space:]]*[^[:space:]]' 'Human이 결정해야 할 사항' 'BLOCKED: HUMAN HANDOFF REQUIRED'
    ;;
esac

printf 'HARNESS CHECK PASSED\n\n'
printf 'Issue: #%s\n' "$issue"
printf 'Attempt: %s/%s\n' "$attempt" "$MAX_ATTEMPTS"
printf 'Status: %s\n' "$status"
printf 'Attempt Log: %s\n' "$attempt_rel"
printf 'Verification Log: %s\n' "$verification_rel"
