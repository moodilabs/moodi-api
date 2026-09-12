package com.moodi.support.application;

import com.moodi.support.application.dto.FaqCategoryView;
import com.moodi.support.application.dto.FaqItem;
import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import com.moodi.support.domain.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 앱의 FAQ 조회(`MY-05-01`). 노출 카테고리 안의 노출 항목만, 어드민이 정한 순서로. 항목이 없는 카테고리는 뺀다.
 */
@Service
@Transactional(readOnly = true)
public class FaqQueryService {

    private final FaqCategoryRepository faqCategoryRepository;
    private final FaqRepository faqRepository;

    public FaqQueryService(FaqCategoryRepository faqCategoryRepository, FaqRepository faqRepository) {
        this.faqCategoryRepository = faqCategoryRepository;
        this.faqRepository = faqRepository;
    }

    public List<FaqCategoryView> getVisibleFaqs() {
        Map<Long, List<Faq>> faqsByCategory = faqRepository.findAllByOrderByCategoryIdAscSortOrderAscIdAsc()
                .stream()
                .filter(Faq::isVisible)
                .collect(Collectors.groupingBy(Faq::getCategoryId));

        return faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(FaqCategory::isVisible)
                .filter(category -> faqsByCategory.containsKey(category.getId()))
                .map(category -> FaqCategoryView.of(category,
                        faqsByCategory.get(category.getId()).stream().map(FaqItem::from).toList()))
                .toList();
    }
}
