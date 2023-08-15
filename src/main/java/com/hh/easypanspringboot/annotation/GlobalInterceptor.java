package com.hh.easypanspringboot.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {

    boolean checkParams() default false;

    boolean checkLogin() default false;

    boolean checkAdmin() default false;
}
