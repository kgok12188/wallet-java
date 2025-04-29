package com.tk.chain.sol.model;

public enum PaymentStatusEnum {

    CONFIRMING("CONFIRMING", "确认中, 需要客户端根据当前高度进行判断"),

    CHECKING("CHECKING", "检查中, 需要二次验证做持续检查"),

    CONFIRMED("CONFIRMED", "到达确认数"),

    FAIL("FAIL", "失败");

    private final String code;
    private final String desc;

    PaymentStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


}
