package com.moodi.discovery.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지역 자동완성 (`DSC-04`).
 *
 * <p>자유 입력을 허용하지 않는 필드라(`COM-P03`) 여기서 돌려준 값만 {@code POST /picks}로 되돌아온다.
 * 그래서 <b>실제로 스팟이 있는 지역만</b> 내보내는 것이 이 API의 계약이다.
 */
@Service
@Transactional(readOnly = true)
public class AreaSuggestService {

    /**
     * 키워드가 한 글자면 후보가 지나치게 넓어져 고르는 데 도움이 되지 않는다.
     * 화면도 입력에 따라 좁혀지는 패널을 전제한다.
     */
    private static final int MIN_KEYWORD_LENGTH = 2;

    private final AreaSuggestReader areaSuggestReader;

    public AreaSuggestService(AreaSuggestReader areaSuggestReader) {
        this.areaSuggestReader = areaSuggestReader;
    }

    /**
     * 짧거나 빈 키워드는 오류가 아니라 <b>빈 목록</b>이다. 타이핑 중에 매번 400을 돌려주면
     * 클라이언트가 정상 입력 과정을 오류로 다뤄야 한다.
     */
    public List<AreaSuggestion> search(String keyword, int limit) {
        if (keyword == null || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            return List.of();
        }
        return areaSuggestReader.search(keyword.trim(), limit);
    }
}
