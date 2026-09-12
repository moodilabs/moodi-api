package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.FaqCategoryCommand;
import com.moodi.support.application.dto.FaqCategoryView;
import com.moodi.support.application.dto.FaqCommand;
import com.moodi.support.application.dto.FaqItem;
import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import com.moodi.support.domain.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 어드민의 FAQ 관리. 숨김 항목도 다룬다. 컨트롤러는 관리자 인증(`ADM-F01`)과 함께 붙는다.
 */
@Service
@Transactional
public class FaqAdminService {

    private final FaqCategoryRepository faqCategoryRepository;
    private final FaqRepository faqRepository;

    public FaqAdminService(FaqCategoryRepository faqCategoryRepository, FaqRepository faqRepository) {
        this.faqCategoryRepository = faqCategoryRepository;
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public List<FaqCategoryView> getAll() {
        Map<Long, List<Faq>> faqsByCategory = faqRepository.findAllByOrderByCategoryIdAscSortOrderAscIdAsc()
                .stream()
                .collect(Collectors.groupingBy(Faq::getCategoryId));

        return faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(category -> FaqCategoryView.of(category,
                        faqsByCategory.getOrDefault(category.getId(), List.of()).stream()
                                .map(FaqItem::from).toList()))
                .toList();
    }

    public Long createCategory(FaqCategoryCommand command) {
        int nextOrder = faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc().size();
        FaqCategory category = FaqCategory.create(command.name(), nextOrder, command.visible());
        return faqCategoryRepository.save(category).getId();
    }

    public void updateCategory(Long categoryId, FaqCategoryCommand command) {
        FaqCategory category = findCategory(categoryId);
        category.update(command.name(), command.visible());
        faqCategoryRepository.save(category);
    }

    /**
     * 전체 카테고리 ID를 원하는 순서로 받아 `sortOrder = index`로 갱신한다. 누락·중복·모르는 ID는 거부한다.
     */
    public void reorderCategories(List<Long> orderedIds) {
        List<FaqCategory> categories = faqCategoryRepository.findAllByOrderBySortOrderAscIdAsc();
        validateSameIds(orderedIds, categories.stream().map(FaqCategory::getId).toList());

        Map<Long, FaqCategory> byId = categories.stream()
                .collect(Collectors.toMap(FaqCategory::getId, category -> category));
        for (int index = 0; index < orderedIds.size(); index++) {
            FaqCategory category = byId.get(orderedIds.get(index));
            category.reorder(index);
            faqCategoryRepository.save(category);
        }
    }

    public void deleteCategory(Long categoryId) {
        FaqCategory category = findCategory(categoryId);
        if (faqRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException(ErrorCode.FAQ_CATEGORY_NOT_EMPTY);
        }
        faqCategoryRepository.delete(category);
    }

    public Long createFaq(FaqCommand command) {
        validateCategoryExists(command.categoryId());
        int nextOrder = faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(command.categoryId()).size();
        Faq faq = Faq.create(command.categoryId(), command.question(), command.answer(), nextOrder,
                command.visible());
        return faqRepository.save(faq).getId();
    }

    public void updateFaq(Long faqId, FaqCommand command) {
        Faq faq = findFaq(faqId);
        validateCategoryExists(command.categoryId());
        int orderIfMoved = faq.belongsTo(command.categoryId())
                ? faq.getSortOrder()
                : faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(command.categoryId()).size();
        faq.update(command.categoryId(), command.question(), command.answer(), command.visible(), orderIfMoved);
        faqRepository.save(faq);
    }

    /**
     * 한 카테고리 안의 항목 ID 전체를 원하는 순서로 받아 갱신한다.
     */
    public void reorderFaqs(Long categoryId, List<Long> orderedIds) {
        validateCategoryExists(categoryId);
        List<Faq> faqs = faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(categoryId);
        validateSameIds(orderedIds, faqs.stream().map(Faq::getId).toList());

        Map<Long, Faq> byId = faqs.stream().collect(Collectors.toMap(Faq::getId, faq -> faq));
        for (int index = 0; index < orderedIds.size(); index++) {
            Faq faq = byId.get(orderedIds.get(index));
            faq.reorder(index);
            faqRepository.save(faq);
        }
    }

    public void deleteFaq(Long faqId) {
        faqRepository.delete(findFaq(faqId));
    }

    private FaqCategory findCategory(Long categoryId) {
        return faqCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_CATEGORY_NOT_FOUND));
    }

    private Faq findFaq(Long faqId) {
        return faqRepository.findById(faqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
    }

    private void validateCategoryExists(Long categoryId) {
        if (categoryId == null || !faqCategoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.FAQ_CATEGORY_NOT_FOUND);
        }
    }

    private void validateSameIds(List<Long> orderedIds, List<Long> existingIds) {
        if (orderedIds == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> ordered = new HashSet<>(orderedIds);
        if (ordered.size() != orderedIds.size() || !ordered.equals(new HashSet<>(existingIds))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
