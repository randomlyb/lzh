package com.travlejava.ai.service.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Agent 离线评测入口 */
@RestController
@RequestMapping("/api/agent/eval")
public class AgentEvaluationController {

    private final AgentEvaluationService evaluationService;

    public AgentEvaluationController(AgentEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /** 查看评测用例集 */
    @GetMapping("/cases")
    public ResponseEntity<List<AgentTestCase>> listCases() throws IOException {
        return ResponseEntity.ok(evaluationService.loadTestCases());
    }

    /**
     * 跑全量评测。
     * 注意：每条用例都会真实调用若干次 LLM，整体耗时较长（分钟级）。
     */
    @PostMapping("/run")
    public ResponseEntity<AgentEvaluationService.EvalSummary> runAll() {
        return ResponseEntity.ok(evaluationService.runAll().block());
    }

    /** 跑单条用例，用于排查 */
    @PostMapping("/run-one")
    public ResponseEntity<AgentEvaluationService.CaseResult> runOne(@RequestParam Integer id) throws IOException {
        AgentTestCase testCase = evaluationService.loadTestCases().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用例不存在: " + id));
        return ResponseEntity.ok(evaluationService.runOne(testCase).block());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
