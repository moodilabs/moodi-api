package com.moodi.spot.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.moodi.spot.application.LlmAdmissionController;
import com.moodi.spot.application.MoodAnalysisClient;
import com.moodi.spot.application.MoodAnalysisClient.MoodAnalysisResult;
import com.moodi.spot.application.RateLimitException;
import com.moodi.spot.support.MoodVectorFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 보호 계층 통합 테스트.
 *
 * <p>{@link LocalAdmissionController}와 {@link ProtectedMoodAnalysisClient}를
 * 실제로 조립하여 동시성 제한·Circuit Open·429 cooldown·fallback 시나리오를 검증한다.
 * Mock 없이 실제 Semaphore·Rate Limiter·Circuit Breaker가 동작한다.
 */
class LlmProtectionIntegrationTest {

    private static final MoodAnalysisResult SUCCESS_RESULT =
            new MoodAnalysisResult(MoodVectorFixture.create(), 0.8, 0.3);

    private LlmAdmissionController admissionController;

    @BeforeEach
    void setUp() {
        LlmProtectionProperties properties =
                new LlmProtectionProperties("local", 2, 5000, 20, 3, 10);
        admissionController = new LocalAdmissionController(properties);
    }

    @Test
    @DisplayName("동시 요청이 maxConcurrent를 초과하면 초과분은 거부되어 fallback 대상이 된다")
    void concurrent_requests_exceeding_limit_are_rejected() throws InterruptedException {
        SlowDelegate slowDelegate = new SlowDelegate(300);
        ProtectedMoodAnalysisClient client =
                new ProtectedMoodAnalysisClient(slowDelegate, admissionController, 100);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    client.analyze(List.of("url"), null);
                    succeeded.incrementAndGet();
                } catch (RuntimeException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // maxConcurrent=2이므로 대부분 거부
        assertThat(rejected.get()).isGreaterThan(0);
        assertThat(succeeded.get() + rejected.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("연속 실패로 Circuit이 열리면 delegate 호출 없이 즉시 거부된다")
    void circuit_open_rejects_without_calling_delegate() {
        CallCountingDelegate countingDelegate = new CallCountingDelegate();
        ProtectedMoodAnalysisClient client =
                new ProtectedMoodAnalysisClient(countingDelegate, admissionController, 10_000);

        // 3번 연속 실패로 Circuit Open
        for (int i = 0; i < 3; i++) {
            try {
                countingDelegate.setFailOnNext(true);
                client.analyze(List.of("url"), null);
            } catch (RuntimeException ignored) {
            }
        }

        assertThat(admissionController.isCircuitOpen()).isTrue();
        int callsBefore = countingDelegate.getCallCount();

        // Circuit Open 상태에서 호출 시도
        boolean rejected = false;
        try {
            client.analyze(List.of("url"), null);
        } catch (RuntimeException e) {
            rejected = true;
        }

        assertThat(rejected).isTrue();
        // delegate가 호출되지 않았어야 한다
        assertThat(countingDelegate.getCallCount()).isEqualTo(callsBefore);
    }

    @Test
    @DisplayName("429 발생 후 cooldown 동안 후속 요청이 즉시 거부된다")
    void rate_limit_triggers_cooldown_for_subsequent_requests() {
        RateLimitDelegate rateLimitDelegate = new RateLimitDelegate();
        ProtectedMoodAnalysisClient client =
                new ProtectedMoodAnalysisClient(rateLimitDelegate, admissionController, 10_000);

        // 첫 번째 호출에서 429 발생
        try {
            client.analyze(List.of("url"), null);
        } catch (RateLimitException ignored) {
        }

        // cooldown 중이므로 두 번째 호출도 즉시 거부 (delegate 호출 없이)
        boolean rejected = false;
        try {
            client.analyze(List.of("url"), null);
        } catch (RuntimeException e) {
            rejected = true;
        }

        assertThat(rejected).isTrue();
        // delegate는 첫 번째에서만 호출됨
        assertThat(rateLimitDelegate.getCallCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공한 호출은 Circuit 실패 카운터를 리셋한다")
    void success_resets_circuit_failure_counter() {
        CallCountingDelegate delegate = new CallCountingDelegate();
        ProtectedMoodAnalysisClient client =
                new ProtectedMoodAnalysisClient(delegate, admissionController, 10_000);

        // 2번 실패 (threshold=3이므로 아직 안 열림)
        for (int i = 0; i < 2; i++) {
            try {
                delegate.setFailOnNext(true);
                client.analyze(List.of("url"), null);
            } catch (RuntimeException ignored) {
            }
        }

        // 1번 성공 → 카운터 리셋
        delegate.setFailOnNext(false);
        client.analyze(List.of("url"), null);

        // 다시 2번 실패 → 리셋 덕분에 아직 threshold(3) 미달
        for (int i = 0; i < 2; i++) {
            try {
                delegate.setFailOnNext(true);
                client.analyze(List.of("url"), null);
            } catch (RuntimeException ignored) {
            }
        }

        assertThat(admissionController.isCircuitOpen()).isFalse();
    }

    @Test
    @DisplayName("분당 호출 제한을 초과하면 거부된다")
    void rate_limiter_rejects_when_per_minute_limit_exceeded() {
        LlmProtectionProperties props =
                new LlmProtectionProperties("local", 10, 5000, 3, 5, 10);
        LlmAdmissionController controller = new LocalAdmissionController(props);
        CallCountingDelegate delegate = new CallCountingDelegate();
        ProtectedMoodAnalysisClient client =
                new ProtectedMoodAnalysisClient(delegate, controller, 10_000);

        // 3번 성공 (RPM=3)
        for (int i = 0; i < 3; i++) {
            client.analyze(List.of("url"), null);
        }

        // 4번째는 거부
        boolean rejected = false;
        try {
            client.analyze(List.of("url"), null);
        } catch (RuntimeException e) {
            rejected = true;
        }

        assertThat(rejected).isTrue();
        assertThat(delegate.getCallCount()).isEqualTo(3);
    }

    // --- Test doubles ---

    /**
     * 지정된 시간 동안 블로킹하는 delegate. 동시성 제한 테스트용.
     */
    private static class SlowDelegate implements MoodAnalysisClient {
        private final long delayMillis;

        SlowDelegate(long delayMillis) {
            this.delayMillis = delayMillis;
        }

        @Override
        public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return SUCCESS_RESULT;
        }
    }

    /**
     * 호출 횟수를 세고, 실패를 제어할 수 있는 delegate.
     */
    private static class CallCountingDelegate implements MoodAnalysisClient {
        private int callCount = 0;
        private boolean failOnNext = false;

        @Override
        public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
            callCount++;
            if (failOnNext) {
                failOnNext = false;
                throw new IllegalStateException("LLM 분석 실패 (테스트)");
            }
            return SUCCESS_RESULT;
        }

        void setFailOnNext(boolean fail) {
            this.failOnNext = fail;
        }

        int getCallCount() {
            return callCount;
        }
    }

    /**
     * 항상 429를 던지는 delegate.
     */
    private static class RateLimitDelegate implements MoodAnalysisClient {
        private int callCount = 0;

        @Override
        public MoodAnalysisResult analyze(List<String> imageUrls, String overview) {
            callCount++;
            throw new RateLimitException("OpenAI API rate limit (429)");
        }

        int getCallCount() {
            return callCount;
        }
    }
}
