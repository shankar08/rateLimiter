package org.interview;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;


/**
 * LLM Response Streaming Manager with Circuit Breaker
 * Handles: Rate limiting, circuit breaking, concurrent streaming
 */
public class LLMStreamingManager {

    /* ---------------- Circuit Breaker ---------------- */

    private enum CircuitState { CLOSED, OPEN }

    private static class CircuitBreaker {
        private CircuitState state = CircuitState.CLOSED;
        private int failures = 0;
        private final int threshold = 5;
        private Instant lastFailure = Instant.now();

        boolean allow() {
            if (state == CircuitState.OPEN &&
                    Instant.now().minusSeconds(30).isBefore(lastFailure)) {
                return false;
            }
            state = CircuitState.CLOSED;
            return true;
        }

        void success() {
            failures = 0;
            state = CircuitState.CLOSED;
        }

        void failure() {
            failures++;
            lastFailure = Instant.now();
            if (failures >= threshold) {
                state = CircuitState.OPEN;
            }
        }
    }

    /* ---------------- Rate Limiter ---------------- */

    private static class RateLimiter {
        private final int limit;
        private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();

        RateLimiter(int limitPerMinute) {
            this.limit = limitPerMinute;
        }

        synchronized boolean allow() {
            long now = System.currentTimeMillis();
            timestamps.removeIf(t -> now - t > 60_000);

            if (timestamps.size() >= limit) return false;

            timestamps.add(now);
            return true;
        }
    }

    /* ---------------- Manager ---------------- */

    private final CircuitBreaker circuitBreaker = new CircuitBreaker();
    private final RateLimiter rateLimiter;
    private final ExecutorService executor =
            Executors.newFixedThreadPool(5);

    public LLMStreamingManager(int maxRequestsPerMinute) {
        this.rateLimiter = new RateLimiter(maxRequestsPerMinute);
    }

    public CompletableFuture<Void> stream(
            String prompt,
            Consumer<String> onChunk) {

        if (!circuitBreaker.allow())
            return CompletableFuture.failedFuture(
                    new RuntimeException("Circuit open"));

        if (!rateLimiter.allow())
            return CompletableFuture.failedFuture(
                    new RuntimeException("Rate limit exceeded"));

        return CompletableFuture.runAsync(() -> {
            try {
                simulateStream(onChunk);
                circuitBreaker.success();
            } catch (Exception e) {
                circuitBreaker.failure();
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private void simulateStream(Consumer<String> consumer)
            throws InterruptedException {

        for (String token : new String[]{"This ", "is ", "streaming"}) {
            Thread.sleep(100);
            consumer.accept(token);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}