package com.moodi.support.application;

/**
 * 어드민 리치 에디터가 보낸 HTML에서 허용 태그·속성만 남기는 포트. 약관 전문처럼 앱이 HTML로 렌더하는 본문에 쓴다.
 * 스크립트·이벤트 핸들러·외부 리소스는 여기서 걸러져 앱 WebView/렌더러에 닿지 않는다.
 */
public interface HtmlSanitizer {

    String sanitize(String html);
}
