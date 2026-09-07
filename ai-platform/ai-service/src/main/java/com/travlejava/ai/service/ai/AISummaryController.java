package com.travlejava.ai.service.ai;

import com.travlejava.ai.pojo.ai.dto.SummaryRequest;
import com.travlejava.ai.pojo.ai.dto.SummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
public class AISummaryController {

    private final AISummaryService aiSummaryService;

    public AISummaryController(AISummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping("/summary")
    public Mono<ResponseEntity<SummaryResponse>> generateSummary(@RequestBody SummaryRequest request) {
        return aiSummaryService.generateSummaryAndTags(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    SummaryResponse fallback = new SummaryResponse("服务异常：" + e.getMessage(), java.util.List.of());
                    return Mono.just(ResponseEntity.ok(fallback));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}