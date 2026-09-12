# 기능 스펙 (Spec)

Figma 화면설계서를 기준으로 백엔드가 제공해야 할 API·도메인·DB·패키지 구조를 정리한다.
구현 전 합의용 문서이며, 구현 후에는 REST Docs(`src/docs/asciidoc`)가 정본이 된다.

| 문서 | 범위 | 담당 |
|---|---|---|
| [mypage.md](./mypage.md) | 마이페이지 `MY-01 ~ MY-07` — 계정설정·탈퇴·공지·FAQ·1:1 문의·약관 | 개발자 A |
| [admin.md](./admin.md) | 관리자 백엔드 — 관리자 인증, 회원·공지·FAQ·약관·문의 관리, 대시보드 | 개발자 A (스팟 관리는 B) |

## 화면 ID ↔ 스펙 매핑

| 화면 ID | 화면명 | 스펙 |
|---|---|---|
| MY-01-01 / 01-02 | 마이 메인 (회원 / 비회원) | mypage.md §1 |
| MY-01-03 | 테마 변경 | mypage.md §1 (클라이언트 전용) |
| MY-02-01 | 계정설정 | mypage.md §2 |
| MY-02-02 | 닉네임 변경 | mypage.md §2 |
| MY-02-03 | 국가 변경 | mypage.md §2 |
| MY-03-01 / 03-02 | 회원탈퇴 1·2단계 | mypage.md §3 |
| MY-04-01 / 04-02 | 공지사항 목록·상세 | mypage.md §4 · admin.md §4 |
| MY-05-01 | FAQ | mypage.md §5 · admin.md §5 |
| MY-06-01 ~ 06-03 | 1:1 문의 목록·상세·작성 | mypage.md §6 · admin.md §7 |
| MY-07-01 / 07-02 | 약관 목록·상세 | mypage.md §7 · admin.md §6 |
| COM-01-02 / 01-03 | 삭제된 스팟 | admin.md §8 (스팟 관리, B) |
| COM-05-01 | 통합검색 인기 지역 | admin.md §9 (검색 로그, 후순위) |
