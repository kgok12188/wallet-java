package com.tk.chain.sol.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockTx {


    private Long blockHeight;

    private String blockHash;

    private String parentHash;

    // 秒
    private Long blockTime;

    private List<Transaction> txs;

    private List<String> failedTxs;
}
