# Load Test 결과 수집 / 분석 / 보고서 자동화

`collect-results.sh` → `analyze-monitor.sh` → `generate-report.sh` 3단계로
부하 테스트 측정값을 자동 수집하고 보고서를 생성한다.

## 파일 구성

```
backend/load-test/
├── scripts/
│   ├── collect-results.sh      # 데이터 수집 (before/monitor/after)
│   ├── analyze-monitor.sh      # 폴링 raw → peak/avg 정리
│   ├── generate-report.sh      # 템플릿 + 수집 데이터 → report.md
│   └── README.md               # 본 파일
└── docs/results/
    ├── _template.md            # 보고서 빈 템플릿
    └── scenario-{d|a|e}_v{N}-{변경요약}_{YYYY-MM-DD}.md  # 채워진 보고서
```

## 출력 폴더 구조 (자동 생성)

```
~/loadtest-results/{scenario}_{version}_{date}/
├── before/             # 시작 전 측정 (8개 파일)
├── monitor/            # 부하 중 폴링 (시계열, 수십~수백 개)
├── after/              # 끝난 후 측정 (13개 파일)
├── k6_output.log       # k6 콘솔 로그 (사용자가 복사)
├── analyze.md          # analyze-monitor.sh 출력
└── report.md           # generate-report.sh 최종 보고서
```

## 전체 흐름 (운영 EC2 기준)

```bash
SCENARIO=d            # 또는 a / e
VERSION=v1            # baseline=v1, 그 후 v2/v3...
CHANGE=baseline       # 또는 index/cache/pool-tune 등

# 1. 시드 리셋 + 시작 전 측정
sudo docker exec -i myfave-postgres psql -U postgres -d myfave < ~/MyFave/backend/load-test/seed/reset.sql
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION before

# 2. 부하 중 백그라운드 폴링 시작
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION monitor 5 &
MONITOR_PID=$!

# 3. (사용자) k6 EC2에서 시나리오 실행 + 로그 저장
#    BASE_URL=https://api.myfave.shop/api/v1 k6 run scenarios/scenario-d-soldout.js 2>&1 | tee /tmp/k6.log
#    scp /tmp/k6.log <운영-ec2>:~/loadtest-results/${SCENARIO}_${VERSION}_$(date +%Y-%m-%d)/k6_output.log

# 4. 폴링 종료 + 끝난 후 측정
kill $MONITOR_PID
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION after

# 5. 폴링 데이터 분석
~/MyFave/backend/load-test/scripts/analyze-monitor.sh $SCENARIO $VERSION

# 6. 보고서 생성
~/MyFave/backend/load-test/scripts/generate-report.sh $SCENARIO $VERSION $CHANGE
```

→ 최종 산출물: `~/loadtest-results/d_v1_2026-05-23/report.md`

## 각 스크립트 상세

### collect-results.sh

```
사용법:
  collect-results.sh <scenario> <version> <phase> [interval]
  - phase: before / monitor / after
  - interval: monitor 폴링 간격(초, 기본 5)

phase별 수집 내용:
  - before  : DB tx 통계, stock, memory, docker stats, commit hash, 시각
  - monitor : 5초마다 actuator/prometheus, DB tx, DB locks, DB activity,
              docker stats, memory, loadavg
  - after   : before + 오버셀 검증, 주문/결제 상태 분포, 채팅 메시지(E),
              백엔드 ERROR/WARN 로그
```

### analyze-monitor.sh

```
사용법:
  analyze-monitor.sh <scenario> <version> [date]

monitor 폴더의 raw 시계열 데이터에서 다음 항목 자동 추출:
  - HTTP RPS 평균 / Tomcat busy peak
  - JVM Heap peak/avg, GC pause max, GC count
  - Hikari active/pending peak
  - DB Lock 모드별 peak
  - DB activity (active/idle) peak
  - Transaction commit/rollback rate 평균, deadlock 증가량
  - 백엔드 컨테이너 CPU/Memory peak
  - 시스템 load avg / mem available 최저

출력: Markdown 표 형식 (stdout + analyze.md)
```

### generate-report.sh

```
사용법:
  generate-report.sh <scenario> <version> <변경요약> [date]

처리:
  1. _template.md 복사 → report.md
  2. 메타정보 자동 채우기 (날짜, commit hash 등)
  3. before/after raw 데이터 부착
  4. analyze.md 결과 부착
  5. k6_output.log 부착 (있으면)
```

## k6 로그 수집 (사용자 수작업)

k6 EC2에서 부하 테스트할 때 콘솔 출력을 파일로 저장하고 운영 EC2로 복사:

```bash
# k6 EC2
cd ~/MyFave/backend/load-test/k6
BASE_URL=https://api.myfave.shop/api/v1 k6 run scenarios/scenario-d-soldout.js 2>&1 \
  | tee /tmp/k6_d_v1.log

# 운영 EC2로 복사 (k6 EC2가 운영 EC2에 SSH/SCP 접근 가능한 경우)
scp /tmp/k6_d_v1.log <운영-ec2-주소>:~/loadtest-results/d_v1_$(date +%Y-%m-%d)/k6_output.log
```

→ 또는 복사 대신 k6 출력을 직접 보면서 핵심 수치를 보고서에 수작업으로 박아도 됨.

## 시나리오별 권장 흐름

### 시나리오 D (45초)

```bash
SCENARIO=d VERSION=v1 CHANGE=baseline

sudo docker exec -i myfave-postgres psql -U postgres -d myfave < ~/MyFave/backend/load-test/seed/reset.sql
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION before
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION monitor 5 &
PID=$!
# (k6 EC2에서 시나리오 D 실행, 45초)
sleep 60   # 대기 (또는 k6 끝난 신호 받고)
kill $PID
~/MyFave/backend/load-test/scripts/collect-results.sh $SCENARIO $VERSION after
~/MyFave/backend/load-test/scripts/analyze-monitor.sh $SCENARIO $VERSION
~/MyFave/backend/load-test/scripts/generate-report.sh $SCENARIO $VERSION $CHANGE
```

### 시나리오 A (4분 10초)

위와 동일, `sleep 280` 으로 변경. reset.sql은 stock 차감 없는 시나리오라 생략 가능.

### 시나리오 E (4분 30초)

위와 동일, `sleep 300` 으로 변경. ChatRoom 1번이 활성 상태인지 확인 필요.

## 보고서 활용

생성된 `report.md` 는 두 부분으로 나뉨:

1. **위쪽 (사용자 채울 자리)** — `_template.md` 의 빈 표
2. **아래쪽 (자동 첨부)** — B(before) / C(monitor 분석) / D(after) / E(k6 로그) / F(파일 인덱스)

→ 아래쪽 자동 자료를 보고 위쪽 표를 직접 채워 완성.
→ 완성된 보고서는 `backend/load-test/docs/results/scenario-{d|a|e}_v{N}-{change}_{date}.md` 로 옮겨 develop 에 커밋.

## 트러블슈팅

- `actuator fetch failed` → 백엔드가 다운됐거나 8080 포트 안 열림. `_errors.log` 확인.
- analyze 결과에 `N/A` 다수 → monitor 폴링이 짧거나 actuator 응답 없음. 폴링 시간 확보.
- generate 결과에 k6 로그 없음 → k6 EC2에서 운영 EC2로 복사 필요.
