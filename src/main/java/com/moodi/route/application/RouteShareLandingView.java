package com.moodi.route.application;

/**
 * 루트 공유 랜딩 페이지({@code GET /routes/shared/{publicId}})의 OG 메타태그·딥링크에 쓰는 값.
 *
 * <p>존재하지 않거나·비공개·삭제된 루트는 앱이 자체적으로 [RTE-04-03] 화면을 보여주므로
 * 랜딩 페이지는 여기서 에러를 내지 않고 기본 제목으로 조용히 폴백한다 — 카카오톡 크롤러가
 * 이 URL을 스크랩할 때도 항상 200 + 미리보기 카드가 나와야 한다.
 */
public record RouteShareLandingView(String title, boolean found) {

    private static final String DEFAULT_TITLE = "Moodi";

    public static RouteShareLandingView of(String title) {
        return new RouteShareLandingView(title, true);
    }

    public static RouteShareLandingView notFound() {
        return new RouteShareLandingView(DEFAULT_TITLE, false);
    }
}
