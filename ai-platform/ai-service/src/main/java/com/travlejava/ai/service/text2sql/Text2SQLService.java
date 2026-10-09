package com.travlejava.ai.service.text2sql;

import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Text2SQL：自然语言 -> SQL -> 校验 -> 执行。
 *
 * 早期实现只做到「生成 + 校验」，返回的 SQL 字符串并没有真正执行，链路是断的。
 * 现在补上执行环节，并施加三重约束：
 *   1. 必须先通过 SQLSecurityValidator 的 AST 校验（只允许单条 SELECT）；
 *   2. 结果集限行（jdbcTemplate.maxRows），避免大表全量返回；
 *   3. 查询限时（jdbcTemplate.queryTimeout），避免慢查询拖垮连接池。
 */
@Service
public class Text2SQLService {

    private static final Logger logger = LoggerFactory.getLogger(Text2SQLService.class);

    private final WebClient webClient;
    private final SQLSecurityValidator securityValidator;
    private final DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.text2sql-model:deepseek-chat}")
    private String text2sqlModel;

    @Value("${text2sql.confidence-threshold:0.7}")
    private double confidenceThreshold;

    @Value("${text2sql.max-retries:3}")
    private int maxRetries;

    @Value("${text2sql.max-rows:200}")
    private int maxRows;

    @Value("${text2sql.query-timeout-seconds:10}")
    private int queryTimeoutSeconds;

    public Text2SQLService(WebClient.Builder webClientBuilder,
                           SQLSecurityValidator securityValidator,
                           DataSource dataSource) {
        this.webClient = webClientBuilder.build();
        this.securityValidator = securityValidator;
        this.dataSource = dataSource;
    }

    @PostConstruct
    void initJdbcTemplate() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setMaxRows(maxRows);
        this.jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
        logger.info("Text2SQL 执行器已初始化: maxRows={}, queryTimeout={}s", maxRows, queryTimeoutSeconds);
    }

    public SQLExecutionResult execute(String question, String schemaContext) {
        logger.info("开始Text2SQL执行: question={}", question);

        try {
            String raw = generateSQL(question, schemaContext);
            double confidence = extractConfidence(raw);
            String sql = stripMetadataComments(raw);
            logger.info("生成SQL(置信度 {}): {}", confidence, sql);

            if (confidence < confidenceThreshold) {
                logger.warn("置信度 {} 低于阈值 {}，拒绝执行", confidence, confidenceThreshold);
                return SQLExecutionResult.lowConfidence(confidence);
            }

            // 1) AST 安全校验
            securityValidator.validate(sql);

            // 2) 真正执行
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            List<String> columns = rows.isEmpty()
                    ? List.of()
                    : new ArrayList<>(rows.get(0).keySet());
            logger.info("Text2SQL 执行成功: 返回 {} 行, 列={}", rows.size(), columns);

            return SQLExecutionResult.success(sql, confidence, columns, rows);
        } catch (SecurityException e) {
            logger.error("SQL安全校验失败: {}", e.getMessage());
            return SQLExecutionResult.securityError(e.getMessage());
        } catch (Exception e) {
            logger.error("Text2SQL执行失败", e);
            return SQLExecutionResult.error(e.getMessage());
        }
    }

    private String generateSQL(String question, String schemaContext) {
        String prompt = buildSQLPrompt(question, schemaContext);
        try {
            return callLLM(prompt).block();
        } catch (Exception e) {
            logger.error("SQL生成失败", e);
            throw new RuntimeException("SQL生成失败: " + e.getMessage());
        }
    }

    private Mono<String> callLLM(String prompt) {
        DeepSeekChatRequest request = new DeepSeekChatRequest(text2sqlModel, prompt);

        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekChatResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent().trim();
                    }
                    throw new RuntimeException("LLM返回结果为空");
                });
    }

    private String buildSQLPrompt(String question, String schemaContext) {
        return String.format("""
                你是一个SQL专家。请根据用户问题和数据库schema生成SQL查询。

                【数据库Schema】
                %s

                【用户问题】
                %s

                要求：
                1. 只生成一条SELECT语句
                2. 使用标准SQL语法
                3. 在末尾添加置信度注释，格式：-- confidence: 0.85
                4. 在末尾添加引用的表名注释，格式：-- tables: table1, table2

                请直接输出SQL，不要解释。
                """, schemaContext, question);
    }

    private double extractConfidence(String sql) {
        try {
            for (String line : sql.split("\n")) {
                if (line.contains("-- confidence:")) {
                    return Double.parseDouble(line.split(":")[1].trim());
                }
            }
        } catch (Exception e) {
            logger.warn("无法提取置信度，使用默认值0.5");
        }
        return 0.5;
    }

    /** 去掉模型附加的元信息注释行，避免它们参与执行 */
    private String stripMetadataComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (t.startsWith("-- confidence:") || t.startsWith("-- tables:")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    public static class SQLExecutionResult {
        private final String sql;
        private final double confidence;
        private final String error;
        private final boolean success;
        private final List<String> columns;
        private final List<Map<String, Object>> rows;

        private SQLExecutionResult(String sql, double confidence, String error, boolean success,
                                   List<String> columns, List<Map<String, Object>> rows) {
            this.sql = sql;
            this.confidence = confidence;
            this.error = error;
            this.success = success;
            this.columns = columns;
            this.rows = rows;
        }

        public static SQLExecutionResult success(String sql, double confidence,
                                                 List<String> columns, List<Map<String, Object>> rows) {
            return new SQLExecutionResult(sql, confidence, null, true, columns, rows);
        }

        public static SQLExecutionResult lowConfidence(double confidence) {
            return new SQLExecutionResult(null, confidence, "置信度过低，需要人工确认", false, List.of(), List.of());
        }

        public static SQLExecutionResult securityError(String error) {
            return new SQLExecutionResult(null, 0, "安全校验失败: " + error, false, List.of(), List.of());
        }

        public static SQLExecutionResult error(String error) {
            return new SQLExecutionResult(null, 0, "执行失败: " + error, false, List.of(), List.of());
        }

        public String getSql() { return sql; }
        public double getConfidence() { return confidence; }
        public String getError() { return error; }
        public boolean isSuccess() { return success; }
        public List<String> getColumns() { return columns; }
        public List<Map<String, Object>> getRows() { return rows; }
        public int getRowCount() { return rows == null ? 0 : rows.size(); }

        @Override
        public String toString() {
            if (success) {
                return String.format("SQL: %s, 置信度: %.2f, 行数: %d", sql, confidence, getRowCount());
            }
            return String.format("失败: %s", error);
        }
    }
}
