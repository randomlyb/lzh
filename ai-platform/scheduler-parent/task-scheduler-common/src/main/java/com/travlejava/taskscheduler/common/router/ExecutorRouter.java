package com.travlejava.taskscheduler.common.router;

import com.travlejava.taskscheduler.common.entity.ExecutorInfo;
import com.travlejava.taskscheduler.common.entity.Job;

import java.util.List;

public interface ExecutorRouter {
    /**
     * Route a job to an executor.
     *
     * @param job The job to be routed.
     * @param availableExecutors List of currently available executors.
     * @return The selected executor, or null if no executor can be found.
     */
    ExecutorInfo route(Job job, List<ExecutorInfo> availableExecutors);
}
