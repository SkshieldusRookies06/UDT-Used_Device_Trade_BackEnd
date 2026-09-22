# GitHub 규약 — UDT (백엔드·프론트 공통)

> 두 리포(`UDT-Used_Device_Trade-backend` · `UDT-Used_Device_Trade-frontend`)에 똑같이 적용된다.
> 정본은 이 파일 하나다. README·온보딩의 브랜치·커밋 절은 이 파일의 요약이고, 어긋나면 이 파일이 맞다.
> **초보 기준으로 썼다.** `rebase` · `force-push` · `cherry-pick`은 이 프로젝트에서 쓰지 않는다.

관련: [`역할분담.md`](역할분담.md) §2(수정 허용 파일) · §3.1(머지 거절 기준) ·
[`전체로드맵.md`](전체로드맵.md)(오늘 무슨 티켓인지) · `SPEC.md` §12(리포 2개 규칙)

---

## 0. 한 장 요약 (이것만 지켜도 된다)

```
아침   두 리포 모두  git checkout main && git pull
시작   git checkout -b be-<작업명>        (프론트는 fe-<작업명>)   ← main에서 판다
작업   티켓에 적힌 [수정 허용 파일]만 건드린다
끝     게이트 green → git add <파일들> → git commit -m "feat(범위): 한 줄 [T-###]" → git push -u origin be-<작업명>
PR     GitHub에서 Pull Request → 템플릿 채움 → 오너(BE-A / FE-A)에게 리뷰 요청
머지   오너만 누른다. 머지되면 브랜치 삭제 · 팀 채널에 "머지: T-### · <해시>" 한 줄
```

절대 하지 않는 것 — **`main`에 직접 push** · **`--force`** · **`rebase`** · **`.env`·`uploads/`·`seed-images/` 커밋** · **남의 파일 수정**.

---

## 1. 리포 2개

| 리포 | 오너(머지 권한) | 무엇이 사는가 |
|---|---|---|
| `UDT-Used_Device_Trade-backend` | **BE-A** | Spring Boot · `SPEC.md`(계약 정본) · `seams/`(게이트) · `mock/` · **`docs/`(제출 문서 13종 — 프론트 담당자도 여기에 커밋)** · `tasks/` |
| `UDT-Used_Device_Trade-frontend` | **FE-A** | React · `tasks/` · `onboarding/` |

- **형제 폴더로 클론**하고 폴더 이름을 바꾸지 않는다 (`npm run gate`가 `../UDT-Used_Device_Trade-backend`를 찾는다).
- 티켓 번호(`T-###`)는 두 리포가 **공유**한다. 같은 번호가 양쪽에 있으면 계약 개정 짝이다 (§6).
- 프론트 담당자가 백엔드 리포에 커밋하는 경우는 **`docs/` 아래 문서뿐**이다. 브랜치 접두사는 `docs-<문서명>`.

## 2. 브랜치

```
main                       정본. 항상 기동·빌드·게이트 green 상태를 유지한다
 ├── be-<작업명>            백엔드 작업 브랜치   예) be-product-list · be-jwt-filter
 ├── fe-<작업명>            프론트 작업 브랜치   예) fe-login-page · fe-mypage-tabs
 └── docs-<문서명>          문서만 고치는 브랜치 예) docs-api-design
```

규칙

1. **항상 최신 `main`에서 판다.** `git checkout main && git pull` 먼저.
2. **브랜치 하나 = 티켓 하나.** 티켓이 크면 브랜치를 쪼개지 말고 **티켓을 쪼갠다**(오너에게 요청).
3. 작업명은 **영문 소문자·하이픈**만. 한글·공백·대문자·슬래시 없음.
4. 머지된 브랜치는 지운다(GitHub의 "Delete branch" 버튼). 로컬은 `git branch -d be-<작업명>`.
5. `main`은 GitHub 설정에서 **보호**한다(팀장이 D1에 설정 · §8).

## 3. 커밋 메시지

