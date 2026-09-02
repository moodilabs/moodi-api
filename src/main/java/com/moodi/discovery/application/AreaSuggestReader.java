package com.moodi.discovery.application;

import java.util.List;

/**
 * 지역 자동완성 후보를 읽는다 (`DSC-04`).
 *
 * <p>스팟 원장에 **실제로 스팟이 있는 지역만** 돌려준다. 고를 수는 있는데 결과가 0건인 지역을
 * 자동완성에 노출하면 사용자가 빈 결과 화면으로 떨어진다.
 */
public interface AreaSuggestReader {

    List<AreaSuggestion> search(String keyword, int limit);
}
