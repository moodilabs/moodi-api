package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.application.dto.InquiryCreateCommand;
import com.moodi.support.application.dto.InquiryCursor;
import com.moodi.support.application.dto.InquiryDetail;
import com.moodi.support.application.dto.InquirySummary;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryAttachment;
import com.moodi.support.domain.InquiryAttachmentFile;
import com.moodi.support.domain.InquiryAttachmentType;
import com.moodi.support.domain.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 회원의 1:1 문의(`MY-06`). 등록·내 목록·상세·첨부 업로드 URL. */
@Service
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryQueryRepository inquiryQueryRepository;
    private final InquiryAttachmentStorage attachmentStorage;

    public InquiryService(InquiryRepository inquiryRepository, InquiryQueryRepository inquiryQueryRepository,
                          InquiryAttachmentStorage attachmentStorage) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryQueryRepository = inquiryQueryRepository;
        this.attachmentStorage = attachmentStorage;
    }

    public InquiryAttachmentStorage.UploadTarget issueUploadUrl(UUID memberId, String contentType,
                                                                long contentLength) {
        InquiryAttachmentFile file = InquiryAttachmentFile.of(contentType, contentLength);
        return attachmentStorage.issueUploadUrl(file.objectName(memberId), file.getType().getContentType(),
                file.getContentLength());
    }

    /**
     * 첨부 키는 업로드 URL 발급 때 서버가 정한 본인 경로(`inquiries/{memberId}/…`)여야 한다.
     * 다른 회원의 객체나 임의 경로를 붙이는 걸 막는다. 실제 업로드 여부는 확인하지 않는다 — 없는 객체는 읽기 URL이 404다.
     */
    @Transactional
    public UUID create(UUID memberId, InquiryCreateCommand command) {
        List<InquiryCreateCommand.Attachment> requested = command.attachments() == null
                ? List.of() : command.attachments();
        if (requested.size() > Inquiry.MAX_ATTACHMENTS) {
            throw new BusinessException(ErrorCode.INQUIRY_TOO_MANY_ATTACHMENTS);
        }
        String ownPrefix = InquiryAttachmentFile.keyPrefix(memberId);
        List<InquiryAttachment> attachments = new java.util.ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            InquiryCreateCommand.Attachment attachment = requested.get(i);
            if (attachment.attachmentKey() == null || !attachment.attachmentKey().startsWith(ownPrefix)) {
                throw new BusinessException(ErrorCode.INQUIRY_INVALID_ATTACHMENT_KEY);
            }
            InquiryAttachmentType type = InquiryAttachmentType.from(attachment.contentType());
            attachments.add(InquiryAttachment.of(attachment.attachmentKey(), type.getContentType(), i));
        }
        Inquiry inquiry = Inquiry.create(memberId, command.topic(), command.subject(), command.content(),
                attachments);
        return inquiryRepository.save(inquiry).getId();
    }

    public CursorResponse<InquirySummary> getMine(UUID memberId, String cursor, int size) {
        InquiryCursor parsed = InquiryCursor.parse(cursor);
        List<Inquiry> rows = inquiryQueryRepository.findByMember(memberId,
                parsed == null ? null : parsed.createdAt(),
                parsed == null ? null : parsed.id(),
                size + 1);
        boolean hasNext = rows.size() > size;
        List<Inquiry> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        List<InquirySummary> items = page.stream().map(InquirySummary::from).toList();
        return CursorResponse.of(items, hasNext ? InquiryCursor.of(page.getLast()) : null, hasNext);
    }

    public InquiryDetail getMine(UUID memberId, UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
        if (!inquiry.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.INQUIRY_FORBIDDEN);
        }
        return new InquiryDetail(inquiry.getId(), inquiry.getTopic(), inquiry.getSubject(), inquiry.getContent(),
                inquiry.getStatus(), inquiry.getCreatedAt(), toViews(inquiry),
                inquiry.isAnswered() ? new InquiryDetail.Answer(inquiry.getAnswerContent(), inquiry.getAnsweredAt())
                        : null);
    }

    List<InquiryAttachmentView> toViews(Inquiry inquiry) {
        return inquiry.getAttachments().stream()
                .map(attachment -> new InquiryAttachmentView(attachmentStorage.issueReadUrl(attachment.getObjectKey()),
                        attachment.getContentType()))
                .toList();
    }
}