```
<type>(<범위>): <한 줄 요약> [T-###]

type   feat | fix | docs | refactor | chore | test
범위   product · auth · transaction · dispute · me · wish · admin · config · common   (백엔드)
       pages · components · api · store · styles · router                              (프론트)
       spec · seams · mock · tasks · onboarding                                        (공통)
```

예

```
feat(product): 상품 목록 검색·페이징 API [T-006]
fix(auth): 만료 토큰에 401 JSON 봉투 반환 [T-005]
docs(spec): §4.5 Transaction에 buyerId·sellerId 추가 [T-017]
chore(config): CORS 허용 오리진에 5173 추가 [T-001]
test(transaction): BR-03 이중 확정 409 검증 [T-022]
```

규칙

- **한국어 한 줄, 50자 안팎.** 마침표 없음. "수정", "작업중", "ㅇㅇ" 같은 메시지 금지.
- **`[T-###]`는 필수.** 티켓 없는 커밋은 없다 — 티켓이 없으면 먼저 `tasks/`에 티켓을 만든다.
- 커밋 하나에 **하나의 의도.** 상품 API 고치면서 CSS도 손댔으면 커밋을 둘로 나눈다 (`git add -p` 또는 IntelliJ 부분 커밋).
- `git add -A` 대신 **파일을 지정해서 add**한다. 실수로 `.env`·`uploads/`가 들어가는 사고의 90%가 `-A`에서 난다.
- **커밋·push는 사람이 한다.** 에이전트에게 시키지 않는다(`CLAUDE.md` 금지 항목).

## 4. Pull Request

PR 없이 `main`에 들어가는 코드는 없다. 팀장 본인도 PR을 연다.

### 4.1 여는 법

1. push 하면 GitHub가 "Compare & pull request" 버튼을 띄운다. 누른다.
2. **base는 `main`**, compare는 내 브랜치인지 확인.
3. 제목은 커밋 메시지와 같은 형식: `feat(product): 상품 목록 검색·페이징 API [T-006]`
4. 본문은 **템플릿이 자동으로 채워진다**(`.github/PULL_REQUEST_TEMPLATE.md`). 칸을 지우지 말고 채운다.
5. Reviewers에 오너(백엔드 BE-A · 프론트 FE-A)를 지정한다. **T-009(상태 머신)는 BE-A 리뷰가 머지 조건**이다.
6. 팀 채널에 PR 링크 한 줄.

### 4.2 PR 본문에 반드시 들어가는 것

| 칸 | 내용 |
|---|---|
| 티켓 | `T-###` 링크 |
| 수용 기준 | 티켓의 `[수용 기준]`을 **체크박스로 옮겨 적고 전부 체크** |
| 게이트 출력 | `node seams/check-api.mjs` 또는 `npm run build` 결과를 **그대로 붙여넣기** (산문 "됐어요" 무효) |
| 수정 파일 확인 | 티켓 `[제약]`의 수정 허용 파일 밖을 건드리지 않았다는 체크 |
| 계약 변경 여부 | 있으면 짝 PR 링크 (§6) |

### 4.3 크기

**diff 한 화면(≈100줄) 이하.** `git diff --stat main` 으로 미리 확인한다. 넘으면 열지 말고 오너에게 티켓 분할을 요청한다.
예외는 오너가 사전에 인정한 경우뿐(예: T-002 엔티티 일괄, T-018 Thymeleaf 템플릿).

## 5. 리뷰·머지 (오너용)

머지 버튼은 **오너만** 누른다. 머지 방식은 **"Squash and merge"** 하나로 통일한다 —
`main` 로그가 티켓 단위로 남고, 초보의 "wip", "fix" 커밋이 정본 히스토리를 더럽히지 않는다.
squash 커밋 메시지는 PR 제목을 그대로 쓴다(`[T-###]` 포함 여부 확인).

### 5.1 거절 기준 — [`역할분담.md` §3.1](역할분담.md) 그대로

