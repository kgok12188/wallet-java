package com.tk.chains.exceptions;


import lombok.Getter;

public class GasException extends RuntimeException {

    @Getter
    private String chainId;
    @Getter
    private String configJson;

    public GasException(String chainId, String configJson) {
        super(configJson);
        this.configJson = configJson;
        this.chainId = chainId;
    }

}
