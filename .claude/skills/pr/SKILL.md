# PR 생성 스킬

## 설명

현재 브랜치의 변경사항을 분석하여 PR을 생성한다.

## 참고 파일

- `CLAUDE.md` — 프로젝트 컨벤션 및 아키텍처
- `.github/pull_request_template.md` — PR 템플릿 원본

## 절차

1. `git status`로 현재 상태 확인
2. `git log main..HEAD --oneline`으로 커밋 목록 확인
3. `git diff main...HEAD`로 전체 변경사항 분석
4. 아래 템플릿 양식에 맞춰 PR 제목과 본문 작성
5. `gh pr create`로 PR 생성

## PR 템플릿

```markdown
## 작업 내용


## 변경사항
-

### API 스펙 (REST API 추가 시)

| 항목 | 값 |
|---|---|
| Method | `GET` / `POST` ... |
| Path | `/api/v1/...` |
| 파라미터 1 | 타입, 필수/선택, 제약 |
| ... | ... |

### 시나리오별 응답 (REST API 추가 시)

**정상** — `요청 예시`
```json
HTTP 200
{ ... }
```

**검증 실패** — `요청 예시`
```json
HTTP 400
{ "error": { "message": "..." } }
```

## 테스트 결과
- [x] `./gradlew test` 통과 (N tests, 0 failed)
- [x] [수동 검증 항목]
- [ ] [리뷰어가 검증할 항목]

## 관련 이슈

```

## 규칙

- PR 제목은 커밋 컨벤션을 따른다 (feat:, fix:, chore: 등)
- 70자 이내로 작성
- 본문은 반드시 위 템플릿 양식을 그대로 사용한다
- REST API 변경이 포함된 경우 API 스펙과 시나리오별 응답을 반드시 작성한다
- REST API 변경이 없으면 API 스펙/시나리오별 응답 섹션은 생략한다
- 변경사항은 커밋 단위가 아니라 기능 단위로 정리한다
- base 브랜치는 `main`을 기본으로 한다
