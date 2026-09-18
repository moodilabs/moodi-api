package com.moodi.support.infrastructure.html;

import com.moodi.support.application.HtmlSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * jsoup 화이트리스트 정제. 어드민 에디터(TipTap)가 만드는 서식 — 굵게·기울임·밑줄·취소선·글자색·제목·목록·줄바꿈·링크 — 만 남긴다.
 * {@code style}은 글자색(<code>color</code>)만 허용하고 나머지 인라인 스타일은 제거한다.
 */
@Component
public class JsoupHtmlSanitizer implements HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "b", "strong", "i", "em", "u", "s", "strike",
                    "h1", "h2", "h3", "ul", "ol", "li", "span", "a")
            .addAttributes("span", "style")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    private static final java.util.regex.Pattern COLOR_STYLE =
            java.util.regex.Pattern.compile("^\\s*color\\s*:\\s*(#[0-9a-fA-F]{3,8}|rgb\\([0-9,\\s]+\\)|[a-zA-Z]+)\\s*;?\\s*$");

    @Override
    public String sanitize(String html) {
        if (html == null) {
            return null;
        }
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        Document dirty = Jsoup.parseBodyFragment(html);
        dirty.outputSettings(settings);
        dirty.select("span[style]").forEach(span -> {
            if (!COLOR_STYLE.matcher(span.attr("style")).matches()) {
                span.removeAttr("style");
            }
        });
        return Jsoup.clean(dirty.body().html(), "", SAFELIST, settings);
    }
}
