package com.leeroy.forwordpanel.forwordpanel.common.enums;

/**
 * @program: forwordpanel
 * @description: 服务器状态
 * @author: liruifeng
 * @created: 2020/10/13 16:55
 */
public enum ForwardStatusEnum {
    INIT(1, "未设置"), STOP(2, "停止中"), START(3, "启动中"),
    STOPPED(4, "已停止"), STARTED(5, "已启动"),STOP_FAIL(6, "停止失败"),START_FAIL(7, "启动失败");

    ForwardStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    private int code;
    private String name;

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
