package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 10_000;
    public static final int PREVIEW_LENGTH = 200;

    private Long id;
    private NoticeType type;
    private String title;
    private String content;
    private boolean visible;
    private LocalDate publishedAt;

    private Notice(NoticeType type, String title, String content, boolean visible, LocalDate publishedAt) {
        validate(type, title, content, publishedAt);
        this.type = type;
        this.title = title;
        this.content = content;
        this.visible = visible;
        this.publishedAt = publishedAt;
    }

    public static Notice create(NoticeType type, String title, String content, boolean visible,
                                LocalDate publishedAt) {
        return new Notice(type, title, content, visible, publishedAt);
    }

    public void update(NoticeType type, String title, String content, boolean visible, LocalDate publishedAt) {
        validate(type, title, content, publishedAt);
        this.type = type;
        this.title = title;
        this.content = content;
        this.visible = visible;
        this.publishedAt = publishedAt;
    }

    public void changeVisibility(boolean visible) {
        this.visible = visible;
    }

    /**
     * 목록의 2줄 미리보기용. 줄바꿈을 공백으로 접고 앞 {@value #PREVIEW_LENGTH}자만 남긴다.
     */
    public String preview() {
        String flattened = content.replaceAll("\\s+", " ").trim();
        return flattened.length() <= PREVIEW_LENGTH ? flattened : flattened.substring(0, PREVIEW_LENGTH);
    }

    private static void validate(NoticeType type, String title, String content, LocalDate publishedAt) {
        if (type == null || publishedAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
