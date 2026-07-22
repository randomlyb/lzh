package com.travlejava.taskscheduler.service.router;

import com.travlejava.taskscheduler.common.entity.ExecutorInfo;
import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.common.enums.RouteStrategy;
import com.travlejava.taskscheduler.common.router.ExecutorRouter;
import com.travlejava.taskscheduler.service.registry.ExecutorRegistryService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutorRouteService {

    private final Map<RouteStrategy, ExecutorRouter> routerMap = new HashMap<>();
    private final ExecutorRegistryService executorRegistryService;

    public ExecutorRouteService(RandomExecutorRouter randomExecutorRouter, 
                                RoundRobinExecutorRouter roundRobinExecutorRouter,
                                ExecutorRegistryService executorRegistryService) {
        routerMap.put(RouteStrategy.RANDOM, randomExecutorRouter);
        routerMap.put(RouteStrategy.ROUND_ROBIN, roundRobinExecutorRouter);
        this.executorRegistryService = executorRegistryService;
    }

    public ExecutorInfo routeExecutor(Job job) {
        List<ExecutorInfo> availableExecutors = executorRegistryService.getAvailableExecutors();
        if (availableExecutors.isEmpty()) {
            throw new RuntimeException("No available executors for job: " + job.getJobName());
        }

        ExecutorRouter router = routerMap.get(job.getRouteStrategy());
        if (router == null) {
            throw new IllegalArgumentException("Unknown route strategy: " + job.getRouteStrategy());
        }

        return router.route(job, availableExecutors);
    }
}
