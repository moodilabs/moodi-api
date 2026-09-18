package com.moodi.route.domain;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 루트 공유 단축 링크({@code /s/{code}})에 쓰는 코드.
 *
 * <p>UUID(36자)는 카카오톡 카드·문자 메시지에 넣기엔 길고 손으로 옮겨 적기도 어렵다. base62 8자리면
 * 62^8 ≈ 2.2×10^14 가지라 무작위 충돌은 사실상 없고, 혹시 겹치더라도 {@code uk_route_short_code}
 * 유니크 인덱스가 막아준다. 0/O·1/l 같은 헷갈리는 글자는 굳이 빼지 않았다 — 링크는 탭해서 여는
 * 용도지 읽어서 입력하는 용도가 아니다.
 */
public final class RouteShortCode {

    public static final int LENGTH = 8;

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern VALID = Pattern.compile("^[0-9A-Za-z]{" + LENGTH + "}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private RouteShortCode() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 경로 변수로 들어온 값이 코드 형식인지. DB 조회 전에 걸러 쓸데없는 쿼리를 막는다. */
    public static boolean isValid(String code) {
        return code != null && VALID.matcher(code).matches();
    }
}
