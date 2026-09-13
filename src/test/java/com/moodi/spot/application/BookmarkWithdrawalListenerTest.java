package com.moodi.spot.application;

import com.moodi.shared.event.MemberWithdrawnEvent;
import com.moodi.spot.domain.BookmarkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkWithdrawalListenerTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkWithdrawalListener listener;

    @Test
    @DisplayName("탈퇴 이벤트를 받으면 그 회원의 북마크를 모두 지운다")
    void deletes_bookmarks_of_withdrawn_member() {
        UUID memberId = UUID.randomUUID();

        listener.on(new MemberWithdrawnEvent(memberId));

        verify(bookmarkRepository).deleteByMemberId(memberId);
    }
}
