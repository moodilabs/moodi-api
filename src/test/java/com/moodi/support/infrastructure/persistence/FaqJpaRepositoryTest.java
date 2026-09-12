package com.moodi.support.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import com.moodi.support.domain.FaqRepository;
import com.moodi.support.support.FaqFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FaqJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private FaqCategoryRepository faqCategoryRepository;

    @Autowired
    private FaqRepository faqRepository;

    @Test
    @DisplayName("카테고리는 sortOrder 오름차순으로 조회된다")
    void categories_ordered_by_sort_order() {
        faqCategoryRepository.save(FaqFixture.category("Routes", 2));
        faqCategoryRepository.save(FaqFixture.category("Account", 0));
        faqCategoryRepository.save(FaqFixture.category("Travel times", 1));

        List<FaqCategory> result = faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc();

        assertThat(result).extracting(FaqCategory::getName).containsExactly("Account", "Travel times", "Routes");
    }

    @Test
    @DisplayName("항목은 카테고리·순서 오름차순으로 조회되고 카테고리별 조회도 된다")
    void faqs_ordered_by_category_then_sort_order() {
        FaqCategory account = faqCategoryRepository.save(FaqFixture.category("Account", 0));
        FaqCategory routes = faqCategoryRepository.save(FaqFixture.category("Routes", 1));
        faqRepository.save(FaqFixture.faq(routes.getId(), "r1", 0));
        faqRepository.save(FaqFixture.faq(account.getId(), "a2", 1));
        faqRepository.save(FaqFixture.faq(account.getId(), "a1", 0));

        List<Faq> all = faqRepository.findAllByOrderByCategoryIdAscSortOrderAscIdAsc();
        List<Faq> accountOnly = faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(account.getId());

        assertThat(all).extracting(Faq::getQuestion).containsExactly("a1", "a2", "r1");
        assertThat(accountOnly).extracting(Faq::getQuestion).containsExactly("a1", "a2");
        assertThat(faqRepository.existsByCategoryId(account.getId())).isTrue();
        assertThat(faqRepository.existsByCategoryId(999L)).isFalse();
    }
}
