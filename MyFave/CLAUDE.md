# MyFave Global Project Guide

## 1. Project Context
- **Description**: 1인 인플루언서 플리마켓 쇼핑몰 백엔드
- **Root Directory Rules**: AI는 작업 시작 전 반드시 이 파일을 읽고, 하위 도메인 폴더(`backend/`, `frontend/`)로 진입하면 해당 폴더의 `claude.md`를 즉시 참조하세요.

## 2. Token-Saving AI Guidelines
- **Zero Filler**: 인사, 서론, 결론, "예, 알겠습니다" 등 불필요한 텍스트를 출력하지 마세요.
- **Explicit Context**: "장바구니 로직 수정해줘" 대신 "domain/cart의 Service와 Controller를 읽고 로직 수정해줘"와 같이 구체적으로 요구하세요.
- **Surgical Code**: 수정이 필요한 부분만 정확히 `replace` 또는 `write_file` 하세요. 전체 파일 재출력은 금지입니다.

## 3. GitHub & Workflow
- **Issue-Based**: 반드시 Issue(#번호) 생성 후 관련 브랜치에서 작업하세요.
- **Commit Style**: `.gitmessage.txt` 템플릿을 준수하고, "Why"에 집중하여 작성하세요.
- **No Co-Authored-By**: 커밋 메시지에 `Co-Authored-By: Claude` 트레일러를 절대 추가하지 마세요.
- **Safe Push**: Push 전 반드시 `git pull`을 수행하여 충돌을 로컬에서 해결하세요.

## 4. Key Global Commands
- **Infrastructure**: `docker-compose up -d` (PostgreSQL, Redis 실행)
- **Check Status**: `git status && git branch`
- **Full Build**: `cd backend && ./gradlew build`
