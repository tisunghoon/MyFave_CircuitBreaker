#!/usr/bin/env bash
# Monitor 폴링 raw 데이터 → 정리된 수치 (peak/avg) 추출.
#
# 사용법:
#   ./analyze-monitor.sh <scenario> <version> [date]
#   - date 생략 시 오늘 날짜로 검색
#
# 예시:
#   ./analyze-monitor.sh d v1
#   ./analyze-monitor.sh d v1 2026-05-23
#
# 출력:
#   1. stdout — Markdown 표 형식 (보고서에 복사 가능)
#   2. ~/loadtest-results/{scenario}_{version}_{date}/analyze.md (위와 동일 내용)

set -euo pipefail

if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <scenario> <version> [date]"
  exit 1
fi

SCENARIO=$1
VERSION=$2
DATE=${3:-$(date +%Y-%m-%d)}

BASE_DIR="$HOME/loadtest-results/${SCENARIO}_${VERSION}_${DATE}"
MON_DIR="$BASE_DIR/monitor"

if [ ! -d "$MON_DIR" ]; then
  echo "ERROR: monitor 폴더 없음 — $MON_DIR"
  echo "       먼저 'collect-results.sh $SCENARIO $VERSION monitor' 로 폴링 데이터를 수집하세요."
  exit 1
fi

OUT_FILE="$BASE_DIR/analyze.md"

# ---------- 헬퍼 함수 ----------

# Actuator 메트릭 한 줄의 값(마지막 컬럼) 추출.
# 입력 패턴: 'jvm_memory_used_bytes{area="heap",...} 1.234E9'
extract_metric_value() {
  local pattern=$1
  local file=$2
  grep -E "^${pattern}" "$file" 2>/dev/null | awk '{print $NF}' | head -1
}

# 시계열 파일들에서 metric의 값을 모아 sort.
# stdout: 한 줄에 하나의 숫자 (오름차순)
extract_metric_series() {
  local pattern=$1
  local glob=$2
  for f in ${MON_DIR}/${glob}; do
    [ -f "$f" ] || continue
    extract_metric_value "$pattern" "$f"
  done | sort -n
}

# peak (최댓값) 계산
metric_peak() {
  extract_metric_series "$1" "$2" | tail -1
}

# avg (산술 평균) 계산
metric_avg() {
  extract_metric_series "$1" "$2" \
    | awk '{ sum += $1; count++ } END { if (count>0) printf "%.2f", sum/count }'
}

# Bytes → MiB 변환
bytes_to_mib() {
  awk -v v="$1" 'BEGIN{ if (v=="") print "-"; else printf "%.1f MiB", v/1024/1024 }'
}

# 시계열 파일 개수
count_files() {
  ls "${MON_DIR}"/$1 2>/dev/null | wc -l | tr -d ' '
}

# ---------- 본격 분석 ----------

POLLING_COUNT=$(count_files "metrics_*.txt")
START_TS=$(ls "${MON_DIR}"/metrics_*.txt 2>/dev/null | head -1 | sed 's/.*metrics_\([0-9]*\)\.txt/\1/')
END_TS=$(ls "${MON_DIR}"/metrics_*.txt 2>/dev/null | tail -1 | sed 's/.*metrics_\([0-9]*\)\.txt/\1/')

# 모니터링 기간 계산 (단순 차이, HHMMSS)
duration_sec() {
  local start=$1
  local end=$2
  python3 -c "
s='$start'; e='$end'
if not s or not e: print('-'); exit()
def to_sec(t): return int(t[:2])*3600 + int(t[2:4])*60 + int(t[4:6])
print(to_sec(e) - to_sec(s))
" 2>/dev/null || echo "-"
}

DUR=$(duration_sec "$START_TS" "$END_TS")

# ----- JVM -----
HEAP_PEAK_RAW=$(metric_peak 'jvm_memory_used_bytes\{area="heap"' 'metrics_*.txt')
HEAP_AVG_RAW=$(metric_avg 'jvm_memory_used_bytes\{area="heap"' 'metrics_*.txt')
HEAP_PEAK=$(bytes_to_mib "$HEAP_PEAK_RAW")
HEAP_AVG=$(bytes_to_mib "$HEAP_AVG_RAW")

GC_PAUSE_MAX=$(metric_peak 'jvm_gc_pause_seconds_max' 'metrics_*.txt')
GC_COUNT=$(metric_peak 'jvm_gc_pause_seconds_count' 'metrics_*.txt')

# ----- Tomcat / HTTP -----
TOMCAT_BUSY_PEAK=$(metric_peak 'tomcat_threads_busy_threads' 'metrics_*.txt')
TOMCAT_CONN_PEAK=$(metric_peak 'tomcat_threads_current_threads' 'metrics_*.txt')
HTTP_REQS_TOTAL=$(metric_peak 'http_server_requests_seconds_count' 'metrics_*.txt')