| 반려 | 판정 |
|---|---|
| 한 화면(≈100줄) 초과 | `git diff --stat` |
| 수정 허용 밖 파일 | diff 파일 목록 vs 티켓 `[제약]` |
| 게이트 red / 빌드 실패 | 본문에 붙인 출력 · 필요하면 오너가 직접 브랜치 받아서 재실행 |
| 전면 재작성 | diff에서 `-`가 `+`만큼 많다 |

되돌려 줄 말은 §3.1 표의 문장을 **그대로 복사**한다. 이유를 새로 쓰지 않는다.

### 5.2 리뷰 순서 (오너 · PR 하나 5분)

```
1) 본문 — 게이트 출력이 붙어 있나 · 체크박스 전부 체크됐나        (없으면 읽지 않고 반려)
2) Files changed 탭 — 파일 목록이 티켓 [제약] 안인가
3) diff를 위에서 아래로 한 번 읽는다 — 이해 안 되는 줄엔 코멘트
4) 계약 변경이면 짝 PR이 열려 있나 · backend 쪽이 먼저인가 (§6)
5) Squash and merge → Delete branch → 채널에 "머지: T-### · <해시>"
```

### 5.3 머지 후 (작성자)

```
git checkout main
git pull
git branch -d be-<작업명>
```

**다른 사람도 그날 안에 `main`을 pull** 한다 — 아침 루프의 첫 줄이 이것이다.

## 6. 계약(SPEC) 변경 — 두 리포 짝 맞추기

`SPEC.md` §4(API) · §5(에러 코드) · 응답 봉투를 바꾸는 변경은 **한 리포만 고치면 반드시 깨진다.**

```
1) SPEC.md 를 먼저 고친다 (백엔드 리포 · docs-spec-<내용> 또는 작업 브랜치 안에서)
2) 같은 티켓 번호로 두 PR을 연다
     backend  feat(transaction): 응답에 buyerId·sellerId 추가 [T-017]
     frontend feat(api): Transaction에 buyerId·sellerId 반영 [T-017]
   각 PR 본문 "계약 변경" 칸에 상대 PR 링크
3) 게이트(seams/check-api.mjs)·목 서버(mock/server.mjs)도 backend PR 안에서 같이 고친다
4) 오너가 backend를 먼저 머지 → frontend 머지
5) 팀 채널에 "T-017 양쪽 머지 완료" — 이 줄이 올라와야 티켓이 닫힌다 (SPEC §12)
```

- 구두 합의는 무효다. **SPEC.md에 머지되기 전까지 계약은 바뀌지 않은 것**이다.
- 백엔드 담당자가 프론트 리포를 대신 고치지 않는다(반대도). 상대 오너에게 PR을 요청한다.

## 7. 충돌

**초보용 · `rebase` 안 쓴다.** `팀원용-한장.md`의 레시피와 같다.

```
1) 절대 억지로 풀지 않는다. 채널에 "T-0XX 충돌, <파일명>" 한 줄
2) git fetch origin && git checkout origin/main -- <충돌난 파일>     ← main 것을 그대로 받는다
3) 내가 넣었던 줄만 다시 넣는다 (내 diff는 git stash show -p 나 IDE 로컬 히스토리에 있다)
4) 게이트 돌려서 green 확인 → 커밋 → push (같은 브랜치 · force 아님)
```

- **공용 파일**(`SPEC.md` · `data.sql` · `application.yml` · `package.json` · `pom.xml`)에서 충돌 나면 2번을 무조건 한다.
- `package-lock.json`은 합치지 않는다. main 것을 받고 `npm install`로 재생성.
- 충돌을 줄이는 방법은 하나 — **작은 PR을 자주, 매일 아침 pull.** 3일 묵힌 브랜치는 반드시 충돌한다.

## 8. GitHub 리포 설정 (팀장 · D1 · 10분)

Settings → Branches → Add branch protection rule → `main`

- [x] Require a pull request before merging (approvals: 1)
- [x] Do not allow bypassing the above settings
- [ ] Require status checks — CI가 없으므로 끈다 (게이트는 PR 본문 붙여넣기로 대신한다)

