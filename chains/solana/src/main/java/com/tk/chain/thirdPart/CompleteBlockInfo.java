package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteBlockInfo {


    private Long blockHeight;


    private String blockHash;


    private String parentHash;

    private Long blockTime;

    private Long epoch;

    private Long chainId;

    private Boolean ignore;

    private Collection<Transaction> txs;
}
