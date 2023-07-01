package com.hh.easypanspringboot.entity.enums;


public enum FileFolderTypeEnum {
    FILE(0, "文件"),
    FOLDER(1, "目录");

    private Integer type;
    private String desc;

    FileFolderTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
