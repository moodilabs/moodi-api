package com.moodi.support.domain;

/** `Received`(접수) → `Answered`(답변완료). 되돌아가지 않는다. */
public enum InquiryStatus {
    RECEIVED,
    ANSWERED
}
