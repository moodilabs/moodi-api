package com.moodi.support.application;

import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 문의 목록 조회 포트. 정렬은 `createdAt DESC, id DESC`, 커서는 마지막 항목의 (createdAt, id).
 * `limit`만큼 돌려주므로 `size + 1`을 넘겨 hasNext를 판단한다.
 */
public interface InquiryQueryRepository {

    List<Inquiry> findByMember(UUID memberId, LocalDateTime cursorCreatedAt, UUID cursorId, int limit);

    List<Inquiry> findAll(InquiryStatus status, InquiryTopic topic, LocalDateTime cursorCreatedAt, UUID cursorId,
                          int limit);
}
