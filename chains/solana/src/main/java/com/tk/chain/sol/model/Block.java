package com.tk.chain.sol.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    private Long blockHeight;

    private String blockHash;


    private String parentHash;

    private Long blockTime;

    private List<String> transactionIds;

}
