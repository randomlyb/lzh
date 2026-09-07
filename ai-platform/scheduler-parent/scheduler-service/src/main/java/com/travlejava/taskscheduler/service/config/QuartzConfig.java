package com.travlejava.taskscheduler.service.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class QuartzConfig {

    private final SpringJobFactory springJobFactory;

    public QuartzConfig(SpringJobFactory springJobFactory) {
        this.springJobFactory = springJobFactory;
    }

    @Bean
    public Scheduler scheduler() throws SchedulerException {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "TaskScheduler");
        props.setProperty("org.quartz.threadPool.threadCount", "20");
        props.setProperty("org.quartz.threadPool.threadPriority", "5");
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
        
        StdSchedulerFactory factory = new StdSchedulerFactory(props);
        Scheduler scheduler = factory.getScheduler();
        scheduler.setJobFactory(springJobFactory);
        scheduler.start();
        return scheduler;
    }
}