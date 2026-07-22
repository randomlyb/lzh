package com.travlejava.taskscheduler.service.executor;

import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("pdfParseJobExecutor") // Register with a name for dynamic lookup
public class PdfParseJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(PdfParseJobExecutor.class);

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("Executing PdfParseJobExecutor with params: {}", param);
        // Simulate PDF parsing logic
        try {
            Thread.sleep(2000); // Simulate work
            logger.info("PDF parsing for document {} completed successfully.", param.getJobParam());
            return Result.success("PDF parsing completed.");
        } catch (InterruptedException e) {
            logger.error("PdfParseJobExecutor interrupted.", e);
            Thread.currentThread().interrupt();
            return Result.fail("PDF parsing interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.error("PdfParseJobExecutor failed.", e);
            return Result.fail("PDF parsing failed: " + e.getMessage());
        }
    }
}
