package com.tk.chains.event;

import com.tk.wallet.common.entity.ChainTransaction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TransactionEvent extends Event {
    private String chainId;
    private ChainTransaction chainTransaction;
    private List<ChainTransaction> chainTransactions;

    public TransactionEvent(String chainId, ChainTransaction chainTransaction, List<ChainTransaction> chainTransactions) {
        this.chainId = chainId;
        this.chainTransaction = chainTransaction;
        this.chainTransactions = chainTransactions;
    }

}
