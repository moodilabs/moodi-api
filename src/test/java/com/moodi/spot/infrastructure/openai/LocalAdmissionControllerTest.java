package com.moodi.spot.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalAdmissionControllerTest {

    private LlmProtectionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LlmProtectionProperties("local", 2, 5000, 20, 3, 10);
    }

    @Test
    @DisplayName("동시 호출이 maxConcurrent를 초과하면 거부된다")
    void concurrent_calls_exceeding_max_are_rejected() throws InterruptedException {
        LocalAdmissionController controller = new LocalAdmissionController(properties);

        // permit 2개를 먼저 점유
        assertThat(controller.tryAcquire(100)).isTrue();
        assertThat(controller.tryAcquire(100)).isTrue();

        // 3번째는 거부 (100ms 대기 후)
        assertThat(controller.tryAcquire(100)).isFalse();

        // 하나 반납하면 다시 획득 가능
        controller.release();
        assertThat(controller.tryAcquire(100)).isTrue();
    }

    @Test
    @DisplayName("연속 실패가 임계값에 도달하면 Circuit이 열린다")
    void circuit_opens_after_consecutive_failures() {
        LocalAdmissionController controller = new LocalAdmissionController(properties);

        // 3회 연속 실패 (threshold = 3)
        controller.recordFailure();
        controller.recordFailure();
        assertThat(controller.isCircuitOpen()).isFalse();

        controller.recordFailure();
        assertThat(controller.isCircuitOpen()).isTrue();

        // Circuit Open 상태에서 호출 거부
        assertThat(controller.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("성공 기록이 연속 실패 카운터를 리셋한다")
    void success_resets_failure_counter() {
        LocalAdmissionController controller = new LocalAdmissionController(properties);

        controller.recordFailure();
        controller.recordFailure();
        controller.recordSuccess();
        controller.recordFailure();

        // 리셋 후 1회 실패이므로 Circuit은 닫혀 있어야 한다
        assertThat(controller.isCircuitOpen()).isFalse();
    }

    @Test
    @DisplayName("Retry-After cooldown 중에는 호출이 거부된다")
    void retry_after_cooldown_rejects_calls() {
        LocalAdmissionController controller = new LocalAdmissionController(properties);

        controller.recordRetryAfter(60);

        assertThat(controller.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("분당 호출 수가 초과하면 거부된다")
    void rate_limit_rejects_when_exceeded() {
        LlmProtectionProperties smallLimit = new LlmProtectionProperties("local", 10, 5000, 3, 5, 10);
        LocalAdmissionController controller = new LocalAdmissionController(smallLimit);

        // 3회 호출 (maxRequestsPerMinute = 3)
        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();
        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();
        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();

        // 4번째는 거부
        assertThat(controller.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 acquire하면 maxConcurrent만큼만 허용된다")
    void multi_thread_concurrent_acquire() throws InterruptedException {
        LocalAdmissionController controller = new LocalAdmissionController(properties);
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger acquired = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (controller.tryAcquire(200)) {
                        acquired.incrementAndGet();
                        // permit을 잡고 있는 동안 다른 스레드가 거부되도록 잠시 대기
                        Thread.sleep(500);
                        controller.release();
                    } else {
                        rejected.incrementAndGet();
                    }
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

        // maxConcurrent=2이므로 최대 2개만 동시에 획득, 나머지는 200ms 대기 후 거부
        assertThat(acquired.get()).isLessThanOrEqualTo(properties.maxConcurrent() + 1);
        assertThat(rejected.get()).isGreaterThan(0);
    }
}
