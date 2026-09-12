package com.moodi.support.support;

import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqCategory;
import org.springframework.test.util.ReflectionTestUtils;

public class FaqFixture {

    public static FaqCategory category(String name, int sortOrder) {
        return FaqCategory.create(name, sortOrder, true);
    }

    public static FaqCategory hiddenCategory(String name, int sortOrder) {
        return FaqCategory.create(name, sortOrder, false);
    }

    public static FaqCategory categoryWithId(Long id, String name, int sortOrder) {
        FaqCategory category = category(name, sortOrder);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static Faq faq(Long categoryId, String question, int sortOrder) {
        return Faq.create(categoryId, question, "Answer for: " + question, sortOrder, true);
    }

    public static Faq hiddenFaq(Long categoryId, String question, int sortOrder) {
        return Faq.create(categoryId, question, "Answer for: " + question, sortOrder, false);
    }

    public static Faq faqWithId(Long id, Long categoryId, String question, int sortOrder) {
        Faq faq = faq(categoryId, question, sortOrder);
        ReflectionTestUtils.setField(faq, "id", id);
        return faq;
    }

    private FaqFixture() {
    }
}
