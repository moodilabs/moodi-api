package com.moodi.support.application;

import com.moodi.support.application.dto.FaqCategoryView;
import com.moodi.support.application.dto.FaqItem;
import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import com.moodi.support.domain.FaqRepository;
import com.moodi.support.support.FaqFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqQueryServiceTest {

    @Mock
    private FaqCategoryRepository faqCategoryRepository;

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private FaqQueryService faqQueryService;

    @Test
    @DisplayName("노출 카테고리의 노출 항목만 카테고리 순서대로 묶어 돌려준다")
    void get_visible_faqs_groups_visible_items_by_visible_category() {
        FaqCategory account = FaqFixture.categoryWithId(1L, "Account", 0);
        FaqCategory travel = FaqFixture.categoryWithId(2L, "Travel times", 1);
        FaqCategory hidden = hidden(3L, "Hidden", 2);
        when(faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(account, travel, hidden));
        when(faqRepository.findAllByOrderByCategoryIdAscSortOrderAscIdAsc()).thenReturn(List.of(
                FaqFixture.faqWithId(10L, 1L, "How do I change my username?", 0),
                FaqFixture.hiddenFaq(1L, "hidden item", 1),
                FaqFixture.faqWithId(20L, 2L, "How are travel times calculated?", 0),
                FaqFixture.faqWithId(30L, 3L, "in hidden category", 0)));

        List<FaqCategoryView> result = faqQueryService.getVisibleFaqs();

        assertThat(result).extracting(FaqCategoryView::name).containsExactly("Account", "Travel times");
        assertThat(result.getFirst().items()).extracting(FaqItem::id).containsExactly(10L);
        assertThat(result.get(1).items()).extracting(FaqItem::question)
                .containsExactly("How are travel times calculated?");
    }

    @Test
    @DisplayName("노출 항목이 하나도 없는 카테고리는 목록에서 빠진다")
    void get_visible_faqs_drops_empty_category() {
        FaqCategory empty = FaqFixture.categoryWithId(1L, "Empty", 0);
        when(faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(empty));
        when(faqRepository.findAllByOrderByCategoryIdAscSortOrderAscIdAsc())
                .thenReturn(List.of(FaqFixture.hiddenFaq(1L, "hidden", 0)));

        assertThat(faqQueryService.getVisibleFaqs()).isEmpty();
    }

    private FaqCategory hidden(Long id, String name, int order) {
        FaqCategory category = FaqFixture.hiddenCategory(name, order);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
