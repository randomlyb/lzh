package com.travlejava.ai.service.text2sql;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/text2sql")
public class Text2SQLController {

    private final Text2SQLService text2sqlService;
    private final SQLSecurityValidator securityValidator;

    public Text2SQLController(Text2SQLService text2sqlService,
                              SQLSecurityValidator securityValidator) {
        this.text2sqlService = text2sqlService;
        this.securityValidator = securityValidator;
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(
            @RequestParam String question,
            @RequestParam(required = false) String schema) {
        
        String schemaContext = schema != null ? schema : getDefaultSchema();
        Text2SQLService.SQLExecutionResult result = text2sqlService.execute(question, schemaContext);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("sql", result.getSql());
        response.put("confidence", result.getConfidence());
        response.put("error", result.getError());
        response.put("columns", result.getColumns());
        response.put("rowCount", result.getRowCount());
        response.put("rows", result.getRows());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String sql) {
        try {
            securityValidator.validate(sql);
            return ResponseEntity.ok(Map.of("valid", true, "message", "SQL安全校验通过"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    private String getDefaultSchema() {
        return """
                表名: tasks
                字段: id (BIGINT), job_name (VARCHAR), status (VARCHAR), create_time (DATETIME), update_time (DATETIME)
                说明: 任务调度表，记录任务执行状态
                
                表名: documents
                字段: id (BIGINT), title (VARCHAR), content (TEXT), type (VARCHAR), create_time (DATETIME)
                说明: 文档表，存储知识库文档
                """;
    }
}