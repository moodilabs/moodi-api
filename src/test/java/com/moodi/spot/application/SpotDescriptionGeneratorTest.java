package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotDescriptionContext;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotDescriptionGeneratorTest {

    @Mock
    private SpotDescriptionRepository descriptionRepository;

    @Mock
    private SpotDescriptionClient descriptionClient;

    @Mock
    private SpotDescriptionWriter descriptionWriter;

    @InjectMocks
    private SpotDescriptionGenerator descriptionGenerator;

    @Test
    @DisplayName("기존 AI 설명이 있으면 LLM을 호출하지 않는다")
    void get_or_generate_returns_existing_description() {
        // given
        SpotDescriptionContext context = createContext(1L);
        SpotDescription existing = SpotDescription.create(1L, "ko-KR", "기존 설명입니다.");

        when(descriptionRepository.findBySpotIdAndLocale(1L, "ko-KR"))
                .thenReturn(Optional.of(existing));

        // when
        String result = descriptionGenerator.getOrGenerate(context);

        // then
        assertThat(result).isEqualTo("기존 설명입니다.");
        verify(descriptionClient, never()).generate(any(), any(), any(), any(), any(), any());
        verify(descriptionWriter, never()).saveIfAbsent(any(), any(), any());
    }

    @Test
    @DisplayName("AI 설명이 없으면 LLM 호출 후 저장한다")
    void get_or_generate_creates_new_description() {
        // given
        SpotDescriptionContext context = createContext(1L);
        SpotDescription saved = SpotDescription.create(1L, "ko-KR", "AI가 생성한 설명입니다.");

        when(descriptionRepository.findBySpotIdAndLocale(1L, "ko-KR"))
                .thenReturn(Optional.empty());
        when(descriptionClient.generate(
                eq("가회동성당"), eq("TOURIST_ATTRACTION"), eq("서울"),
                eq("#Cozy, #Serene"), any(), eq("ko-KR")))
                .thenReturn("AI가 생성한 설명입니다.");
        when(descriptionWriter.saveIfAbsent(1L, "ko-KR", "AI가 생성한 설명입니다."))
                .thenReturn(saved);

        // when
        String result = descriptionGenerator.getOrGenerate(context);

        // then
        assertThat(result).isEqualTo("AI가 생성한 설명입니다.");
        verify(descriptionClient).generate(any(), any(), any(), any(), any(), any());
        verify(descriptionWriter).saveIfAbsent(1L, "ko-KR", "AI가 생성한 설명입니다.");
    }

    @Test
    @DisplayName("LLM 호출 실패 시 null을 반환하고 상세 API는 정상 동작한다")
    void get_or_generate_returns_null_on_llm_failure() {
        // given
        SpotDescriptionContext context = createContext(1L);

        when(descriptionRepository.findBySpotIdAndLocale(1L, "ko-KR"))
                .thenReturn(Optional.empty());
        when(descriptionClient.generate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("LLM API 호출 실패"));

        // when
        String result = descriptionGenerator.getOrGenerate(context);

        // then
        assertThat(result).isNull();
        verify(descriptionWriter, never()).saveIfAbsent(any(), any(), any());
    }

    @Test
    @DisplayName("LLM은 성공했지만 저장 실패 시 null을 반환한다")
    void get_or_generate_returns_null_on_save_failure() {
        // given
        SpotDescriptionContext context = createContext(1L);

        when(descriptionRepository.findBySpotIdAndLocale(1L, "ko-KR"))
                .thenReturn(Optional.empty());
        when(descriptionClient.generate(any(), any(), any(), any(), any(), any()))
                .thenReturn("생성된 설명");
        when(descriptionWriter.saveIfAbsent(any(), any(), any()))
                .thenThrow(new RuntimeException("DB 저장 실패"));

        // when
        String result = descriptionGenerator.getOrGenerate(context);

        // then
        assertThat(result).isNull();
    }

    private SpotDescriptionContext createContext(Long spotId) {
        return new SpotDescriptionContext(
                spotId, "가회동성당", "TOURIST_ATTRACTION", "서울",
                List.of("#Cozy", "#Serene"), "가회동성당은 종로구에 위치한 성당이다."
        );
    }
}
