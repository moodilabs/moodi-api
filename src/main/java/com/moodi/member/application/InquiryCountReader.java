package com.moodi.member.application;

import java.util.UUID;

/** 고객지원 컨텍스트의 문의 수. 어드민 회원 상세에 쓴다. */
public interface InquiryCountReader {

    long countByMemberId(UUID memberId);
}
