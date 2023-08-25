package com.hh.easypanspringboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication
@MapperScan(basePackages = "com.hh.easypanspringboot.mappers")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class EasypanSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasypanSpringbootApplication.class, args);
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

}
