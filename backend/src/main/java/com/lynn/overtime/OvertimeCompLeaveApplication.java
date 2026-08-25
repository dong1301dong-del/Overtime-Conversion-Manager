package com.lynn.overtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.lynn.overtime")
@EnableJpaRepositories(basePackages = "com.lynn.overtime")
public class OvertimeCompLeaveApplication {
    public static void main(String[] args) {
        SpringApplication.run(OvertimeCompLeaveApplication.class, args);
    }
}
