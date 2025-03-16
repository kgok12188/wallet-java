package com.tk.chains.event;

import java.math.BigInteger;

public class UpdatePendingStatusTransactionEvent extends Event {
    private String chainId;
    private BigInteger blockHeight;


    public UpdatePendingStatusTransactionEvent(String chainId, BigInteger blockHeight) {
        this.chainId = chainId;
        this.blockHeight = blockHeight;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public BigInteger getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(BigInteger blockHeight) {
        this.blockHeight = blockHeight;
    }
}
