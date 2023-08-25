package com.hh.easypanspringboot.entity.enums;


public enum ResponseCodeEnum {
    CODE_200(200, "请求成功"),
    CODE_404(404, "请求地址不存在"),
    CODE_600(600, "请求参数错误"),
    CODE_601(601, "信息已经存在"),
    CODE_500(500, "服务器返回错误，请联系管理员"),
    CODE_901(901, "未登录，请先登录后操作"),
    CODE_902(902, "分享链接不存在"),
    CODE_903(903, "分享链接已失效"),
    CODE_904(904, "网盘空间不足，请扩容"),
    CODE_905(905, "分享验证失效，请重新验证"),
    CODE_906(906, "验证码错误");

    private Integer code;

    private String msg;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
