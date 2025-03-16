package com.tk.wallet.common.vo;

public class R<T> {
    private int code;
    private String msg;
    private T data;

    public R() {

    }

    public static <T> R<T> success(T data) {
        R<T> r = new R<T>();
        r.setCode(0);
        r.setData(data);
        return r;
    }

    public static <T> R<T> success() {
        R<T> r = new R<T>();
        r.setCode(0);
        r.setData(null);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<T>();
        r.setCode(1);
        r.setMsg(msg);
        return r;
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
