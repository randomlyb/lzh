package com.travlejava.taskscheduler.service.router;

import com.travlejava.taskscheduler.common.entity.ExecutorInfo;
import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.common.router.ExecutorRouter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinExecutorRouter implements ExecutorRouter {

    private final ConcurrentHashMap<Long, AtomicInteger> jobLastExecutorIndex = new ConcurrentHashMap<>();

    @Override
    public ExecutorInfo route(Job job, List<ExecutorInfo> availableExecutors) {
        if (availableExecutors == null || availableExecutors.isEmpty()) {
            return null;
        }

        AtomicInteger index = jobLastExecutorIndex.computeIfAbsent(job.getId(), k -> new AtomicInteger(-1));
        int newIndex = index.incrementAndGet();
        if (newIndex >= availableExecutors.size()) {
            newIndex = 0;
            index.set(0);
        }
        return availableExecutors.get(newIndex);
    }
}
