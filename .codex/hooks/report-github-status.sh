#!/usr/bin/env bash

set -euo pipefail

# 이 스크립트는 GitHub 상태 댓글의 dry-run만 담당한다.
# 실제 GitHub 쓰기는 연결 도구를 사용한다.

target=""
number=""
status=""
attempt=""
reason=""
log_path=""
verification_log="logs/verification-log.md"
changed_files="로그 참조"
unverified="로그 참조"
human_action="최종 결과 확인"
repo_root=""

usage() {
  printf '%s\n' 'Usage:'
  printf '%s\n' '  report-github-status.sh --target <issue|pr> --number <positive-integer> \'
  printf '%s\n' '    --status <PASS|BLOCKED> --attempt <1|2> --reason <text> \'
  printf '%s\n' '    --log-path <repository-relative-path> [options]'
  printf '\n%s\n' 'Options:'
  printf '%s\n' '  --verification-log <path>  Default: logs/verification-log.md'
  printf '%s\n' '  --changed-files <text>'
  printf '%s\n' '  --unverified <text>'
  printf '%s\n' '  --human-action <text>'
  printf '%s\n' '  --help'
  printf '\n%s\n' 'This command never writes to GitHub.'
}

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

require_value() {
  option=$1
  count=$2
  if [ "$count" -lt 2 ]; then
    fail "${option} requires a value"
  fi
}

validate_relative_file() {
  relative_path=$1
  label=$2

  case "$relative_path" in
    ""|/*|../*|*/../*|*/..)
      fail "${label} must be a repository-relative path"
      ;;
  esac

  if [ ! -f "${repo_root}/${relative_path}" ]; then
    fail "${label} not found: ${relative_path}"
  fi
}

render_body() {
  if [ "$status" = "PASS" ]; then
    printf '%s\n\n' '## AI 하네스 실행 완료'
    printf -- '- 대상: %s #%s\n' "$target" "$number"
    printf '%s\n' '- 상태: PASS'
    printf -- '- Attempt: %s/2\n' "$attempt"
    printf -- '- Reviewer 최종 판정: %s\n' "$reason"
    printf -- '- 검증 결과: %s\n' "$reason"
    printf -- '- 변경 파일: %s\n' "$changed_files"
    printf -- '- 미검증 항목: %s\n' "$unverified"
    printf -- '- Attempt Log: `%s`\n' "$log_path"
    printf -- '- Verification Log: `%s`\n' "$verification_log"
    printf -- '- Human 확인 사항: %s\n' "$human_action"
  else
    printf '%s\n\n' '## AI 하네스 실행 중단'
    printf -- '- 대상: %s #%s\n' "$target" "$number"
    printf -- '- 최종 BLOCKED 상태: %s\n' "$reason"
    printf -- '- Attempt: %s/2\n' "$attempt"
    printf -- '- 마지막 실패 원인: %s\n' "$reason"
    printf -- '- 마지막 테스트 또는 빌드 결과: `%s` 참조\n' "$verification_log"
    printf -- '- 변경 파일: %s\n' "$changed_files"
    printf -- '- 미검증 항목: %s\n' "$unverified"
    printf -- '- Attempt Log: `%s`\n' "$log_path"
    printf -- '- Verification Log: `%s`\n' "$verification_log"
    printf -- '- Human이 결정해야 할 사항: %s\n' "$human_action"
    printf '%s\n' '- 자동 진행이 중단되었습니다.'
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --target)
      require_value "$1" "$#"
      target=$2
      shift 2
      ;;
    --number)
      require_value "$1" "$#"
      number=$2
      shift 2
      ;;
    --status)
      require_value "$1" "$#"
      status=$2
      shift 2
      ;;
    --attempt)
      require_value "$1" "$#"
      attempt=$2
      shift 2
      ;;
    --reason)
      require_value "$1" "$#"
      reason=$2
      shift 2
      ;;
    --log-path)
      require_value "$1" "$#"
      log_path=$2
      shift 2
      ;;
    --verification-log)
      require_value "$1" "$#"
      verification_log=$2
      shift 2
      ;;
    --changed-files)
      require_value "$1" "$#"
      changed_files=$2
      shift 2
      ;;
    --unverified)
      require_value "$1" "$#"
      unverified=$2
      shift 2
      ;;
    --human-action)
      require_value "$1" "$#"
      human_action=$2
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

case "$target" in
  issue|pr) ;;
  "") usage >&2; fail '--target is required' ;;
  *) fail '--target must be issue or pr' ;;
esac

if ! printf '%s' "$number" | grep -Eq '^[1-9][0-9]*$'; then
  fail '--number must be a positive integer'
fi

if [ "$status" = "FAIL" ]; then
  fail 'FAIL is an internal retry state and must not be published'
fi

case "$status" in
  PASS|BLOCKED) ;;
  "") fail '--status is required' ;;
  *) fail '--status must be PASS or BLOCKED' ;;
esac

case "$attempt" in
  1|2) ;;
  "") fail '--attempt is required' ;;
  *) fail '--attempt must be 1 or 2' ;;
esac

if [ -z "$reason" ]; then
  fail '--reason must not be empty'
fi

if [ -z "$log_path" ]; then
  fail '--log-path is required'
fi

if ! repo_root=$(git rev-parse --show-toplevel 2>/dev/null); then
  fail 'git repository root not found'
fi

validate_relative_file "$log_path" '--log-path'
validate_relative_file "$verification_log" '--verification-log'

printf '%s\n\n' 'DRY RUN: GitHub comment was not published'
printf 'Target: %s\n' "$target"
printf 'Number: #%s\n' "$number"
printf 'Status: %s\n' "$status"
printf 'Attempt: %s/2\n\n' "$attempt"
render_body
