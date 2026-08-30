package com.moodi.spot.application;

import java.util.Map;

public class RegionDictionary {

    private static final Map<String, String> AREA_MAP = Map.ofEntries(
            Map.entry("서울", "Seoul"),
            Map.entry("부산", "Busan"),
            Map.entry("대구", "Daegu"),
            Map.entry("인천", "Incheon"),
            Map.entry("광주", "Gwangju"),
            Map.entry("대전", "Daejeon"),
            Map.entry("울산", "Ulsan"),
            Map.entry("세종", "Sejong"),
            Map.entry("경기", "Gyeonggi"),
            Map.entry("강원", "Gangwon"),
            Map.entry("충북", "Chungbuk"),
            Map.entry("충남", "Chungnam"),
            Map.entry("전북", "Jeonbuk"),
            Map.entry("전남", "Jeonnam"),
            Map.entry("경북", "Gyeongbuk"),
            Map.entry("경남", "Gyeongnam"),
            Map.entry("제주", "Jeju"),
            Map.entry("강릉", "Gangneung"),
            Map.entry("경주", "Gyeongju"),
            Map.entry("전주", "Jeonju")
    );

    private static final Map<String, String> DISTRICT_MAP = Map.ofEntries(
            // 서울
            Map.entry("종로구", "Jongno-gu"), Map.entry("중구", "Jung-gu"),
            Map.entry("용산구", "Yongsan-gu"), Map.entry("성동구", "Seongdong-gu"),
            Map.entry("광진구", "Gwangjin-gu"), Map.entry("동대문구", "Dongdaemun-gu"),
            Map.entry("중랑구", "Jungnang-gu"), Map.entry("성북구", "Seongbuk-gu"),
            Map.entry("강북구", "Gangbuk-gu"), Map.entry("도봉구", "Dobong-gu"),
            Map.entry("노원구", "Nowon-gu"), Map.entry("은평구", "Eunpyeong-gu"),
            Map.entry("서대문구", "Seodaemun-gu"), Map.entry("마포구", "Mapo-gu"),
            Map.entry("양천구", "Yangcheon-gu"), Map.entry("강서구", "Gangseo-gu"),
            Map.entry("구로구", "Guro-gu"), Map.entry("금천구", "Geumcheon-gu"),
            Map.entry("영등포구", "Yeongdeungpo-gu"), Map.entry("동작구", "Dongjak-gu"),
            Map.entry("관악구", "Gwanak-gu"), Map.entry("서초구", "Seocho-gu"),
            Map.entry("강남구", "Gangnam-gu"), Map.entry("송파구", "Songpa-gu"),
            Map.entry("강동구", "Gangdong-gu"),
            // 부산
            Map.entry("해운대구", "Haeundae-gu"), Map.entry("수영구", "Suyeong-gu"),
            Map.entry("남구", "Nam-gu"), Map.entry("연제구", "Yeonje-gu"),
            Map.entry("동구", "Dong-gu"), Map.entry("영도구", "Yeongdo-gu"),
            Map.entry("부산진구", "Busanjin-gu"), Map.entry("사하구", "Saha-gu"),
            Map.entry("북구", "Buk-gu"), Map.entry("사상구", "Sasang-gu"),
            Map.entry("금정구", "Geumjeong-gu"), Map.entry("연수구", "Yeonsu-gu"),
            Map.entry("동래구", "Dongnae-gu"),
            Map.entry("기장군", "Gijang-gun"), Map.entry("서구", "Seo-gu"),
            // 인천
            Map.entry("미추홀구", "Michuhol-gu"),
            Map.entry("남동구", "Namdong-gu"), Map.entry("부평구", "Bupyeong-gu"),
            Map.entry("계양구", "Gyeyang-gu"), Map.entry("옹진군", "Ongjin-gun"),
            Map.entry("강화군", "Ganghwa-gun"), Map.entry("검단구", "Geomdan-gu"),
            Map.entry("서해구", "Seohae-gu"), Map.entry("영종구", "Yeongjong-gu"),
            Map.entry("제물포구", "Jemulpo-gu"),
            // 대구
            Map.entry("수성구", "Suseong-gu"), Map.entry("달서구", "Dalseo-gu"),
            Map.entry("달성군", "Dalseong-gun"),
            // 광주
            Map.entry("광산구", "Gwangsan-gu"),
            // 대전
            Map.entry("유성구", "Yuseong-gu"), Map.entry("대덕구", "Daedeok-gu"),
            // 울산
            Map.entry("울주군", "Ulju-gun"),
            // 경기
            Map.entry("수원시", "Suwon-si"), Map.entry("성남시", "Seongnam-si"),
            Map.entry("고양시", "Goyang-si"), Map.entry("용인시", "Yongin-si"),
            Map.entry("부천시", "Bucheon-si"), Map.entry("안산시", "Ansan-si"),
            Map.entry("안양시", "Anyang-si"), Map.entry("남양주시", "Namyangju-si"),
            Map.entry("화성시", "Hwaseong-si"), Map.entry("평택시", "Pyeongtaek-si"),
            Map.entry("의정부시", "Uijeongbu-si"), Map.entry("시흥시", "Siheung-si"),
            Map.entry("파주시", "Paju-si"), Map.entry("김포시", "Gimpo-si"),
            Map.entry("광명시", "Gwangmyeong-si"), Map.entry("광주시", "Gwangju-si"),
            Map.entry("군포시", "Gunpo-si"), Map.entry("하남시", "Hanam-si"),
            Map.entry("오산시", "Osan-si"), Map.entry("이천시", "Icheon-si"),
            Map.entry("안성시", "Anseong-si"), Map.entry("의왕시", "Uiwang-si"),
            Map.entry("양평군", "Yangpyeong-gun"), Map.entry("여주시", "Yeoju-si"),
            Map.entry("동두천시", "Dongducheon-si"), Map.entry("과천시", "Gwacheon-si"),
            Map.entry("가평군", "Gapyeong-gun"), Map.entry("연천군", "Yeoncheon-gun"),
            Map.entry("양주시", "Yangju-si"), Map.entry("포천시", "Pocheon-si"),
            Map.entry("구리시", "Guri-si"),
            // 강원
            Map.entry("춘천시", "Chuncheon-si"), Map.entry("원주시", "Wonju-si"),
            Map.entry("강릉시", "Gangneung-si"), Map.entry("동해시", "Donghae-si"),
            Map.entry("태백시", "Taebaek-si"), Map.entry("속초시", "Sokcho-si"),
            Map.entry("삼척시", "Samcheok-si"), Map.entry("홍천군", "Hongcheon-gun"),
            Map.entry("횡성군", "Hoengseong-gun"), Map.entry("영월군", "Yeongwol-gun"),
            Map.entry("평창군", "Pyeongchang-gun"), Map.entry("정선군", "Jeongseon-gun"),
            Map.entry("철원군", "Cheorwon-gun"), Map.entry("화천군", "Hwacheon-gun"),
            Map.entry("양구군", "Yanggu-gun"), Map.entry("인제군", "Inje-gun"),
            Map.entry("고성군", "Goseong-gun"), Map.entry("양양군", "Yangyang-gun"),
            // 충북
            Map.entry("청주시", "Cheongju-si"), Map.entry("충주시", "Chungju-si"),
            Map.entry("제천시", "Jecheon-si"), Map.entry("보은군", "Boeun-gun"),
            Map.entry("옥천군", "Okcheon-gun"), Map.entry("영동군", "Yeongdong-gun"),
            Map.entry("증평군", "Jeungpyeong-gun"), Map.entry("진천군", "Jincheon-gun"),
            Map.entry("괴산군", "Goesan-gun"), Map.entry("음성군", "Eumseong-gun"),
            Map.entry("단양군", "Danyang-gun"),
            // 충남
            Map.entry("천안시", "Cheonan-si"), Map.entry("공주시", "Gongju-si"),
            Map.entry("보령시", "Boryeong-si"), Map.entry("아산시", "Asan-si"),
            Map.entry("서산시", "Seosan-si"), Map.entry("논산시", "Nonsan-si"),
            Map.entry("계룡시", "Gyeryong-si"), Map.entry("당진시", "Dangjin-si"),
            Map.entry("금산군", "Geumsan-gun"), Map.entry("부여군", "Buyeo-gun"),
            Map.entry("서천군", "Seocheon-gun"), Map.entry("청양군", "Cheongyang-gun"),
            Map.entry("홍성군", "Hongseong-gun"), Map.entry("예산군", "Yesan-gun"),
            Map.entry("태안군", "Taean-gun"),
            // 전북
            Map.entry("전주시", "Jeonju-si"), Map.entry("군산시", "Gunsan-si"),
            Map.entry("익산시", "Iksan-si"), Map.entry("정읍시", "Jeongeup-si"),
            Map.entry("남원시", "Namwon-si"), Map.entry("김제시", "Gimje-si"),
            Map.entry("완주군", "Wanju-gun"), Map.entry("진안군", "Jinan-gun"),
            Map.entry("무주군", "Muju-gun"), Map.entry("장수군", "Jangsu-gun"),
            Map.entry("임실군", "Imsil-gun"), Map.entry("순창군", "Sunchang-gun"),
            Map.entry("고창군", "Gochang-gun"), Map.entry("부안군", "Buan-gun"),
            // 전남
            Map.entry("목포시", "Mokpo-si"), Map.entry("여수시", "Yeosu-si"),
            Map.entry("순천시", "Suncheon-si"), Map.entry("나주시", "Naju-si"),
            Map.entry("광양시", "Gwangyang-si"), Map.entry("담양군", "Damyang-gun"),
            Map.entry("곡성군", "Gokseong-gun"), Map.entry("구례군", "Gurye-gun"),
            Map.entry("고흥군", "Goheung-gun"), Map.entry("보성군", "Boseong-gun"),
            Map.entry("화순군", "Hwasun-gun"), Map.entry("장흥군", "Jangheung-gun"),
            Map.entry("강진군", "Gangjin-gun"), Map.entry("해남군", "Haenam-gun"),
            Map.entry("영암군", "Yeongam-gun"), Map.entry("무안군", "Muan-gun"),
            Map.entry("함평군", "Hampyeong-gun"), Map.entry("영광군", "Yeonggwang-gun"),
            Map.entry("장성군", "Jangseong-gun"), Map.entry("완도군", "Wando-gun"),
            Map.entry("진도군", "Jindo-gun"), Map.entry("신안군", "Sinan-gun"),
            // 경북
            Map.entry("포항시", "Pohang-si"), Map.entry("경주시", "Gyeongju-si"),
            Map.entry("김천시", "Gimcheon-si"), Map.entry("안동시", "Andong-si"),
            Map.entry("구미시", "Gumi-si"), Map.entry("영주시", "Yeongju-si"),
            Map.entry("영천시", "Yeongcheon-si"), Map.entry("상주시", "Sangju-si"),
            Map.entry("문경시", "Mungyeong-si"), Map.entry("경산시", "Gyeongsan-si"),
            Map.entry("의성군", "Uiseong-gun"), Map.entry("청송군", "Cheongsong-gun"),
            Map.entry("영양군", "Yeongyang-gun"), Map.entry("영덕군", "Yeongdeok-gun"),
            Map.entry("청도군", "Cheongdo-gun"), Map.entry("고령군", "Goryeong-gun"),
            Map.entry("성주군", "Seongju-gun"), Map.entry("칠곡군", "Chilgok-gun"),
            Map.entry("예천군", "Yecheon-gun"), Map.entry("봉화군", "Bonghwa-gun"),
            Map.entry("울진군", "Uljin-gun"), Map.entry("울릉군", "Ulleung-gun"),
            // 경남
            Map.entry("창원시", "Changwon-si"), Map.entry("진주시", "Jinju-si"),
            Map.entry("통영시", "Tongyeong-si"), Map.entry("사천시", "Sacheon-si"),
            Map.entry("김해시", "Gimhae-si"), Map.entry("밀양시", "Miryang-si"),
            Map.entry("거제시", "Geoje-si"), Map.entry("양산시", "Yangsan-si"),
            Map.entry("의령군", "Uiryeong-gun"), Map.entry("함안군", "Haman-gun"),
            Map.entry("창녕군", "Changnyeong-gun"),
            Map.entry("남해군", "Namhae-gun"), Map.entry("하동군", "Hadong-gun"),
            Map.entry("산청군", "Sancheong-gun"), Map.entry("함양군", "Hamyang-gun"),
            Map.entry("거창군", "Geochang-gun"), Map.entry("합천군", "Hapcheon-gun"),
            // 제주
            Map.entry("제주시", "Jeju-si"), Map.entry("서귀포시", "Seogwipo-si")
    );

