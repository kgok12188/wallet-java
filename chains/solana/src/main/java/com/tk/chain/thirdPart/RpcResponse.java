package com.tk.chain.thirdPart;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@SuppressWarnings("all")
public class RpcResponse<T> implements Serializable {
    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = -1;
    private static final int IGNORABLE_CODE = -2;
    private static final String SUCCESS_MESSAGE = "success";

    private static final long serialVersionUID = 3891734167216488012L;

    private Integer code;

    private String message;

    private T data;

    public RpcResponse(Integer code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }

    public static <T> RpcResponse success(T t) {
        return new RpcResponse(SUCCESS_CODE, SUCCESS_MESSAGE, t);
    }

    public static RpcResponse error(String message) {
        return new RpcResponse(ERROR_CODE, message, null);
    }

    public boolean isSuccess() {
        return this.code == SUCCESS_CODE;
    }

    public boolean isNotSuccess() {
        return this.code != SUCCESS_CODE;
    }

    public boolean isNotIgnorable() {
        return !isIgnorable();
    }

    public boolean isIgnorable() {
        return this.code == IGNORABLE_CODE;
    }
}