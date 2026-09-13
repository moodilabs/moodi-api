package com.moodi.discovery.application;

import java.util.List;
import java.util.UUID;

/** 회원 탈퇴 시 Pick 요청·지역·결과를 한 번에 지우는 포트. 지운 요청의 사진 키를 돌려준다(GCS 삭제용). */
public interface PickWithdrawalRepository {

    List<String> deleteAllByMemberId(UUID memberId);
}
