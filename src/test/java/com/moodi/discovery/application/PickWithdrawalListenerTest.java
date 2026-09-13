package com.moodi.discovery.application;

import com.moodi.shared.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickWithdrawalListenerTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private PickWithdrawalRepository pickWithdrawalRepository;

    @Mock
    private ImageStorageClient imageStorageClient;

    @InjectMocks
    private PickWithdrawalListener listener;

    @Test
    @DisplayName("트랜잭션 밖이면 행을 지운 뒤 바로 사진을 지운다")
    void deletes_rows_then_images_outside_transaction() {
        when(pickWithdrawalRepository.deleteAllByMemberId(MEMBER_ID)).thenReturn(List.of("picks/a.jpg", "picks/b.jpg"));

        listener.on(new MemberWithdrawnEvent(MEMBER_ID));

        verify(imageStorageClient).delete("picks/a.jpg");
        verify(imageStorageClient).delete("picks/b.jpg");
    }

    @Test
    @DisplayName("Pick이 없으면 사진 삭제를 호출하지 않는다")
    void skips_image_deletion_when_nothing_to_delete() {
        when(pickWithdrawalRepository.deleteAllByMemberId(MEMBER_ID)).thenReturn(List.of());

        listener.on(new MemberWithdrawnEvent(MEMBER_ID));

        verify(imageStorageClient, never()).delete(anyString());
    }

    @Test
    @DisplayName("사진 하나가 실패해도 나머지는 계속 지우고 예외를 던지지 않는다")
    void continues_after_image_deletion_failure() {
        when(pickWithdrawalRepository.deleteAllByMemberId(MEMBER_ID)).thenReturn(List.of("picks/a.jpg", "picks/b.jpg"));
        doThrow(new RuntimeException("gcs down")).when(imageStorageClient).delete("picks/a.jpg");

        listener.on(new MemberWithdrawnEvent(MEMBER_ID));

        verify(imageStorageClient).delete("picks/b.jpg");
    }
}
