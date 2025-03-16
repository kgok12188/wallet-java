package com.tk.chains.event;

import com.tk.wallet.common.entity.ChainTransaction;

import java.util.List;

public class TransactionEvent extends Event {
    private String chainId;
    private ChainTransaction chainTransaction;
    private List<ChainTransaction> chainTransactions;

    public TransactionEvent(String chainId, ChainTransaction chainTransaction, List<ChainTransaction> chainTransactions) {
        this.chainId = chainId;
        this.chainTransaction = chainTransaction;
        this.chainTransactions = chainTransactions;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public ChainTransaction getChainTransaction() {
        return chainTransaction;
    }

    public void setChainTransaction(ChainTransaction chainTransaction) {
        this.chainTransaction = chainTransaction;
    }

    public List<ChainTransaction> getChainTransactions() {
        return chainTransactions;
    }

    public void setChainTransactions(List<ChainTransaction> chainTransactions) {
        this.chainTransactions = chainTransactions;
    }
}
