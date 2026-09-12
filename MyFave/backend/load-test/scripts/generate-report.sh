#!/usr/bin/env bash
# 부하 테스트 결과 보고서 자동 생성.
# - _template.md 복사
# - before/after raw 데이터를 해당 섹션에 부착
# - analyze-monitor.sh 결과를 monitor 섹션에 부착
# - k6 콘솔 로그를 부착
#
# 사용법:
#   ./generate-report.sh <scenario> <version> <변경요약> [date]
#
# 예시:
#   ./generate-report.sh d v1 baseline
#   ./generate-report.sh a v2 index 2026-05-23
#   ./generate-report.sh e v1 baseline
#
# 출력:
#   ~/loadtest-results/{scenario}_{version}_{date}/report.md
#   (사용자가 k6 콘솔 로그를 같은 폴더에 k6_output.log로 두면 자동 첨부)

set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <scenario> <version> <변경요약> [date]"
  echo "  변경요약: baseline / index / cache / pool-tune 등 한 단어"
  exit 1
fi

SCENARIO=$1
VERSION=$2
CHANGE=$3
DATE=${4:-$(date +%Y-%m-%d)}

BASE_DIR="$HOME/loadtest-results/${SCENARIO}_${VERSION}_${DATE}"

if [ ! -d "$BASE_DIR" ]; then
  echo "ERROR: 결과 폴더 없음 — $BASE_DIR"
  echo "       먼저 collect-results.sh를 before/after로 실행했는지 확인하세요."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATE="$SCRIPT_DIR/../docs/results/_template.md"
OUT_FILE="$BASE_DIR/report.md"

if [ ! -f "$TEMPLATE" ]; then
  echo "ERROR: 템플릿 없음 — $TEMPLATE"
  exit 1
fi

# 1. 템플릿 복사
cp "$TEMPLATE" "$OUT_FILE"

# 2. 헤더 라인 메타정보 자동 채우기 (간단한 sed 치환)
COMMIT_HASH=$(cd ~/MyFave 2>/dev/null && git rev-parse --short HEAD 2>/dev/null || echo "N/A")
TODAY_TIME=$(date +"%Y-%m-%d %H:%M %Z")
USER_NAME=$(whoami)

# 첫 줄(title)에 시나리오/버전/변경요약 박기
sed -i.bak "1c\\
# 부하 테스트 결과 — 시나리오 ${SCENARIO^^} ${VERSION}-${CHANGE} (${DATE})" "$OUT_FILE"
rm -f "${OUT_FILE}.bak"

# 3. 첨부 — 보고서 맨 아래에 자동 데이터 부착
{
  echo
  echo "---"
  echo
  echo "# 📊 자동 첨부 자료"
  echo
  echo "이 아래는 \`generate-report.sh\` 가 자동 첨부한 raw 데이터입니다."
  echo "위 섹션의 표는 본 자료를 보고 직접 채우세요."
  echo

  # ----- 메타정보 자동 -----
  echo "## A. 자동 추출 메타정보"
  echo
  echo "| 항목 | 값 |"
  echo "|---|---|"
  echo "| 시나리오 | ${SCENARIO} |"
  echo "| 버전 | ${VERSION}-${CHANGE} |"
  echo "| 측정일 | ${DATE} |"
  echo "| 보고서 생성 시각 | ${TODAY_TIME} |"
  echo "| 작성자 | ${USER_NAME} |"
  echo "| 백엔드 commit | ${COMMIT_HASH} |"
  echo "| 결과 폴더 | ${BASE_DIR} |"
  echo

  # ----- before raw -----
  if [ -d "$BASE_DIR/before" ]; then
    echo "## B. Before (시작 전 측정)"
    echo
    for f in tx_stats.txt stock.txt stock_summary.txt memory.txt docker_stats.txt backend_commit.txt timestamp_local.txt; do
      [ -f "$BASE_DIR/before/$f" ] || continue
      echo "### before/$f"
      echo
      echo '```'
      cat "$BASE_DIR/before/$f"
      echo '```'
      echo
    done
  fi

  # ----- monitor 분석 -----
  if [ -f "$BASE_DIR/analyze.md" ]; then
    echo "## C. Monitor 폴링 분석 (analyze-monitor.sh 결과)"
    echo
    # 첫 번째 #제목 라인은 제거 (보고서 흐름에 맞춤)
    tail -n +2 "$BASE_DIR/analyze.md"
    echo
  elif [ -d "$BASE_DIR/monitor" ]; then
    echo "## C. Monitor 폴링 분석"
    echo
    echo "⚠️ \`analyze-monitor.sh ${SCENARIO} ${VERSION} ${DATE}\` 를 먼저 실행하세요."
    echo "monitor 폴더는 있지만 analyze.md 가 없습니다."
    echo
    POLLING_COUNT=$(ls "$BASE_DIR/monitor/metrics_"*.txt 2>/dev/null | wc -l | tr -d ' ')
    echo "- 폴링 횟수: ${POLLING_COUNT}회 (raw 데이터)"
    echo
  fi

  # ----- after raw -----
  if [ -d "$BASE_DIR/after" ]; then
    echo "## D. After (끝난 후 측정)"
    echo
    for f in tx_stats.txt stock.txt stock_summary.txt order_status.txt payment_status.txt oversell_check.txt chat_messages.txt memory.txt docker_stats.txt backend_errors.txt timestamp_local.txt; do
      [ -f "$BASE_DIR/after/$f" ] || continue
      echo "### after/$f"
      echo
      echo '```'
      cat "$BASE_DIR/after/$f"
      echo '```'
      echo
    done
  fi

  # ----- k6 콘솔 로그 -----
  K6_LOG="$BASE_DIR/k6_output.log"
  if [ -f "$K6_LOG" ]; then
    echo "## E. k6 콘솔 로그"
    echo
    echo '```'
    cat "$K6_LOG"
    echo '```'
    echo
  else
    echo "## E. k6 콘솔 로그"
    echo
    echo "⚠️ k6 콘솔 로그 파일이 없습니다."
    echo "k6 EC2에서 실행 후 다음과 같이 운영 EC2로 복사하세요:"
    echo ""
    echo '```bash'
    echo "# k6 EC2"
    echo "BASE_URL=https://api.myfave.shop/api/v1 k6 run scenarios/scenario-${SCENARIO}-*.js 2>&1 | tee /tmp/k6_${SCENARIO}_${VERSION}.log"
    echo "scp /tmp/k6_${SCENARIO}_${VERSION}.log <운영-ec2>:${K6_LOG}"
    echo ""
    echo "# 그 후 운영 EC2에서 generate-report.sh 재실행"
    echo '```'
    echo
  fi

  # ----- 결과 폴더 파일 인덱스 -----
  echo "## F. 첨부 폴더 파일 인덱스"
  echo
  echo '```'
  find "$BASE_DIR" -maxdepth 2 -type f 2>/dev/null | sort
  echo '```'

} >> "$OUT_FILE"

echo "[generate-report] 보고서 생성 완료: $OUT_FILE"
echo
echo "다음 단계:"
echo "  1. less $OUT_FILE  → 자동 첨부 자료 확인 (B/C/D/E 섹션)"
echo "  2. 위 자료를 보고 본 보고서 위쪽 빈 표 직접 채우기"
echo "  3. Grafana 스크린샷 캡처 후 같은 폴더에 저장"
echo "  4. develop 브랜치에 backend/load-test/docs/results/ 로 커밋"
