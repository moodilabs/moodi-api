package com.moodi.spot.application;

import com.moodi.shared.event.MemberWithdrawnEvent;
import com.moodi.spot.domain.BookmarkRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 탈퇴 시 북마크 삭제 — 화면 정책 "저장한 스팟을 영구 삭제". 탈퇴 트랜잭션 안에서 실행된다. */
@Component
public class BookmarkWithdrawalListener {

    private final BookmarkRepository bookmarkRepository;

    public BookmarkWithdrawalListener(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        bookmarkRepository.deleteByMemberId(event.memberId());
    }
}
