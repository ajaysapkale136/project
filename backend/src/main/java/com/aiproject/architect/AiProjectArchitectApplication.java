package com.aiproject.architect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiProjectArchitectApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiProjectArchitectApplication.class, args);
    }
}
