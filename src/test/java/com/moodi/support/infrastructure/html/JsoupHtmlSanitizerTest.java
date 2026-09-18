package com.moodi.support.infrastructure.html;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupHtmlSanitizerTest {

    private final JsoupHtmlSanitizer sanitizer = new JsoupHtmlSanitizer();

    @Test
    @DisplayName("굵게·글자색·줄바꿈·목록 같은 에디터 서식은 남긴다")
    void keeps_editor_formatting() {
        String html = "<p><strong>제1조</strong> 목적<br><span style=\"color: #ff0000\">중요</span></p><ul><li>항목</li></ul>";

        String result = sanitizer.sanitize(html);

        assertThat(result).contains("<strong>제1조</strong>", "<br>", "<span style=\"color: #ff0000\">중요</span>", "<li>항목</li>");
    }

    @Test
    @DisplayName("스크립트·이벤트 핸들러·글자색 외 인라인 스타일은 제거한다")
    void strips_dangerous_markup() {
        String html = "<p onclick=\"alert(1)\">본문</p><script>alert(1)</script>"
                + "<span style=\"position:fixed;color:red\">x</span><img src=\"http://evil/x.png\">";

        String result = sanitizer.sanitize(html);

        assertThat(result).doesNotContain("script", "onclick", "position", "img");
        assertThat(result).contains("<p>본문</p>", "<span>x</span>");
    }

    @Test
    @DisplayName("순수 텍스트는 개행을 유지한 채 그대로 통과한다")
    void plain_text_passes_through() {
        assertThat(sanitizer.sanitize("제1조\n제2조")).isEqualTo("제1조\n제2조");
    }

    @Test
    @DisplayName("링크는 http·https·mailto만 허용한다")
    void allows_only_safe_link_protocols() {
        String result = sanitizer.sanitize("<a href=\"javascript:alert(1)\">x</a><a href=\"https://moodi.kr\">y</a>");

        assertThat(result).doesNotContain("javascript");
        assertThat(result).contains("href=\"https://moodi.kr\"");
    }
}
