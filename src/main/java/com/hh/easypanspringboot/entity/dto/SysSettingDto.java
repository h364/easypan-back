package com.hh.easypanspringboot.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {

    private String registerMailTile = "邮箱验证码";
    private String registerMailContent = "您好，您的邮箱验证码是：%s，15分钟内有效";

    private Integer userInitUseSpace = 5;

    public String getRegisterMailTile() {
        return registerMailTile;
    }

    public String getRegisterMailContent() {
        return registerMailContent;
    }

    public Integer getUserInitUseSpace() {
        return userInitUseSpace;
    }
}