    private static final Map<String, String> REVERSE_AREA_MAP = invert(AREA_MAP);
    private static final Map<String, String> REVERSE_DISTRICT_MAP = invert(DISTRICT_MAP);

    private RegionDictionary() {
    }

    private static Map<String, String> invert(Map<String, String> source) {
        return source.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public static String translateArea(String koArea) {
        if (koArea == null) {
            return null;
        }
        return AREA_MAP.getOrDefault(koArea, koArea);
    }

    public static String translateDistrict(String koDistrict) {
        if (koDistrict == null) {
            return null;
        }
        return DISTRICT_MAP.getOrDefault(koDistrict, koDistrict);
    }

    /**
     * 영문 지역명을 원장에 저장된 한국어로 되돌린다.
     *
     * <p>응답이 영문으로 나가므로 클라이언트는 영문 지역명을 되돌려 준다. 조회 조건으로 쓰려면
     * 다시 한국어로 바꿔야 한다. 사전에 없는 값은 그대로 두어 한국어를 직접 보내도 동작한다.
     */
    public static String toKoreanArea(String area) {
        if (area == null) {
            return null;
        }
        return REVERSE_AREA_MAP.getOrDefault(area, area);
    }

    public static String toKoreanDistrict(String district) {
        if (district == null) {
            return null;
        }
        return REVERSE_DISTRICT_MAP.getOrDefault(district, district);
    }
}
