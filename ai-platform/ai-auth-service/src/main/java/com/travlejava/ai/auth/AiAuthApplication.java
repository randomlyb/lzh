package com.travlejava.ai.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.travlejava.ai.auth.mapper")
public class AiAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAuthApplication.class, args);
    }
}
