#!/usr/bin/env bash
# 부하 테스트 결과 자동 수집 — 운영 EC2에서 실행.
# 시나리오 시작 전 / 부하 중 폴링 / 끝난 후 세 가지 phase 지원.
#
# 사용법:
#   ./collect-results.sh <scenario> <version> <phase> [interval]
#   - scenario: d / a / e
#   - version:  v1 / v2 / ...
#   - phase:    before / monitor / after
#   - interval: monitor 모드 폴링 간격 (초, 기본 5)
#
# 예시:
#   ./collect-results.sh d v1 before
#   # 부하 중 폴링 (백그라운드)
#   ./collect-results.sh d v1 monitor 5 &
#   PID=$!
#   # ... k6 시나리오 실행 ...
#   kill $PID    # 폴링 종료
#   ./collect-results.sh d v1 after
#
# 출력: ~/loadtest-results/{scenario}_{version}_{date}/{phase}/

set -euo pipefail

if [ "$#" -lt 3 ]; then
  echo "Usage: $0 <scenario> <version> <phase> [interval]"
  echo "  scenario: d / a / e"
  echo "  version:  v1 / v2 / ..."
  echo "  phase:    before / monitor / after"
  echo "  interval: monitor 모드 폴링 간격 (초, 기본 5)"
  exit 1
fi

SCENARIO=$1
VERSION=$2
PHASE=$3
POLLING_INTERVAL=${4:-5}

case "$SCENARIO" in
  d|a|e) ;;
  *) echo "ERROR: scenario는 d / a / e 중 하나"; exit 1 ;;
esac

case "$PHASE" in
  before|after|monitor) ;;
  *) echo "ERROR: phase는 before / after / monitor 중 하나"; exit 1 ;;
esac

DATE=$(date +%Y-%m-%d)
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

BASE_DIR="$HOME/loadtest-results/${SCENARIO}_${VERSION}_${DATE}"
OUT_DIR="$BASE_DIR/$PHASE"
mkdir -p "$OUT_DIR"

DB_CONTAINER="myfave-postgres"
APP_CONTAINER="myfave-app"
ACTUATOR_URL="${ACTUATOR_URL:-http://localhost:8080/api/v1/actuator/prometheus}"

# ---------- monitor 모드 — 부하 중 백그라운드 폴링 ----------

if [ "$PHASE" = "monitor" ]; then
  echo "[monitor] scenario=$SCENARIO version=$VERSION interval=${POLLING_INTERVAL}s"
  echo "[monitor] actuator: $ACTUATOR_URL"
  echo "[monitor] output: $OUT_DIR"
  echo "[monitor] Ctrl+C 또는 kill로 종료"
  echo

  ITER=0
  trap 'echo "[monitor] 종료 (총 $ITER회 폴링)"; exit 0' INT TERM

  while true; do
    TS=$(date +%H%M%S)
    ITER=$((ITER + 1))

    # 1. 백엔드 actuator/prometheus 메트릭
    curl -sf "$ACTUATOR_URL" \
      -o "$OUT_DIR/metrics_${TS}.txt" 2>/dev/null \
      || echo "[monitor $TS] actuator fetch failed" >> "$OUT_DIR/_errors.log"

    # 2. DB 트랜잭션 통계 (시계열)
    sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave -t -A \
      -c "SELECT xact_commit, xact_rollback, deadlocks FROM pg_stat_database WHERE datname='myfave';" \
      > "$OUT_DIR/tx_${TS}.txt" 2>/dev/null || true

    # 3. DB 락 분포 (myfave DB만)
    sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave -t -A \
      -c "SELECT mode, count(*) FROM pg_locks WHERE database = (SELECT oid FROM pg_database WHERE datname='myfave') GROUP BY mode ORDER BY count DESC;" \
      > "$OUT_DIR/locks_${TS}.txt" 2>/dev/null || true

    # 4. DB 활성 connection 수
    sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave -t -A \
      -c "SELECT state, count(*) FROM pg_stat_activity WHERE datname='myfave' GROUP BY state;" \
      > "$OUT_DIR/db_activity_${TS}.txt" 2>/dev/null || true

    # 5. 컨테이너 stats (CPU / 메모리 / 네트워크)
    sudo docker stats --no-stream \
      --format "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.NetIO}}|{{.BlockIO}}" \
      > "$OUT_DIR/docker_${TS}.txt" 2>/dev/null || true

    # 6. 시스템 메모리 한 줄
    free -h | grep '^Mem' > "$OUT_DIR/mem_${TS}.txt" 2>/dev/null || true

    # 7. 시스템 load average 한 줄
    cat /proc/loadavg > "$OUT_DIR/loadavg_${TS}.txt" 2>/dev/null || true

    # 진행 표시 (10회마다)
    if [ $((ITER % 10)) -eq 0 ]; then
      echo "[monitor] $ITER회 수집 완료 ($(date +%H:%M:%S))"
    fi

    sleep "$POLLING_INTERVAL"
  done
