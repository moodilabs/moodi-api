package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.FaqCategoryCommand;
import com.moodi.support.application.dto.FaqCommand;
import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import com.moodi.support.domain.FaqRepository;
import com.moodi.support.support.FaqFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqAdminServiceTest {

    @Mock
    private FaqCategoryRepository faqCategoryRepository;

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private FaqAdminService faqAdminService;

    @Test
    @DisplayName("카테고리는 마지막 순서로 등록된다")
    void create_category_appends_to_end() {
        when(faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(FaqFixture.categoryWithId(1L, "Account", 0)));
        when(faqCategoryRepository.save(any(FaqCategory.class)))
                .thenReturn(FaqFixture.categoryWithId(2L, "Routes", 1));

        Long id = faqAdminService.createCategory(new FaqCategoryCommand("Routes", true));

        ArgumentCaptor<FaqCategory> captor = ArgumentCaptor.forClass(FaqCategory.class);
        verify(faqCategoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(1);
        assertThat(id).isEqualTo(2L);
    }

    @Test
    @DisplayName("카테고리 순서 교체는 index를 sortOrder로 쓴다")
    void reorder_categories_assigns_index() {
        FaqCategory a = FaqFixture.categoryWithId(1L, "A", 0);
        FaqCategory b = FaqFixture.categoryWithId(2L, "B", 1);
        FaqCategory c = FaqFixture.categoryWithId(3L, "C", 2);
        when(faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(a, b, c));

        faqAdminService.reorderCategories(List.of(3L, 1L, 2L));

        assertThat(c.getSortOrder()).isZero();
        assertThat(a.getSortOrder()).isEqualTo(1);
        assertThat(b.getSortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("순서 교체 시 ID가 빠지거나 중복되면 거부한다")
    void reorder_categories_rejects_mismatched_ids() {
        when(faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                FaqFixture.categoryWithId(1L, "A", 0), FaqFixture.categoryWithId(2L, "B", 1)));

        assertThatThrownBy(() -> faqAdminService.reorderCategories(List.of(1L, 1L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(faqCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("항목이 남아 있는 카테고리는 삭제할 수 없다")
    void delete_category_with_items_throws() {
        when(faqCategoryRepository.findById(1L)).thenReturn(Optional.of(FaqFixture.categoryWithId(1L, "A", 0)));
        when(faqRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> faqAdminService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FAQ_CATEGORY_NOT_EMPTY);
        verify(faqCategoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("FAQ는 카테고리의 마지막 순서로 등록된다")
    void create_faq_appends_to_end_of_category() {
        when(faqCategoryRepository.existsById(1L)).thenReturn(true);
        when(faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(FaqFixture.faqWithId(10L, 1L, "q1", 0), FaqFixture.faqWithId(11L, 1L, "q2", 1)));
        when(faqRepository.save(any(Faq.class))).thenReturn(FaqFixture.faqWithId(12L, 1L, "q3", 2));

        Long id = faqAdminService.createFaq(new FaqCommand(1L, "q3", "a3", true));

        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);
        verify(faqRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(2);
        assertThat(id).isEqualTo(12L);
    }

    @Test
    @DisplayName("없는 카테고리로는 FAQ를 등록할 수 없다")
    void create_faq_with_unknown_category_throws() {
        when(faqCategoryRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> faqAdminService.createFaq(new FaqCommand(9L, "q", "a", true)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FAQ_CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("FAQ를 다른 카테고리로 옮기면 그 카테고리의 마지막 순서가 된다")
    void update_faq_moving_category_appends_to_target() {
        Faq faq = FaqFixture.faqWithId(10L, 1L, "q", 0);
        when(faqRepository.findById(10L)).thenReturn(Optional.of(faq));
        when(faqCategoryRepository.existsById(2L)).thenReturn(true);
        when(faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(2L))
                .thenReturn(List.of(FaqFixture.faqWithId(20L, 2L, "other", 0)));

        faqAdminService.updateFaq(10L, new FaqCommand(2L, "q", "a", true));

        assertThat(faq.getCategoryId()).isEqualTo(2L);
        assertThat(faq.getSortOrder()).isEqualTo(1);
        verify(faqRepository).save(faq);
    }

    @Test
    @DisplayName("카테고리 안 항목 순서를 교체한다")
    void reorder_faqs_assigns_index() {
        Faq first = FaqFixture.faqWithId(10L, 1L, "q1", 0);
        Faq second = FaqFixture.faqWithId(11L, 1L, "q2", 1);
        when(faqCategoryRepository.existsById(1L)).thenReturn(true);
        when(faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(first, second));

        faqAdminService.reorderFaqs(1L, List.of(11L, 10L));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 FAQ를 삭제하면 실패한다")
    void delete_unknown_faq_throws() {
        when(faqRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> faqAdminService.deleteFaq(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FAQ_NOT_FOUND);
    }
}
