package com.travlejava.ai.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/ai-service")
    public Mono<String> aiServiceFallback() {
        return Mono.just("AI Service is currently unavailable. Please try again later.");
    }
}
