package com.travlejava.taskscheduler.service.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    private final SpringJobFactory springJobFactory;

    public QuartzConfig(SpringJobFactory springJobFactory) {
        this.springJobFactory = springJobFactory;
    }

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.setJobFactory(springJobFactory);
        scheduler.start();
        return scheduler;
    }
}
