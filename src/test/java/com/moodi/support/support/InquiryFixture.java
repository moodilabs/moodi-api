package com.moodi.support.support;

import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryAttachment;
import com.moodi.support.domain.InquiryTopic;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InquiryFixture {

    public static final String DEFAULT_SUBJECT = "Can I add my own spot to a route?";
    public static final String DEFAULT_CONTENT = "I want to add a small bakery near my hotel to my route.";

    public static Inquiry create(UUID memberId) {
        return Inquiry.create(memberId, InquiryTopic.ROUTES, DEFAULT_SUBJECT, DEFAULT_CONTENT, List.of());
    }

    public static Inquiry createWithAttachment(UUID memberId) {
        return Inquiry.create(memberId, InquiryTopic.ROUTES, DEFAULT_SUBJECT, DEFAULT_CONTENT,
                List.of(InquiryAttachment.of("inquiries/" + memberId + "/a.jpg", "image/jpeg", 0)));
    }

    public static Inquiry createWithId(UUID id, UUID memberId, LocalDateTime createdAt) {
        Inquiry inquiry = create(memberId);
        ReflectionTestUtils.setField(inquiry, "id", id);
        ReflectionTestUtils.setField(inquiry, "createdAt", createdAt);
        return inquiry;
    }

    private InquiryFixture() {
    }
}
