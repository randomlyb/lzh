package com.travlejava.ai.service.agent;

import com.travlejava.ai.pojo.agent.dto.AgentSummaryRequest;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agent")
public class AgentSummaryController {

    private final SummaryAgent summaryAgent;

    public AgentSummaryController(SummaryAgent summaryAgent) {
        this.summaryAgent = summaryAgent;
    }

    @PostMapping("/summary")
    public Mono<ResponseEntity<AgentSummaryResponse>> generateSummary(@RequestBody AgentSummaryRequest request) {
        return summaryAgent.execute(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    AgentSummaryResponse fallback = new AgentSummaryResponse();
                    fallback.setSummary("服务异常：" + e.getMessage());
                    fallback.setTags(java.util.List.of());
                    fallback.setCategory("unknown");
                    fallback.setStrategy("fallback");
                    fallback.setRetryCount(0);
                    fallback.setQualityScore(0);
                    return Mono.just(ResponseEntity.ok(fallback));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}