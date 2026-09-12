package com.moodi.support.application;

import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;
import java.util.List;

/**
 * 공지 목록 조회 포트. 정렬은 항상 `publishedAt DESC, id DESC`이고 커서는 마지막 항목의 (publishedAt, id)다.
 * `size + 1`건을 돌려주면 호출자가 hasNext를 판단한다.
 */
public interface NoticeQueryRepository {

    List<Notice> findVisible(LocalDate cursorPublishedAt, Long cursorId, int limit);

    List<Notice> findAll(NoticeType type, Boolean visible, LocalDate cursorPublishedAt, Long cursorId, int limit);
}
