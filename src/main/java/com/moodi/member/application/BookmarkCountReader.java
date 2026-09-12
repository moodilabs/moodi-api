package com.moodi.member.application;

import java.util.UUID;

/**
 * 스팟 컨텍스트의 북마크 수를 읽는 포트. 마이 메인(`MY-01-01`)의 "Saved spots"에 쓰인다.
 */
public interface BookmarkCountReader {

    long countByMemberId(UUID memberId);
}
