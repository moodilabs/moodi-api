package com.moodi.discovery.infrastructure.region;

import com.moodi.spot.application.RegionDictionary;

/**
 * 지역명 표기를 맞추는 지점. 원장은 한국어로 저장하고 응답은 영문으로 내보낸다.
 *
 * <p>사전 자체는 {@code spot}이 갖고 있다. 스팟 원장의 지역 값이 그쪽 소관이라
 * 사전을 두 벌로 나누면 표기가 갈린다. 컨텍스트 경계를 지키기 위해 참조는 이 어댑터에만 둔다.
 *
 * <p>두 컨텍스트가 함께 쓰게 됐으므로 {@code shared}로 옮기는 편이 맞지만,
 * 공유 커널 변경은 합의가 필요해 지금은 위임으로 둔다.
 */
public final class RegionNames {

    private RegionNames() {
    }

    public static String toEnglishArea(String koreanArea) {
        return RegionDictionary.translateArea(koreanArea);
    }

    /**
     * 응답이 영문으로 나가므로 클라이언트가 되돌려 주는 지역명도 영문이다.
     * 조회 조건으로 쓰려면 원장에 저장된 한국어로 되돌려야 한다.
     */
    public static String toKoreanArea(String area) {
        return RegionDictionary.toKoreanArea(area);
    }

    public static String toKoreanDistrict(String district) {
        return RegionDictionary.toKoreanDistrict(district);
    }
}
