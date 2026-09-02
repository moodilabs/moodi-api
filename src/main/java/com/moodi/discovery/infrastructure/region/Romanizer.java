package com.moodi.discovery.infrastructure.region;

/**
 * 한글 지명을 국어의 로마자 표기법(Revised Romanization)으로 옮긴다.
 *
 * <p>동·면은 시·도(20개)·시·군·구(209개)와 달리 수천 개라 사전을 손으로 만들 수 없다.
 * 대신 표기법이 규칙으로 정의돼 있어 변환으로 대체한다. 정부 표준이라 도로 표지판·지하철 표기와도 같아,
 * 외국인 사용자가 화면에서 본 이름을 현장에서 그대로 만나게 된다.
 *
 * <p><b>행정 단위 접미사는 떼고 어간만 돌려준다</b>(성수동 → {@code Seongsu}). 화면 Chip이
 * 상위 지역과 함께 {@code Seongsu, Seoul}로 표기하기 때문이다.
 *
 * <p><b>한계</b> — 표기법의 음운 변화 규칙 중 지명에 흔한 것만 반영한다. 된소리되기는 표기에
 * 반영하지 않는 것이 규칙이므로 다루지 않고, 체언 내부의 ㄱ·ㄷ·ㅂ+ㅎ 축약처럼 드문 경우도 생략했다.
 * 관용 표기가 규칙과 다른 지명(예: 고유명사로 굳은 회사·브랜드명)은 그대로 규칙을 따른다.
 */
public final class Romanizer {

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final int MEDIAL_COUNT = 21;
    private static final int FINAL_COUNT = 28;