# RPS 계산: (마지막 - 첫) / 기간
HTTP_REQS_FIRST=$(extract_metric_series 'http_server_requests_seconds_count' 'metrics_*.txt' | head -1)
HTTP_REQS_LAST=$(extract_metric_series 'http_server_requests_seconds_count' 'metrics_*.txt' | tail -1)
if [ -n "${HTTP_REQS_FIRST:-}" ] && [ -n "${HTTP_REQS_LAST:-}" ] && [ "$DUR" != "-" ] && [ "$DUR" -gt 0 ] 2>/dev/null; then
  RPS_AVG=$(awk -v f="$HTTP_REQS_FIRST" -v l="$HTTP_REQS_LAST" -v d="$DUR" 'BEGIN{ printf "%.1f", (l-f)/d }')
else
  RPS_AVG="-"
fi

# ----- Hikari -----
HIKARI_ACTIVE_PEAK=$(metric_peak 'hikaricp_connections_active' 'metrics_*.txt')
HIKARI_PENDING_PEAK=$(metric_peak 'hikaricp_connections_pending' 'metrics_*.txt')
HIKARI_MAX=$(metric_peak 'hikaricp_connections_max' 'metrics_*.txt')

# ----- DB Lock peak (모드별) -----
declare -A LOCK_PEAKS
for f in ${MON_DIR}/locks_*.txt; do
  [ -f "$f" ] || continue
  while IFS='|' read -r mode count; do
    mode=$(echo "$mode" | tr -d ' ')
    count=$(echo "$count" | tr -d ' ')
    [ -z "$mode" ] || [ -z "$count" ] && continue
    prev=${LOCK_PEAKS[$mode]:-0}
    if [ "$count" -gt "$prev" ] 2>/dev/null; then
      LOCK_PEAKS[$mode]=$count
    fi
  done < "$f"
done

# ----- DB activity peak -----
declare -A DB_ACT_PEAKS
for f in ${MON_DIR}/db_activity_*.txt; do
  [ -f "$f" ] || continue
  while IFS='|' read -r state count; do
    state=$(echo "$state" | tr -d ' ')
    count=$(echo "$count" | tr -d ' ')
    [ -z "$state" ] || [ -z "$count" ] && continue
    prev=${DB_ACT_PEAKS[$state]:-0}
    if [ "$count" -gt "$prev" ] 2>/dev/null; then
      DB_ACT_PEAKS[$state]=$count
    fi
  done < "$f"
done

# ----- Transaction commit/rollback rate -----
TX_FIRST=$(ls ${MON_DIR}/tx_*.txt 2>/dev/null | head -1)
TX_LAST=$(ls ${MON_DIR}/tx_*.txt 2>/dev/null | tail -1)
if [ -n "$TX_FIRST" ] && [ -n "$TX_LAST" ]; then
  COMMIT_FIRST=$(awk -F'|' '{print $1}' "$TX_FIRST" | tr -d ' ')
  COMMIT_LAST=$(awk -F'|' '{print $1}' "$TX_LAST" | tr -d ' ')
  ROLLBACK_FIRST=$(awk -F'|' '{print $2}' "$TX_FIRST" | tr -d ' ')
  ROLLBACK_LAST=$(awk -F'|' '{print $2}' "$TX_LAST" | tr -d ' ')
  DEADLOCK_FIRST=$(awk -F'|' '{print $3}' "$TX_FIRST" | tr -d ' ')
  DEADLOCK_LAST=$(awk -F'|' '{print $3}' "$TX_LAST" | tr -d ' ')
  if [ "$DUR" != "-" ] && [ "$DUR" -gt 0 ] 2>/dev/null; then
    COMMIT_RATE=$(awk -v f="$COMMIT_FIRST" -v l="$COMMIT_LAST" -v d="$DUR" 'BEGIN{ printf "%.1f", (l-f)/d }')
    ROLLBACK_RATE=$(awk -v f="$ROLLBACK_FIRST" -v l="$ROLLBACK_LAST" -v d="$DUR" 'BEGIN{ printf "%.1f", (l-f)/d }')
  else
    COMMIT_RATE="-"
    ROLLBACK_RATE="-"
  fi
  COMMIT_TOTAL_DIFF=$((COMMIT_LAST - COMMIT_FIRST))
  ROLLBACK_TOTAL_DIFF=$((ROLLBACK_LAST - ROLLBACK_FIRST))
  DEADLOCK_DIFF=$((DEADLOCK_LAST - DEADLOCK_FIRST))
fi

# ----- 백엔드 컨테이너 CPU/Memory peak -----
APP_CPU_PEAK=$(grep -h '^myfave-app' ${MON_DIR}/docker_*.txt 2>/dev/null \
  | cut -d'|' -f2 | tr -d '% ' | sort -n | tail -1)
