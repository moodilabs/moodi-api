package com.moodi.support.infrastructure.html;

import com.moodi.support.application.HtmlSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * jsoup 화이트리스트 정제. 어드민 에디터(TipTap)가 만드는 서식 — 굵게·기울임·밑줄·취소선·글자색·형광펜·제목·목록·표·줄바꿈·링크 — 만 남긴다.
 * 표는 셀 병합(colspan·rowspan)만 남기고, TipTap이 table·col에 붙이는 너비 style은 앱 레이아웃과 어긋나므로 버린다.
 * {@code style}은 span의 글자색(<code>color</code>)과 mark의 배경색(<code>background-color</code>)만 허용하고 나머지 인라인 스타일은 제거한다.
 */
@Component
public class JsoupHtmlSanitizer implements HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "b", "strong", "i", "em", "u", "s", "strike",
                    "h1", "h2", "h3", "ul", "ol", "li", "span", "mark", "a",
                    "table", "colgroup", "col", "thead", "tbody", "tr", "th", "td")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("span", "style")
            .addAttributes("mark", "style")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    private static final String COLOR_VALUE = "(#[0-9a-fA-F]{3,8}|rgb\\([0-9,\\s]+\\)|[a-zA-Z]+)";
    private static final java.util.regex.Pattern COLOR_STYLE =
            java.util.regex.Pattern.compile("^\\s*color\\s*:\\s*" + COLOR_VALUE + "\\s*;?\\s*$");
    private static final java.util.regex.Pattern BACKGROUND_STYLE =
            java.util.regex.Pattern.compile("^\\s*background-color\\s*:\\s*" + COLOR_VALUE + "\\s*;?\\s*$");

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
        dirty.select("mark[style]").forEach(mark -> {
            if (!BACKGROUND_STYLE.matcher(mark.attr("style")).matches()) {
                mark.removeAttr("style");
            }
        });
        return Jsoup.clean(dirty.body().html(), "", SAFELIST, settings);
    }
}
