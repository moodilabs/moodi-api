package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 1:1 문의(`MY-06`). 회원이 등록하고 어드민이 한 번 답변한다. 답변은 덮어쓸 수 있지만 상태는 되돌리지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    public static final int MAX_SUBJECT_LENGTH = 60;
    public static final int MAX_CONTENT_LENGTH = 1000;
    public static final int MAX_ANSWER_LENGTH = 5000;
    public static final int MAX_ATTACHMENTS = 5;

    private UUID id;
    private UUID memberId;
    private InquiryTopic topic;
    private String subject;
    private String content;
    private InquiryStatus status;
    private String answerContent;
    private UUID answeredBy;
    private LocalDateTime answeredAt;
    private List<InquiryAttachment> attachments = new ArrayList<>();

    private Inquiry(UUID memberId, InquiryTopic topic, String subject, String content,
                    List<InquiryAttachment> attachments) {
        validate(memberId, topic, subject, content, attachments);
        this.memberId = memberId;
        this.topic = topic;
        this.subject = subject;
        this.content = content;
        this.status = InquiryStatus.RECEIVED;
        this.attachments = new ArrayList<>(attachments);
    }

    public static Inquiry create(UUID memberId, InquiryTopic topic, String subject, String content,
                                 List<InquiryAttachment> attachments) {
        return new Inquiry(memberId, topic, subject, content, attachments);
    }

    public void answer(String answerContent, UUID adminId, LocalDateTime now) {
        if (answerContent == null || answerContent.isBlank() || answerContent.length() > MAX_ANSWER_LENGTH
                || adminId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.answerContent = answerContent;
        this.answeredBy = adminId;
        this.answeredAt = now;
        this.status = InquiryStatus.ANSWERED;
    }

    public boolean isOwnedBy(UUID memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isAnswered() {
        return status == InquiryStatus.ANSWERED;
    }

    public List<InquiryAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    private static void validate(UUID memberId, InquiryTopic topic, String subject, String content,
                                 List<InquiryAttachment> attachments) {
        if (memberId == null || topic == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (subject == null || subject.isBlank() || subject.length() > MAX_SUBJECT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (attachments == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(ErrorCode.INQUIRY_TOO_MANY_ATTACHMENTS);
        }
    }
}
