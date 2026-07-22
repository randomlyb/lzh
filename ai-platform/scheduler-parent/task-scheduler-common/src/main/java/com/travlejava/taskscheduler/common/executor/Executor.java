package com.travlejava.taskscheduler.common.executor;

import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;

/**
 * Interface for task executors.
 */
public interface Executor {
    /**
     * Execute a task.
     *
     * @param param Parameters for task execution.
     * @return Result of the task execution.
     */
    Result<String> execute(TaskExecuteParam param);
}
