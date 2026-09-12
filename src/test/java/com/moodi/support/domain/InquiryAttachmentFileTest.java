package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryAttachmentFileTest {

    @Test
    @DisplayName("허용 형식·용량이면 회원 경로의 객체 이름을 만든다")
    void of_accepts_supported_type_and_builds_member_scoped_name() {
        UUID memberId = UUID.randomUUID();

        InquiryAttachmentFile file = InquiryAttachmentFile.of("image/JPEG", 1024);

        assertThat(file.getType()).isEqualTo(InquiryAttachmentType.JPEG);
        assertThat(file.objectName(memberId))
                .startsWith("inquiries/" + memberId + "/")
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("동영상은 50MB까지, 사진은 10MB까지 허용한다")
    void size_limit_depends_on_type() {
        assertThat(InquiryAttachmentFile.of("video/mp4", 40L * 1024 * 1024).getContentLength())
                .isEqualTo(40L * 1024 * 1024);

        assertThatThrownBy(() -> InquiryAttachmentFile.of("image/png", 11L * 1024 * 1024))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_ATTACHMENT_TOO_LARGE);
    }

    @Test
    @DisplayName("지원하지 않는 형식은 거부한다")
    void of_rejects_unsupported_type() {
        assertThatThrownBy(() -> InquiryAttachmentFile.of("application/zip", 1024))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE);
    }
}
