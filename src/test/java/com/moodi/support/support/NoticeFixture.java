package com.moodi.support.support;

import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public class NoticeFixture {

    public static final LocalDate DEFAULT_PUBLISHED_AT = LocalDate.of(2026, 8, 10);

    private static final NoticeType DEFAULT_TYPE = NoticeType.ANNOUNCEMENT;
    private static final String DEFAULT_TITLE = "Route sharing is now available";
    private static final String DEFAULT_CONTENT = "You can now share your routes with friends.\nOpen a route and tap Share.";

    public static Notice create() {
        return create(DEFAULT_TYPE, DEFAULT_TITLE, DEFAULT_CONTENT, true, DEFAULT_PUBLISHED_AT);
    }

    public static Notice create(NoticeType type, String title, String content, boolean visible,
                                LocalDate publishedAt) {
        return Notice.create(type, title, content, visible, publishedAt);
    }

    public static Notice visible(LocalDate publishedAt) {
        return create(DEFAULT_TYPE, DEFAULT_TITLE, DEFAULT_CONTENT, true, publishedAt);
    }

    public static Notice hidden() {
        return create(DEFAULT_TYPE, DEFAULT_TITLE, DEFAULT_CONTENT, false, DEFAULT_PUBLISHED_AT);
    }

    public static Notice createWithId(Long id) {
        Notice notice = create();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    public static Notice createWithId(Long id, LocalDate publishedAt) {
        Notice notice = visible(publishedAt);
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    private NoticeFixture() {
    }
}
