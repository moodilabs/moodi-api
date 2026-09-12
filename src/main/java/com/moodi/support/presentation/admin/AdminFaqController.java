package com.moodi.support.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.FaqAdminService;
import com.moodi.support.presentation.dto.admin.AdminFaqCategoryRequest;
import com.moodi.support.presentation.dto.admin.AdminFaqCategoryResponse;
import com.moodi.support.presentation.dto.admin.AdminFaqRequest;
import com.moodi.support.presentation.dto.admin.IdResponse;
import com.moodi.support.presentation.dto.admin.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminRequired
@RestController
@RequestMapping("/api/admin")
public class AdminFaqController {

    private final FaqAdminService faqAdminService;

    public AdminFaqController(FaqAdminService faqAdminService) {
        this.faqAdminService = faqAdminService;
    }

    @GetMapping("/faqs")
    public SuccessResponse<List<AdminFaqCategoryResponse>> getAll() {
        return SuccessResponse.of(faqAdminService.getAll().stream().map(AdminFaqCategoryResponse::from).toList());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/faq-categories")
    public SuccessResponse<IdResponse> createCategory(@Valid @RequestBody AdminFaqCategoryRequest request) {
        return SuccessResponse.of(new IdResponse(faqAdminService.createCategory(request.toCommand())));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/faq-categories/order")
    public void reorderCategories(@Valid @RequestBody OrderRequest request) {
        faqAdminService.reorderCategories(request.ids());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/faq-categories/{categoryId}")
    public void updateCategory(@PathVariable Long categoryId, @Valid @RequestBody AdminFaqCategoryRequest request) {
        faqAdminService.updateCategory(categoryId, request.toCommand());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/faq-categories/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId) {
        faqAdminService.deleteCategory(categoryId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/faq-categories/{categoryId}/faqs/order")
    public void reorderFaqs(@PathVariable Long categoryId, @Valid @RequestBody OrderRequest request) {
        faqAdminService.reorderFaqs(categoryId, request.ids());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/faqs")
    public SuccessResponse<IdResponse> createFaq(@Valid @RequestBody AdminFaqRequest request) {
        return SuccessResponse.of(new IdResponse(faqAdminService.createFaq(request.toCommand())));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/faqs/{faqId}")
    public void updateFaq(@PathVariable Long faqId, @Valid @RequestBody AdminFaqRequest request) {
        faqAdminService.updateFaq(faqId, request.toCommand());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/faqs/{faqId}")
    public void deleteFaq(@PathVariable Long faqId) {
        faqAdminService.deleteFaq(faqId);
    }
}
