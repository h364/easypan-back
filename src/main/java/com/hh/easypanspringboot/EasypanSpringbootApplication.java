package com.hh.easypanspringboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.hh.easypanspringboot.mappers")
public class EasypanSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasypanSpringbootApplication.class, args);
    }

}