Settings → General → Pull Requests

- [x] Allow squash merging (기본 메시지: Pull request title)
- [ ] Allow merge commits — 끈다
- [ ] Allow rebase merging — 끈다
- [x] Automatically delete head branches

Collaborators: 7명 전원 **Write**. Admin은 팀장만.

## 9. 커밋 금지 목록

`.gitignore`에 이미 들어 있지만, **한 번 올라간 시크릿은 삭제 커밋으로도 안 지워진다.** 커밋 전 `git status`를 눈으로 본다.

| 절대 커밋하지 않는다 | 이유 |
|---|---|
| `.env` · `application-secret.yml` · 실제 JWT 시크릿 · DB 비밀번호 | 시크릿. `.env.example`에 **키 이름만** |
| `uploads/*` (`.gitkeep` 제외) | 사용자 업로드 파일 |
| `seed-images/*` (`README.md` 제외) | 시연용 사진 — 팀 드라이브로 공유 |
| `target/` · `node_modules/` · `dist/` | 빌드 산출물 |
| `.idea/` · `*.iml` · `.vscode/` | IDE 개인 설정 |
| 작업 폴더 루트의 `00-*_내부전용.md` · `README-종합본.md` | 팀 내부 문서 — 두 리포 바깥(작업 폴더 루트)에만 둔다. **리포 안으로 옮기지 않는다** |
| 팀원 실명·연락처가 들어간 파일 | 오너 코드(BE-A 등)로만 적는다 |

이미 올렸다면 → 즉시 오너에게 말한다. 시크릿이면 **값을 바꾸는 것**이 유일한 해결이다(히스토리 삭제는 하지 않는다).

## 10. 일일 리듬과 git

```
아침 (5분)   두 리포 git checkout main && git pull → 로드맵에서 오늘 칸 → 브랜치 판다
낮           티켓 [수정 허용 파일]만 · 커밋은 의도 단위로 여러 번 (push는 끝에 한 번이어도 됨)
하루 끝 (5분) 게이트 green → git diff --stat main (100줄 이하) → push → PR → 채널 한 줄
             worklog/<내이름>/D##.md 3줄 (머지 · 열었음 · 막힘)
```

`worklog/`는 **자기 폴더만** 쓰고 자기 브랜치에서 같이 올린다. 남의 worklog는 읽지도 고치지도 않는다.

## 11. Windows에서 자주 나는 것

| 증상 | 처치 |
|---|---|
| `warning: LF will be replaced by CRLF` | 무해. 한 번만 `git config --global core.autocrlf true` |
| 한글 파일명이 `\341\204...`로 보인다 | `git config --global core.quotepath false` |
| 실행 파일 권한(mode 100644 ↔ 100755) diff가 뜬다 | `git config core.fileMode false` |
| PowerShell에서 `git checkout origin/main -- <파일>`이 안 된다 | 파일 경로를 따옴표로 감싼다: `git checkout origin/main -- "src/main/resources/data.sql"` |
| push가 거절된다 (`rejected … fetch first`) | 남이 같은 브랜치에 올렸다. `git pull` 후 다시 push. **`--force` 금지** |
| `main`에 실수로 커밋했다 | push 전이면 `git branch be-<작업명>` → `git reset --hard origin/main` → `git checkout be-<작업명>`. push 후면 오너에게 말한다 |

## 12. 자주 쓰는 명령 모음

```bash
# 시작
git checkout main && git pull
git checkout -b be-product-list

# 상태 보기
git status
git diff --stat main            # 100줄 넘는지
git log --oneline -10

# 커밋
git add src/main/java/com/rookies6/udt/service/ProductService.java
git commit -m "feat(product): 상품 목록 검색·페이징 API [T-006]"
git push -u origin be-product-list

# 머지 후 정리
git checkout main && git pull
git branch -d be-product-list

# 실수 되돌리기 (커밋 전)
git checkout -- <파일>           # 한 파일 원복
git stash                        # 전부 잠시 치우기 / git stash pop 으로 복구
```