fi

echo "[collect] scenario=$SCENARIO version=$VERSION phase=$PHASE"
echo "[collect] output: $OUT_DIR"
echo

# ---------- 공통 수집 ----------

# 1. DB 트랜잭션 통계
echo "[1/8] DB transaction stats"
sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
  -c "SELECT xact_commit, xact_rollback, deadlocks FROM pg_stat_database WHERE datname='myfave';" \
  > "$OUT_DIR/tx_stats.txt" 2>&1

# 2. 상품 stock 상태
echo "[2/8] product stock"
sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
  -c "SELECT product_id, stock_quantity, is_soldout FROM products WHERE product_id <= 10 ORDER BY product_id;" \
  > "$OUT_DIR/stock.txt" 2>&1

# 3. 상품 stock 합계
echo "[3/8] stock summary"
sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
  -c "SELECT SUM(stock_quantity) AS remaining_stock, COUNT(CASE WHEN is_soldout THEN 1 END) AS sold_out_count FROM products WHERE product_id <= 10;" \
  > "$OUT_DIR/stock_summary.txt" 2>&1

# 4. 시스템 메모리
echo "[4/8] system memory"
free -h > "$OUT_DIR/memory.txt"
echo "" >> "$OUT_DIR/memory.txt"
echo "--- /proc/meminfo top ---" >> "$OUT_DIR/memory.txt"
head -5 /proc/meminfo >> "$OUT_DIR/memory.txt"

# 5. 컨테이너 stats
echo "[5/8] docker stats"
sudo docker stats --no-stream \
  --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" \
  > "$OUT_DIR/docker_stats.txt" 2>&1

# 6. 백엔드 commit hash
echo "[6/8] backend commit hash"
(cd ~/MyFave && git rev-parse HEAD && git log -1 --pretty=format:'%h %s%n%cd' --date=iso) \
  > "$OUT_DIR/backend_commit.txt" 2>&1

# 7. 시간 (정확한 측정 시각)
date -u +"%Y-%m-%dT%H:%M:%SZ" > "$OUT_DIR/timestamp_utc.txt"
date +"%Y-%m-%d %H:%M:%S %Z" > "$OUT_DIR/timestamp_local.txt"

# ---------- phase=after 전용 추가 수집 ----------

if [ "$PHASE" = "after" ]; then

  # 8a. 오버셀 검증 (D 필수, A/E도 부수 정보로 확인)
  echo "[7/8] oversell check"
  sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
    -c "SELECT p.product_id, COUNT(*) AS paid_count FROM order_items oi JOIN orders o ON oi.orders_id = o.orders_id JOIN products p ON oi.products_id = p.product_id WHERE o.order_status = 'PAID' AND p.product_id <= 10 AND o.created_at > NOW() - INTERVAL '15 minutes' GROUP BY p.product_id ORDER BY p.product_id;" \
    > "$OUT_DIR/oversell_check.txt" 2>&1 || true

  # 8b. 주문 상태 분포 (A 필수)
  echo "[8/8] order/payment status distribution"
  sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
    -c "SELECT order_status, COUNT(*) FROM orders WHERE created_at > NOW() - INTERVAL '15 minutes' GROUP BY order_status ORDER BY order_status;" \
    > "$OUT_DIR/order_status.txt" 2>&1 || true

  # 8c. 결제 상태 분포
  sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
    -c "SELECT payment_status, COUNT(*) FROM payments WHERE created_at > NOW() - INTERVAL '15 minutes' GROUP BY payment_status ORDER BY payment_status;" \
    > "$OUT_DIR/payment_status.txt" 2>&1 || true

  # 8d. 시나리오 E 전용 — 채팅 메시지 수
  if [ "$SCENARIO" = "e" ]; then
    sudo docker exec "$DB_CONTAINER" psql -U postgres -d myfave \
      -c "SELECT chat_room_id, COUNT(*) AS message_count FROM chat_messages WHERE created_at > NOW() - INTERVAL '15 minutes' GROUP BY chat_room_id ORDER BY chat_room_id;" \
      > "$OUT_DIR/chat_messages.txt" 2>&1 || true
  fi

  # 8e. ERROR/WARN 로그 샘플 (최대 100줄)
  echo "[+] backend error/warn logs"
  sudo docker logs "$APP_CONTAINER" --since "15m" 2>&1 \
    | grep -iE "error|exception|warn" \
    | tail -100 \
    > "$OUT_DIR/backend_errors.txt" 2>&1 || true

fi

echo
echo "[collect] done. output dir:"
echo "  $OUT_DIR"
echo
echo "수집된 파일:"
ls -la "$OUT_DIR"