    /** 초성 19개. ㅇ은 소리가 없어 빈 문자열이다. */
    private static final String[] INITIALS = {
            "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    };

    private static final String[] MEDIALS = {
            "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo",
            "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    };

    /** 종성 28개. 0번은 받침 없음. */
    private static final String[] FINALS = {
            "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l",
            "m", "p", "", "t", "t", "ng", "t", "t", "k", "t", "p", "t"
    };

    /**
     * 행정 단위 접미사. 뒤에서부터 한 번만 떼며, 한 글자만 남는 이름은 건드리지 않는다
     * (예: `우동`에서 `동`을 떼면 `우` 하나만 남아 어느 동네인지 알 수 없다).
     *
     * <p><b>`리`·`가`는 일부러 빼 두었다.</b> 행정 단위로도 쓰이지만 이름의 일부로 굳은 지명이 많아
     * (왕십리 · 광안리 · 을지로3가) 떼면 엉뚱한 이름이 된다. 반대로 `동`·`면`·`읍`은
     * 이름의 일부로 쓰이는 경우가 드물어 떼도 안전하다.
     */
    private static final char[] ADMIN_SUFFIXES = {'동', '면', '읍'};

    private Romanizer() {
    }

    /**
     * 행정 단위 접미사를 뗀 어간을 로마자로 옮긴다. 한글이 아닌 글자는 그대로 남긴다.
     */
    public static String romanizePlaceName(String koreanName) {
        if (koreanName == null || koreanName.isBlank()) {
            return koreanName;
        }
        return capitalize(romanize(stripAdminSuffix(koreanName.trim())));
    }

    private static String stripAdminSuffix(String name) {
        if (name.length() < 3) {
            return name;
        }
        char last = name.charAt(name.length() - 1);
        for (char suffix : ADMIN_SUFFIXES) {
            if (last == suffix) {
                return name.substring(0, name.length() - 1);
            }
        }
        return name;
    }

    /**
     * 앞 음절의 종성과 뒤 음절의 초성이 만나는 자리에서 소리가 바뀐다(자음동화·유음화 등).
     * 표기법은 <b>바뀐 소리대로 적는다</b>고 규정하므로, 음절을 따로 옮기지 않고 경계를 함께 본다.
     */
    private static String romanize(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (!isHangulSyllable(ch)) {
                result.append(ch);
                continue;
            }
            int code = ch - HANGUL_BASE;
            int initial = code / (MEDIAL_COUNT * FINAL_COUNT);
            int medial = (code % (MEDIAL_COUNT * FINAL_COUNT)) / FINAL_COUNT;
            int finalConsonant = code % FINAL_COUNT;

            int nextInitial = nextInitialOf(name, i);
            result.append(initialSound(initial, previousFinalOf(name, i)));
            result.append(MEDIALS[medial]);
            result.append(finalSound(finalConsonant, nextInitial));
        }
        return result.toString();
    }

    /**
     * 종성이 뒤 초성을 만나 바뀌는 자리. 뒤 음절이 ㅇ으로 시작하면 종성이 그 자리로 넘어가므로
     * (연음) 여기서는 비우고, 넘겨받은 소리는 {@link #initialSound}가 적는다.
     */
    private static String finalSound(int finalConsonant, int nextInitial) {
        if (finalConsonant == 0) {
            return "";
        }
        String base = FINALS[finalConsonant];
        if (nextInitial == NO_SYLLABLE) {
            return base;
        }
        if (nextInitial == INITIAL_IEUNG) {
            return "";
        }
        return assimilate(base, nextInitial);
    }

    /**
     * 자음동화. 비음화(ㄱ·ㄷ·ㅂ + ㄴ·ㅁ)와 유음화(ㄴ+ㄹ, ㄹ+ㄴ)를 반영한다.
     * 지명에서 실제로 자주 걸리는 경우다(종로 → Jongno, 신라 → Silla, 왕십리 → Wangsimni).
     */
    private static String assimilate(String finalSound, int nextInitial) {
        boolean nextIsNasal = nextInitial == INITIAL_NIEUN || nextInitial == INITIAL_MIEUM;
        boolean nextIsLiquid = nextInitial == INITIAL_RIEUL;

        if (nextIsNasal) {
            return switch (finalSound) {
                case "k" -> "ng";
                case "t" -> "n";
                case "p" -> "m";
                default -> finalSound;
            };
        }
        if (nextIsLiquid) {
            return switch (finalSound) {
                case "k" -> "ng";
                case "t", "n" -> "l";
                case "p" -> "m";
                case "ng", "m" -> finalSound;
                default -> finalSound;
            };
        }
        return finalSound;
    }

    /**
     * 초성이 앞 종성의 영향을 받는 자리. ㄹ은 앞이 ㄴ·ㄹ이면 l로, 그 밖에는 어두면 r로 적는다.
     * ㅇ 초성은 앞 종성을 연음으로 넘겨받는다.
     */
    private static String initialSound(int initial, int previousFinal) {
        if (initial == INITIAL_IEUNG) {
            return previousFinal <= 0 ? "" : liaison(previousFinal);
        }
        if (initial == INITIAL_RIEUL) {
            // 앞 음절에 받침이 없으면 모음 사이라 r로 적는다 (을지로 → Euljiro).
            if (previousFinal <= 0) {
                return "r";
            }
            String previous = FINALS[previousFinal];
            return previous.equals("n") || previous.equals("l") ? "l" : "n";
        }
        return INITIALS[initial];
    }

    /**
     * 연음. 앞 종성이 뒤 음절 초성 자리로 옮겨가므로 종성이 아니라 <b>초성 소리</b>로 적는다
     * (예: 강원 → Gangwon이 아니라 종성 ㅇ은 ng, 삽교 같은 겹받침은 대표음).
     */
    private static String liaison(int previousFinal) {
        return switch (FINALS[previousFinal]) {
            case "k" -> "g";
            case "t" -> "d";
            case "p" -> "b";
            case "l" -> "r";
            case "ng" -> "ng";
            default -> FINALS[previousFinal];
        };
    }

    private static int nextInitialOf(String name, int index) {
        if (index + 1 >= name.length()) {
            return NO_SYLLABLE;
        }
        char next = name.charAt(index + 1);
        if (!isHangulSyllable(next)) {
            return NO_SYLLABLE;
        }
        return (next - HANGUL_BASE) / (MEDIAL_COUNT * FINAL_COUNT);
    }

    private static int previousFinalOf(String name, int index) {
        if (index == 0) {
            return NO_SYLLABLE;
        }
        char previous = name.charAt(index - 1);
        if (!isHangulSyllable(previous)) {
            return NO_SYLLABLE;
        }
        return (previous - HANGUL_BASE) % FINAL_COUNT;
    }

    private static boolean isHangulSyllable(char ch) {
        return ch >= HANGUL_BASE && ch <= HANGUL_LAST;
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static final int NO_SYLLABLE = -1;
    private static final int INITIAL_NIEUN = 2;
    private static final int INITIAL_RIEUL = 5;
    private static final int INITIAL_IEUNG = 11;
    private static final int INITIAL_MIEUM = 6;
}
