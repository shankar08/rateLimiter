package org.interview.service;

import org.interview.client.LlmClient;
import org.interview.resilience.CircuitBreaker;
import org.interview.resilience.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class LlmStreamingService {

    private static final Logger log =
            LoggerFactory.getLogger(LlmStreamingService.class);

    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final LlmClient llmClient;

    public LlmStreamingService(CircuitBreaker circuitBreaker, RateLimiter rateLimiter, LlmClient llmClient) {
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        this.llmClient = llmClient;
    }

    //streaming using flux
    public Flux<String> fluxstream(String prompt) {

        List<String> tokens = List.of(
                "This ",
                "is ",
                "a ",
                "streaming ",
                "response"
        );

        return Flux.fromIterable(tokens)
                .delayElements(Duration.ofMillis(100))
                .doOnSubscribe(s -> log.info("Starting stream for prompt: {}", prompt))
                .doOnComplete(() -> log.info("Stream completed"));
    }

    //mcv way
    @Async
    public CompletableFuture<Void> stream(String prompt, Consumer<String> chunkConsumer){

        if(!circuitBreaker.allowRequest()){
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Circuit Break is Open"));
        }

        if(!rateLimiter.allowRequest()){
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Rate Limit Exceeded"));

        }
        try{
            llmClient.stream(prompt, chunkConsumer);
            circuitBreaker.recordSuccess();
            return CompletableFuture.completedFuture(null);
        }catch (InterruptedException e) {
            circuitBreaker.recordFailure();
            return CompletableFuture.failedFuture(e);
        }

    }
}
