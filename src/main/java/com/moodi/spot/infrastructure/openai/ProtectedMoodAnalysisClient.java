package com.moodi.spot.infrastructure.openai;

import java.util.List;

import com.moodi.shared.mood.MoodVector;
import com.moodi.spot.application.LlmAdmissionController;
import com.moodi.spot.application.MoodAnalysisClient;
import com.moodi.spot.application.RateLimitException;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link MoodAnalysisClient} Decorator.
 *
 * <p>{@link LlmAdmissionController}로 동시성·호출량·Circuit을 제어한 뒤
 * 실제 클라이언트에 위임한다. 호출이 거부되거나 실패하면 {@link RuntimeException}을
 * 그대로 전파하여 상위({@code PickService})가 fallback으로 분기하게 한다.
 */
@Slf4j
public class ProtectedMoodAnalysisClient implements MoodAnalysisClient {

    private final MoodAnalysisClient delegate;
    private final LlmAdmissionController admissionController;
    private final long acquireTimeoutMillis;

    public ProtectedMoodAnalysisClient(MoodAnalysisClient delegate,
                                       LlmAdmissionController admissionController,
                                       long acquireTimeoutMillis) {
        this.delegate = delegate;
        this.admissionController = admissionController;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
    }

    @Override
    public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
        if (!admissionController.tryAcquire(acquireTimeoutMillis)) {
            throw new LlmCallRejectedException("LLM 호출 거부: 동시성 제한 또는 Circuit Open");
        }

        try {
            MoodAnalysisResult result = delegate.analyze(imageUrls, overview);
            admissionController.recordSuccess();
            return result;
        } catch (RateLimitException e) {
            admissionController.recordFailure();
            parseAndRecordRetryAfter(e);
            throw e;
        } catch (RuntimeException e) {
            admissionController.recordFailure();
            throw e;
        } finally {
            admissionController.release();
        }
    }

    @Override
    public int getRetryCount() {
        return delegate.getRetryCount();
    }

    @Override
    public int getRateLimitCount() {
        return delegate.getRateLimitCount();
    }

    @Override
    public void resetCounters() {
        delegate.resetCounters();
    }

    private void parseAndRecordRetryAfter(RateLimitException e) {
        // RateLimitException 메시지에서 Retry-After 추출은 현재 불가 (헤더 접근 불가).
        // 기본 cooldown 적용.
        admissionController.recordRetryAfter(10);
    }

    /**
     * 보호 계층이 호출을 거부했을 때 던지는 예외.
     * {@code PickService.analyze()}의 {@code catch (RuntimeException)}에 잡혀 fallback으로 분기한다.
     */
    public static class LlmCallRejectedException extends RuntimeException {
        public LlmCallRejectedException(String message) {
            super(message);
        }
    }
}
