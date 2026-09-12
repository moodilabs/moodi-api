package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FAQ 질문 유형(`MY-05-01`의 Account / Travel times / Routes …). 어드민이 순서와 노출을 정한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqCategory extends BaseEntity {

    public static final int MAX_NAME_LENGTH = 50;

    private Long id;
    private String name;
    private int sortOrder;
    private boolean visible;

    private FaqCategory(String name, int sortOrder, boolean visible) {
        validateName(name);
        this.name = name;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public static FaqCategory create(String name, int sortOrder, boolean visible) {
        return new FaqCategory(name, sortOrder, visible);
    }

    public void update(String name, boolean visible) {
        validateName(name);
        this.name = name;
        this.visible = visible;
    }

    public void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
