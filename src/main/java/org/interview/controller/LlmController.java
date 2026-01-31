package org.interview.controller;

import org.interview.request.PromptRequest;
import org.interview.service.LlmStreamingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmStreamingService streamingService;

    public LlmController(LlmStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    //flux stream flow
    @GetMapping(
            value = "/streamflux",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> fluxstream(@RequestParam("prompt") String prompt) {
        return streamingService.fluxstream(prompt);
    }

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public ResponseBodyEmitter stream(@RequestBody PromptRequest request) {

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L); // no timeout

        streamingService.stream(
                request.prompt(),
                chunk -> {
                    try {
                        emitter.send(chunk);
                        System.out.println(chunk);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
        ).whenComplete((r, ex) -> emitter.complete());

        return emitter;
    }
}