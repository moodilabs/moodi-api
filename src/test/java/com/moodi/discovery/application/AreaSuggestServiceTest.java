package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickAreaLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AreaSuggestServiceTest {

    private static final int LIMIT = 20;

    @Mock
    private AreaSuggestReader areaSuggestReader;

    @InjectMocks
    private AreaSuggestService areaSuggestService;

    @Test
    @DisplayName("검색어로 지역 후보를 조회한다")
    void search_returns_suggestions() {
        given(areaSuggestReader.search("seoul", LIMIT))
                .willReturn(List.of(new AreaSuggestion(PickAreaLevel.REGION, "Seoul", null, null, "Seoul")));

        List<AreaSuggestion> suggestions = areaSuggestService.search("seoul", LIMIT);

        assertThat(suggestions).extracting(AreaSuggestion::region).containsExactly("Seoul");
    }

    @Test
    @DisplayName("앞뒤 공백은 제거하고 조회한다")
    void search_trims_keyword() {
        given(areaSuggestReader.search("seoul", LIMIT)).willReturn(List.of());

        areaSuggestService.search("  seoul  ", LIMIT);

        verify(areaSuggestReader).search("seoul", LIMIT);
    }

    @Test
    @DisplayName("한 글자 검색어는 조회 없이 빈 목록이다")
    void search_with_single_character_returns_empty() {
        List<AreaSuggestion> suggestions = areaSuggestService.search("s", LIMIT);

        assertThat(suggestions).isEmpty();
        verify(areaSuggestReader, never()).search(anyString(), anyInt());
    }

    @Test
    @DisplayName("공백만 있는 검색어는 조회 없이 빈 목록이다")
    void search_with_blank_keyword_returns_empty() {
        List<AreaSuggestion> suggestions = areaSuggestService.search("   ", LIMIT);

        assertThat(suggestions).isEmpty();
        verify(areaSuggestReader, never()).search(anyString(), anyInt());
    }

    @Test
    @DisplayName("검색어가 없으면 조회 없이 빈 목록이다 - 타이핑 전 상태는 오류가 아니다")
    void search_with_null_keyword_returns_empty() {
        List<AreaSuggestion> suggestions = areaSuggestService.search(null, LIMIT);

        assertThat(suggestions).isEmpty();
        verify(areaSuggestReader, never()).search(anyString(), anyInt());
    }
}
