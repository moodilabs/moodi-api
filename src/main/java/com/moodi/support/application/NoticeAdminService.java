package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.NoticeCommand;
import com.moodi.support.application.dto.NoticeCursor;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeRepository;
import com.moodi.support.domain.NoticeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 어드민의 공지 관리. 숨김 공지도 다룬다. 컨트롤러는 관리자 인증(`ADM-F01`)과 함께 붙는다.
 */
@Service
@Transactional
public class NoticeAdminService {

    private final NoticeRepository noticeRepository;
    private final NoticeQueryRepository noticeQueryRepository;

    public NoticeAdminService(NoticeRepository noticeRepository, NoticeQueryRepository noticeQueryRepository) {
        this.noticeRepository = noticeRepository;
        this.noticeQueryRepository = noticeQueryRepository;
    }

    public Long create(NoticeCommand command) {
        Notice notice = Notice.create(command.type(), command.title(), command.content(), command.visible(),
                command.publishedAt());
        return noticeRepository.save(notice).getId();
    }

    public void update(Long noticeId, NoticeCommand command) {
        Notice notice = findNotice(noticeId);
        notice.update(command.type(), command.title(), command.content(), command.visible(),
                command.publishedAt());
        noticeRepository.save(notice);
    }

    public void changeVisibility(Long noticeId, boolean visible) {
        Notice notice = findNotice(noticeId);
        notice.changeVisibility(visible);
        noticeRepository.save(notice);
    }

    public void delete(Long noticeId) {
        noticeRepository.delete(findNotice(noticeId));
    }

    @Transactional(readOnly = true)
    public NoticeDetail get(Long noticeId) {
        return NoticeDetail.from(findNotice(noticeId));
    }

    @Transactional(readOnly = true)
    public CursorResponse<NoticeDetail> getNotices(NoticeType type, Boolean visible, String cursor, int size) {
        NoticeCursor parsed = NoticeCursor.parse(cursor);
        List<Notice> rows = noticeQueryRepository.findAll(type, visible,
                parsed == null ? null : parsed.publishedAt(),
                parsed == null ? null : parsed.id(),
                size + 1);

        boolean hasNext = rows.size() > size;
        List<Notice> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        List<NoticeDetail> items = page.stream().map(NoticeDetail::from).toList();
        String nextCursor = hasNext ? NoticeCursor.of(page.getLast()) : null;
        return CursorResponse.of(items, nextCursor, hasNext);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
