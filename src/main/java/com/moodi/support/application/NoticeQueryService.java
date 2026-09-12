package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.NoticeCursor;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.application.dto.NoticeSummary;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 앱의 공지 조회(`MY-04`). 노출 상태인 공지만 다룬다.
 */
@Service
@Transactional(readOnly = true)
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;
    private final NoticeQueryRepository noticeQueryRepository;

    public NoticeQueryService(NoticeRepository noticeRepository, NoticeQueryRepository noticeQueryRepository) {
        this.noticeRepository = noticeRepository;
        this.noticeQueryRepository = noticeQueryRepository;
    }

    public CursorResponse<NoticeSummary> getVisibleNotices(String cursor, int size) {
        NoticeCursor parsed = NoticeCursor.parse(cursor);
        List<Notice> rows = noticeQueryRepository.findVisible(
                parsed == null ? null : parsed.publishedAt(),
                parsed == null ? null : parsed.id(),
                size + 1);

        boolean hasNext = rows.size() > size;
        List<Notice> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        List<NoticeSummary> items = page.stream().map(NoticeSummary::from).toList();
        String nextCursor = hasNext ? NoticeCursor.of(page.getLast()) : null;
        return CursorResponse.of(items, nextCursor, hasNext);
    }

    public NoticeDetail getVisibleNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .filter(Notice::isVisible)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
        return NoticeDetail.from(notice);
    }
}
