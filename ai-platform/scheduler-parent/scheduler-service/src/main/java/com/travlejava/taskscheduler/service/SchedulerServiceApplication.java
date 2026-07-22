package com.travlejava.taskscheduler.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.travlejava.taskscheduler.service.mapper")
@ComponentScan(basePackages = {"com.travlejava.taskscheduler", "com.travlejava.taskscheduler.service"})
public class SchedulerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }
}
