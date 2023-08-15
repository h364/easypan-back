package com.hh.easypanspringboot.entity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {

    @Value("${spring.mail.username}")
    private String sendUsername;

    @Value("${admin.mail.username}")
    private String adminEmails;

    @Value("${project.folders}")
    private String projectFolder;

    @Value("${dev:false}")
    private Boolean dev;

    public Boolean getDev() { return dev; }

    public String getProjectFolder() {
        return projectFolder;
    }

    public String getAdminEmails() {
        return adminEmails;
    }

    public String getSendUsername() {
        return sendUsername;
    }
}
