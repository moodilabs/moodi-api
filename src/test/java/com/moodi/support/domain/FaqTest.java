package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.support.FaqFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaqTest {

    @Test
    @DisplayName("카테고리 이름이 비어 있으면 생성할 수 없다")
    void category_rejects_blank_name() {
        assertThatThrownBy(() -> FaqCategory.create(" ", 0, true))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("질문이 200자를 넘으면 생성할 수 없다")
    void faq_rejects_long_question() {
        assertThatThrownBy(() -> Faq.create(1L, "q".repeat(201), "answer", 0, true))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("같은 카테고리로 수정하면 순서를 유지한다")
    void update_within_same_category_keeps_order() {
        Faq faq = FaqFixture.faq(1L, "How do I share a route?", 3);

        faq.update(1L, "How can I share a route?", "Tap Share.", false, 99);

        assertThat(faq.getQuestion()).isEqualTo("How can I share a route?");
        assertThat(faq.getAnswer()).isEqualTo("Tap Share.");
        assertThat(faq.isVisible()).isFalse();
        assertThat(faq.getSortOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("다른 카테고리로 옮기면 지정한 순서로 들어간다")
    void update_to_other_category_takes_given_order() {
        Faq faq = FaqFixture.faq(1L, "How do I share a route?", 3);

        faq.update(2L, "How do I share a route?", "Tap Share.", true, 5);

        assertThat(faq.getCategoryId()).isEqualTo(2L);
        assertThat(faq.getSortOrder()).isEqualTo(5);
        assertThat(faq.belongsTo(2L)).isTrue();
    }
}
