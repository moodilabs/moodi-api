package com.moodi.discovery.application;

import com.moodi.shared.event.MemberWithdrawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 회원 탈퇴 시 Pick 데이터 삭제. 개인 사진이라 "계정 데이터 삭제"에 포함된다.
 * <p>
 * DB 행은 탈퇴 트랜잭션 안에서 지우고, GCS 원본 사진은 **커밋 후**에 지운다 — 탈퇴가 롤백됐는데 사진만 사라지면 안 된다.
 * 사진 삭제 실패는 로그만 남긴다(탈퇴는 이미 확정). 남은 객체는 버킷 수명 정책으로 정리한다.
 */
@Component
public class PickWithdrawalListener {

    private static final Logger log = LoggerFactory.getLogger(PickWithdrawalListener.class);

    private final PickWithdrawalRepository pickWithdrawalRepository;
    private final ImageStorageClient imageStorageClient;

    public PickWithdrawalListener(PickWithdrawalRepository pickWithdrawalRepository,
                                  ImageStorageClient imageStorageClient) {
        this.pickWithdrawalRepository = pickWithdrawalRepository;
        this.imageStorageClient = imageStorageClient;
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        List<String> imageKeys = pickWithdrawalRepository.deleteAllByMemberId(event.memberId());
        if (imageKeys.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteImages(imageKeys);
                }
            });
        } else {
            deleteImages(imageKeys);
        }
    }

    private void deleteImages(List<String> imageKeys) {
        for (String key : imageKeys) {
            try {
                imageStorageClient.delete(key);
            } catch (RuntimeException e) {
                log.warn("탈퇴 회원의 Pick 사진 삭제 실패 (버킷 수명 정책으로 정리): {}", key, e);
            }
        }
    }
}
