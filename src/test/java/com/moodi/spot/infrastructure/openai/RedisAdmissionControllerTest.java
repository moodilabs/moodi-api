package com.moodi.spot.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("redis")
@Testcontainers
class RedisAdmissionControllerTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        String url = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
        redisClient = RedisClient.create(url);
        connection = redisClient.connect();
        commands = connection.sync();
        commands.flushall();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    @Test
    @DisplayName("동시 호출이 maxConcurrent를 초과하면 거부된다")
    void concurrent_calls_exceeding_max_are_rejected() {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 2, 5000, 20, 3, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

        assertThat(controller.tryAcquire(100)).isTrue();
        assertThat(controller.tryAcquire(100)).isTrue();

        // 3번째는 거부 (permit 2개 점유 중)
        assertThat(controller.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("permit 반납 후 다시 획득할 수 있다")
    void acquire_succeeds_after_release() {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 1, 5000, 20, 3, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();

        assertThat(controller.tryAcquire(100)).isTrue();
    }

    @Test
    @DisplayName("분당 호출 수가 초과하면 거부된다")
    void rate_limit_rejects_when_exceeded() {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 10, 5000, 3, 5, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();
        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();
        assertThat(controller.tryAcquire(100)).isTrue();
        controller.release();

        // 4번째는 거부 (RPM = 3)
        assertThat(controller.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("연속 실패가 임계값에 도달하면 Circuit이 열린다")
    void circuit_opens_after_consecutive_failures() {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 10, 5000, 20, 3, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

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
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 10, 5000, 20, 3, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

        controller.recordFailure();
        controller.recordFailure();
        controller.recordSuccess();
        controller.recordFailure();

        assertThat(controller.isCircuitOpen()).isFalse();
    }

    @Test
    @DisplayName("Retry-After cooldown 중에는 모든 인스턴스에서 호출이 거부된다")
    void retry_after_cooldown_rejects_globally() {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 10, 5000, 20, 5, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

        controller.recordRetryAfter(60);

        // cooldown이 Redis에 저장되므로 같은 Redis를 보는 새 컨트롤러에서도 거부
        RedisAdmissionController anotherInstance = new RedisAdmissionController(commands, props);
        assertThat(anotherInstance.tryAcquire(100)).isFalse();
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 acquire하면 maxConcurrent만큼만 허용된다")
    void multi_thread_concurrent_acquire() throws InterruptedException {
        LlmProtectionProperties props = new LlmProtectionProperties("redis", 2, 5000, 20, 5, 10);
        RedisAdmissionController controller = new RedisAdmissionController(commands, props);

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

        assertThat(rejected.get()).isGreaterThan(0);
        assertThat(acquired.get()).isLessThanOrEqualTo(props.maxConcurrent() + 1);
    }
}
