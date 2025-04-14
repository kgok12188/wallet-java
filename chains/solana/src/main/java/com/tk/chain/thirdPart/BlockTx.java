package com.tk.chain.thirdPart;

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

    private Long blockTime;

    private List<Transaction> txs;

    private List<String> failedTxs;
}
