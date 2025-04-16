package com.tk.chains.event;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
public class UpdatePendingStatusTransactionEvent extends Event {
    private String chainId;
    private BigInteger blockHeight;


    public UpdatePendingStatusTransactionEvent(String chainId, BigInteger blockHeight) {
        this.chainId = chainId;
        this.blockHeight = blockHeight;
    }

}
