package com.moodi.spot.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.moodi.spot.application.LlmAdmissionController;
import com.moodi.spot.application.MoodAnalysisClient;
import com.moodi.spot.application.MoodAnalysisClient.MoodAnalysisResult;
import com.moodi.spot.application.RateLimitException;
import com.moodi.spot.infrastructure.openai.ProtectedMoodAnalysisClient.LlmCallRejectedException;
import com.moodi.spot.support.MoodVectorFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProtectedMoodAnalysisClientTest {

    @Mock
    private MoodAnalysisClient delegate;

    @Mock
    private LlmAdmissionController admissionController;

    @Test
    @DisplayName("호출 허가를 받으면 delegate에 위임하고 성공을 기록한다")
    void delegates_and_records_success_when_admitted() {
        given(admissionController.tryAcquire(anyLong())).willReturn(true);
        MoodAnalysisResult expected = new MoodAnalysisResult(MoodVectorFixture.create(), 0.8, 0.3);
        given(delegate.analyze(List.of("url"), null)).willReturn(expected);

        ProtectedMoodAnalysisClient client = new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);
        MoodAnalysisResult result = client.analyze(List.of("url"), null);

        assertThat(result).isEqualTo(expected);
        verify(admissionController).recordSuccess();
        verify(admissionController).release();
    }

    @Test
    @DisplayName("호출이 거부되면 LlmCallRejectedException을 던진다")
    void throws_rejected_when_not_admitted() {
        given(admissionController.tryAcquire(anyLong())).willReturn(false);

        ProtectedMoodAnalysisClient client = new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);

        assertThatThrownBy(() -> client.analyze(List.of("url"), null))
                .isInstanceOf(LlmCallRejectedException.class);
        verify(delegate, never()).analyze(List.of("url"), null);
    }

    @Test
    @DisplayName("delegate가 RateLimitException을 던지면 실패를 기록하고 전파한다")
    void records_failure_and_propagates_rate_limit() {
        given(admissionController.tryAcquire(anyLong())).willReturn(true);
        given(delegate.analyze(List.of("url"), null))
                .willThrow(new RateLimitException("429"));

        ProtectedMoodAnalysisClient client = new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);

        assertThatThrownBy(() -> client.analyze(List.of("url"), null))
                .isInstanceOf(RateLimitException.class);
        verify(admissionController).recordFailure();
        verify(admissionController).recordRetryAfter(10);
        verify(admissionController).release();
    }

    @Test
    @DisplayName("delegate가 RuntimeException을 던지면 실패를 기록하고 전파한다")
    void records_failure_and_propagates_runtime_exception() {
        given(admissionController.tryAcquire(anyLong())).willReturn(true);
        given(delegate.analyze(List.of("url"), null))
                .willThrow(new IllegalStateException("LLM 분석 실패"));

        ProtectedMoodAnalysisClient client = new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);

        assertThatThrownBy(() -> client.analyze(List.of("url"), null))
                .isInstanceOf(IllegalStateException.class);
        verify(admissionController).recordFailure();
        verify(admissionController).release();
    }

    @Test
    @DisplayName("delegate 실패 시에도 permit은 반드시 반납된다")
    void release_is_always_called_on_failure() {
        given(admissionController.tryAcquire(anyLong())).willReturn(true);
        given(delegate.analyze(List.of("url"), null))
                .willThrow(new RuntimeException("네트워크 오류"));

        ProtectedMoodAnalysisClient client = new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);

        assertThatThrownBy(() -> client.analyze(List.of("url"), null))
                .isInstanceOf(RuntimeException.class);
        verify(admissionController).release();
    }
}
