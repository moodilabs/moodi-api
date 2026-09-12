package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.AdminInquiryDetail;
import com.moodi.support.application.dto.AdminInquirySummary;
import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.application.dto.InquiryCursor;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryRepository;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 어드민의 문의 관리. 목록·상세·답변·미답변 수. */
@Service
@Transactional(readOnly = true)
public class InquiryAdminService {

    private final InquiryRepository inquiryRepository;
    private final InquiryQueryRepository inquiryQueryRepository;
    private final InquiryAttachmentStorage attachmentStorage;
    private final MemberSummaryReader memberSummaryReader;
    private final Clock clock;

    public InquiryAdminService(InquiryRepository inquiryRepository, InquiryQueryRepository inquiryQueryRepository,
                               InquiryAttachmentStorage attachmentStorage, MemberSummaryReader memberSummaryReader,
                               Clock clock) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryQueryRepository = inquiryQueryRepository;
        this.attachmentStorage = attachmentStorage;
        this.memberSummaryReader = memberSummaryReader;
        this.clock = clock;
    }

    public CursorResponse<AdminInquirySummary> getInquiries(InquiryStatus status, InquiryTopic topic, String cursor,
                                                            int size) {
        InquiryCursor parsed = InquiryCursor.parse(cursor);
        List<Inquiry> rows = inquiryQueryRepository.findAll(status, topic,
                parsed == null ? null : parsed.createdAt(),
                parsed == null ? null : parsed.id(),
                size + 1);
        boolean hasNext = rows.size() > size;
        List<Inquiry> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        Map<UUID, AdminInquirySummary.Member> members = readMembers(
                page.stream().map(Inquiry::getMemberId).collect(Collectors.toSet()));
        List<AdminInquirySummary> items = page.stream()
                .map(inquiry -> new AdminInquirySummary(inquiry.getId(), inquiry.getTopic(), inquiry.getSubject(),
                        inquiry.getStatus(), inquiry.getCreatedAt(), inquiry.getAnsweredAt(),
                        members.get(inquiry.getMemberId())))
                .toList();
        return CursorResponse.of(items, hasNext ? InquiryCursor.of(page.getLast()) : null, hasNext);
    }

    public AdminInquiryDetail get(UUID inquiryId) {
        Inquiry inquiry = findInquiry(inquiryId);
        AdminInquirySummary.Member member = readMembers(Set.of(inquiry.getMemberId())).get(inquiry.getMemberId());
        List<InquiryAttachmentView> attachments = inquiry.getAttachments().stream()
                .map(attachment -> new InquiryAttachmentView(
                        attachmentStorage.issueReadUrl(attachment.getObjectKey()), attachment.getContentType()))
                .toList();
        AdminInquiryDetail.Answer answer = inquiry.isAnswered()
                ? new AdminInquiryDetail.Answer(inquiry.getAnswerContent(), inquiry.getAnsweredBy(),
                inquiry.getAnsweredAt())
                : null;
        return new AdminInquiryDetail(inquiry.getId(), inquiry.getTopic(), inquiry.getSubject(),
                inquiry.getContent(), inquiry.getStatus(), inquiry.getCreatedAt(), attachments, member, answer);
    }

    /** 재호출은 답변을 덮어쓴다(이력 없음). */
    @Transactional
    public void answer(UUID inquiryId, UUID adminId, String content) {
        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.answer(content, adminId, LocalDateTime.now(clock));
        inquiryRepository.save(inquiry);
    }

    public long countReceived() {
        return inquiryRepository.countByStatus(InquiryStatus.RECEIVED);
    }

    private Inquiry findInquiry(UUID inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private Map<UUID, AdminInquirySummary.Member> readMembers(Set<UUID> memberIds) {
        return memberSummaryReader.readByIds(memberIds).values().stream()
                .collect(Collectors.toMap(MemberSummaryReader.MemberSummary::id,
                        summary -> new AdminInquirySummary.Member(summary.id(), summary.nickname(),
                                summary.email(), summary.withdrawn())));
    }
}
