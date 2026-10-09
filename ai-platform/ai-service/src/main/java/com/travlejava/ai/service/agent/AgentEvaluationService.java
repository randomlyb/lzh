package com.travlejava.ai.service.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 离线评测。
 *
 * <p>RAG 侧的评测（Recall@5 / MRR / LLM-as-judge）已经比较完善，但 Agent 本身一直没有评测。
 * 工具调用类 Agent 的质量不能只看"答案像不像"，更要看过程：
 * <ul>
 *   <li><b>工具召回率 Recall</b>：该调的工具调了没有（expectedTools 命中比例）；</li>
 *   <li><b>工具精确率 Precision</b>：有没有调无关工具（命中数 / 实际调用数）；</li>
 *   <li><b>无效调用率</b>：调用了不在期望集合里的工具的比例；</li>
 *   <li><b>平均步数</b>：是否在预算内收敛；</li>
 *   <li><b>答案关键词命中率</b>：最终回答是否覆盖了必要的结论要素。</li>
 * </ul>
 *
 * <p>评测复用与线上完全相同的 {@link JobAnalysisAgent#runOnce}（同一套系统提示词、工具白名单、
 * 轮次预算），因此结果对线上是有意义的，而不是另建一条"评测专用链路"。
 */
@Service
public class AgentEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentEvaluationService.class);

    private final JobAnalysisAgent jobAnalysisAgent;
    private final ObjectMapper objectMapper;

    public AgentEvaluationService(JobAnalysisAgent jobAnalysisAgent, ObjectMapper objectMapper) {
        this.jobAnalysisAgent = jobAnalysisAgent;
        this.objectMapper = objectMapper;
    }

    public List<AgentTestCase> loadTestCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("agent-test-cases.json");
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<AgentTestCase>>() {});
        }
    }

    /** 逐条串行跑完全部用例，避免并发把上游 LLM 限流 */
    public Mono<EvalSummary> runAll() {
        List<AgentTestCase> cases;
        try {
            cases = loadTestCases();
        } catch (IOException e) {
            return Mono.error(e);
        }
        logger.info("Agent 评测开始，共 {} 条用例", cases.size());
        return Flux.fromIterable(cases)
                .concatMap(this::runOne)
                .collectList()
                .map(this::summarize)
                .doOnSuccess(s -> logger.info("Agent 评测完成: 成功率 {}%", s.getSuccessRate()));
    }

    public Mono<CaseResult> runOne(AgentTestCase testCase) {
        long start = System.currentTimeMillis();
        return jobAnalysisAgent.runOnce(testCase.getQuestion())
                .timeout(Duration.ofSeconds(180))
                .map(outcome -> buildResult(testCase, outcome, System.currentTimeMillis() - start))
                .onErrorResume(e -> {
                    logger.warn("用例 {} 执行失败: {}", testCase.getId(), e.getMessage());
                    return Mono.just(buildError(testCase, e, System.currentTimeMillis() - start));
                });
    }

    private CaseResult buildResult(AgentTestCase testCase, ToolCallLoop.Outcome outcome, long elapsedMs) {
        List<String> expected = testCase.getExpectedTools() == null ? List.of() : testCase.getExpectedTools();
        List<String> called = outcome.getToolTrace() == null ? List.of() : outcome.getToolTrace();

        Set<String> expectedSet = new LinkedHashSet<>(expected);
        Set<String> calledSet = new LinkedHashSet<>(called);

        long hit = calledSet.stream().filter(expectedSet::contains).count();
        double recall = expectedSet.isEmpty() ? 1.0 : (double) hit / expectedSet.size();
        double precision = calledSet.isEmpty()
                ? (expectedSet.isEmpty() ? 1.0 : 0.0)
                : (double) hit / calledSet.size();

        List<String> missing = expectedSet.stream().filter(t -> !calledSet.contains(t)).collect(Collectors.toList());
        List<String> unexpected = calledSet.stream().filter(t -> !expectedSet.contains(t)).collect(Collectors.toList());

        String answer = outcome.getAnswer() == null ? "" : outcome.getAnswer();
        List<String> keywords = testCase.getAnswerKeywords() == null ? List.of() : testCase.getAnswerKeywords();
        List<String> matched = keywords.stream().filter(answer::contains).collect(Collectors.toList());
        double keywordRatio = keywords.isEmpty() ? 1.0 : (double) matched.size() / keywords.size();
        boolean answerPass = keywordRatio >= 0.5;

        int maxSteps = testCase.getMaxSteps() == null ? 4 : testCase.getMaxSteps();
        boolean toolPass = recall >= 1.0;
        boolean stepPass = outcome.getIterations() <= maxSteps;

        CaseResult result = new CaseResult();
        result.setId(testCase.getId());
        result.setQuestion(testCase.getQuestion());
        result.setCategory(testCase.getCategory());
        result.setExpectedTools(expected);
        result.setCalledTools(called);
        result.setMissingTools(missing);
        result.setUnexpectedTools(unexpected);
        result.setSteps(outcome.getIterations());
        result.setMaxSteps(maxSteps);
        result.setBudgetExhausted(outcome.isBudgetExhausted());
        result.setElapsedMs(elapsedMs);
        result.setToolRecall(round(recall));
        result.setToolPrecision(round(precision));
        result.setKeywordRatio(round(keywordRatio));
        result.setMatchedKeywords(matched);
        result.setToolPass(toolPass);
        result.setAnswerPass(answerPass);
        result.setStepPass(stepPass);
        result.setPassed(toolPass && answerPass && stepPass);
        result.setAnswer(abbreviate(answer));
        return result;
    }

    private CaseResult buildError(AgentTestCase testCase, Throwable e, long elapsedMs) {
        CaseResult result = new CaseResult();
        result.setId(testCase.getId());
        result.setQuestion(testCase.getQuestion());
        result.setCategory(testCase.getCategory());
        result.setExpectedTools(testCase.getExpectedTools() == null ? List.of() : testCase.getExpectedTools());
        result.setCalledTools(List.of());
        result.setMissingTools(testCase.getExpectedTools());
        result.setUnexpectedTools(List.of());
        result.setSteps(0);
        result.setMaxSteps(testCase.getMaxSteps() == null ? 4 : testCase.getMaxSteps());
        result.setElapsedMs(elapsedMs);
        result.setToolRecall(0.0);
        result.setToolPrecision(0.0);
        result.setKeywordRatio(0.0);
        result.setMatchedKeywords(List.of());
        result.setPassed(false);
        result.setError(e.getMessage());
        return result;
    }

    private EvalSummary summarize(List<CaseResult> results) {
        int total = results.size();
        long passed = results.stream().filter(CaseResult::isPassed).count();
        int totalCalls = results.stream().mapToInt(r -> r.getCalledTools().size()).sum();
        int invalidCalls = results.stream().mapToInt(r -> r.getUnexpectedTools().size()).sum();
        int missingCalls = results.stream().mapToInt(r -> r.getMissingTools().size()).sum();

        EvalSummary summary = new EvalSummary();
        summary.setTotal(total);
        summary.setPassed((int) passed);
        summary.setSuccessRate(round(total == 0 ? 0 : (double) passed / total));
        summary.setAvgToolRecall(round(results.stream().mapToDouble(CaseResult::getToolRecall).average().orElse(0)));
        summary.setAvgToolPrecision(round(results.stream().mapToDouble(CaseResult::getToolPrecision).average().orElse(0)));
        summary.setAvgKeywordRatio(round(results.stream().mapToDouble(CaseResult::getKeywordRatio).average().orElse(0)));
        summary.setAvgSteps(round(results.stream().mapToInt(CaseResult::getSteps).average().orElse(0)));
        summary.setAvgElapsedMs(Math.round(results.stream().mapToLong(CaseResult::getElapsedMs).average().orElse(0)));
        summary.setTotalToolCalls(totalCalls);
        summary.setInvalidToolCalls(invalidCalls);
        summary.setInvalidCallRate(round(totalCalls == 0 ? 0 : (double) invalidCalls / totalCalls));
        summary.setMissingToolCalls(missingCalls);
        summary.setBudgetExhaustedCount((int) results.stream().filter(CaseResult::isBudgetExhausted).count());

        Map<String, CategoryStat> categories = new LinkedHashMap<>();
        for (CaseResult r : results) {
            CategoryStat stat = categories.computeIfAbsent(
                    r.getCategory() == null ? "未分类" : r.getCategory(), k -> new CategoryStat());
            stat.setTotal(stat.getTotal() + 1);
            if (r.isPassed()) {
                stat.setPassed(stat.getPassed() + 1);
            }
        }
        summary.setCategories(categories);
        summary.setResults(new ArrayList<>(results));
        return summary;
    }

    private static double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }

    @Data
    public static class CaseResult {
        private Integer id;
        private String question;
        private String category;
        private List<String> expectedTools;
        private List<String> calledTools;
        private List<String> missingTools;
        private List<String> unexpectedTools;
        private int steps;
        private int maxSteps;
        private boolean budgetExhausted;
        private long elapsedMs;
        private double toolRecall;
        private double toolPrecision;
        private double keywordRatio;
        private List<String> matchedKeywords;
        private boolean toolPass;
        private boolean answerPass;
        private boolean stepPass;
        private boolean passed;
        private String answer;
        private String error;
    }

    @Data
    public static class CategoryStat {
        private int total;
        private int passed;
    }

    @Data
    public static class EvalSummary {
        private int total;
        private int passed;
        private double successRate;
        private double avgToolRecall;
        private double avgToolPrecision;
        private double avgKeywordRatio;
        private double avgSteps;
        private long avgElapsedMs;
        private int totalToolCalls;
        private int invalidToolCalls;
        private double invalidCallRate;
        private int missingToolCalls;
        private int budgetExhaustedCount;
        private Map<String, CategoryStat> categories;
        private List<CaseResult> results;
    }
}