APP_MEM_PEAK=$(grep -h '^myfave-app' ${MON_DIR}/docker_*.txt 2>/dev/null \
  | cut -d'|' -f3 | awk -F'/' '{print $1}' | tr -d ' ' | sort -h | tail -1)
APP_MEM_PCT_PEAK=$(grep -h '^myfave-app' ${MON_DIR}/docker_*.txt 2>/dev/null \
  | cut -d'|' -f4 | tr -d '% ' | sort -n | tail -1)

# ----- 시스템 메모리 -----
SYS_MEM_AVAIL_MIN=$(awk '{print $7}' ${MON_DIR}/mem_*.txt 2>/dev/null \
  | grep -v '^$' | sort -h | head -1)

# ----- LoadAvg -----
LOADAVG_PEAK=$(awk '{print $1}' ${MON_DIR}/loadavg_*.txt 2>/dev/null \
  | sort -n | tail -1)

# ---------- 출력 ----------

{
  echo "# Monitor 분석 결과 — ${SCENARIO} ${VERSION} (${DATE})"
  echo
  echo "- 모니터링 폴링 횟수: ${POLLING_COUNT}회"
  echo "- 모니터링 기간: ${DUR}초 (${START_TS} ~ ${END_TS})"
  echo
  echo "## HTTP & 백엔드 처리량 (Actuator)"
  echo
  echo "| 메트릭 | avg | peak |"
  echo "|---|---|---|"
  echo "| HTTP 요청 누적 | - | ${HTTP_REQS_TOTAL:-N/A} |"
  echo "| HTTP RPS (avg) | ${RPS_AVG} | - |"
  echo "| Tomcat busy threads | - | ${TOMCAT_BUSY_PEAK:-N/A} (한계 400) |"
  echo "| Tomcat current threads | - | ${TOMCAT_CONN_PEAK:-N/A} |"
  echo
  echo "## JVM"
  echo
  echo "| 메트릭 | avg | peak |"
  echo "|---|---|---|"
  echo "| Heap used | ${HEAP_AVG} | ${HEAP_PEAK} |"
  echo "| GC pause max (sec) | - | ${GC_PAUSE_MAX:-N/A} |"
  echo "| GC count 누적 | - | ${GC_COUNT:-N/A} |"
  echo
  echo "## Hikari Connection Pool"
  echo
  echo "| 메트릭 | peak | 한계 |"
  echo "|---|---|---|"
  echo "| hikari active | ${HIKARI_ACTIVE_PEAK:-N/A} | ${HIKARI_MAX:-50} |"
  echo "| hikari pending | ${HIKARI_PENDING_PEAK:-0} | - |"
  echo
  if [ "${HIKARI_PENDING_PEAK:-0}" -gt 0 ] 2>/dev/null; then
    echo "⚠️ hikari pending > 0 — 커넥션 풀 부족 감지 (응답 시간 지연 원인)"
    echo
  fi
  echo "## DB Locks (peak per polling tick)"
  echo
  echo "| 락 종류 | peak |"
  echo "|---|---|"
  for mode in "${!LOCK_PEAKS[@]}"; do
    echo "| $mode | ${LOCK_PEAKS[$mode]} |"
  done | sort
  echo
  echo "## DB Activity (peak per polling tick)"
  echo
  echo "| state | peak |"
  echo "|---|---|"
  for state in "${!DB_ACT_PEAKS[@]}"; do
    echo "| $state | ${DB_ACT_PEAKS[$state]} |"
  done | sort
  echo
  echo "## Transaction Rate (전체 모니터링 기간 기준)"
  echo
  echo "| 메트릭 | 값 |"
  echo "|---|---|"
  echo "| 모니터링 기간 (초) | ${DUR} |"
  echo "| commit 증가량 | +${COMMIT_TOTAL_DIFF:-N/A} |"
  echo "| rollback 증가량 | +${ROLLBACK_TOTAL_DIFF:-N/A} |"
  echo "| deadlock 증가량 | +${DEADLOCK_DIFF:-N/A} |"
  echo "| commit rate (avg) | ${COMMIT_RATE} commits/s |"
  echo "| rollback rate (avg) | ${ROLLBACK_RATE} rollbacks/s |"
  echo
  echo "## 시스템 리소스 (운영 EC2)"
  echo
  echo "| 메트릭 | peak |"
  echo "|---|---|"
  echo "| 백엔드 컨테이너 CPU | ${APP_CPU_PEAK:-N/A}% |"
  echo "| 백엔드 컨테이너 Memory | ${APP_MEM_PEAK:-N/A} (${APP_MEM_PCT_PEAK:-N/A}%) |"
  echo "| 시스템 load avg (1m) | ${LOADAVG_PEAK:-N/A} |"
  echo "| 시스템 Mem available 최저 | ${SYS_MEM_AVAIL_MIN:-N/A} |"
  echo
} | tee "$OUT_FILE"

echo
echo "[analyze] 결과 저장: $OUT_FILE"
