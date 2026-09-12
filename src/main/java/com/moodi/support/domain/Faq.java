package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseEntity {

    public static final int MAX_QUESTION_LENGTH = 200;
    public static final int MAX_ANSWER_LENGTH = 10_000;

    private Long id;
    private Long categoryId;
    private String question;
    private String answer;
    private int sortOrder;
    private boolean visible;

    private Faq(Long categoryId, String question, String answer, int sortOrder, boolean visible) {
        validate(categoryId, question, answer);
        this.categoryId = categoryId;
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public static Faq create(Long categoryId, String question, String answer, int sortOrder, boolean visible) {
        return new Faq(categoryId, question, answer, sortOrder, visible);
    }

    /**
     * 카테고리를 옮기면 새 카테고리의 마지막 순서로 들어간다. 같은 카테고리면 순서를 유지한다.
     */
    public void update(Long categoryId, String question, String answer, boolean visible, int sortOrderIfMoved) {
        validate(categoryId, question, answer);
        if (!this.categoryId.equals(categoryId)) {
            this.categoryId = categoryId;
            this.sortOrder = sortOrderIfMoved;
        }
        this.question = question;
        this.answer = answer;
        this.visible = visible;
    }

    public void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean belongsTo(Long categoryId) {
        return this.categoryId.equals(categoryId);
    }

    private static void validate(Long categoryId, String question, String answer) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (question == null || question.isBlank() || question.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (answer == null || answer.isBlank() || answer.length() > MAX_ANSWER_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
