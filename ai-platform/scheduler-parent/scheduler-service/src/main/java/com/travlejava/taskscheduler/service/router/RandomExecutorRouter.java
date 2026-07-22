package com.travlejava.taskscheduler.service.router;

import com.travlejava.taskscheduler.common.entity.ExecutorInfo;
import com.travlejava.taskscheduler.common.entity.Job;
import com.travlejava.taskscheduler.common.router.ExecutorRouter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class RandomExecutorRouter implements ExecutorRouter {

    private final Random random = new Random();

    @Override
    public ExecutorInfo route(Job job, List<ExecutorInfo> availableExecutors) {
        if (availableExecutors == null || availableExecutors.isEmpty()) {
            return null;
        }
        return availableExecutors.get(random.nextInt(availableExecutors.size()));
    }
}
