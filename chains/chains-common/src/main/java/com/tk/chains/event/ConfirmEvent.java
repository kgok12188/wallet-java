package com.tk.chains.event;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
public class ConfirmEvent extends Event {
    private String chainId;
    private BigInteger blockHeight;


    public ConfirmEvent(String chainId, BigInteger blockHeight) {
        this.chainId = chainId;
        this.blockHeight = blockHeight;
    }

}
