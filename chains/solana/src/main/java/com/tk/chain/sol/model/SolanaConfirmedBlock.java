package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;


@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolanaConfirmedBlock {


    @JSONField(name = "blockHeight")
    private Long blockHeight;
    @JSONField(name = "blockTime")
    private BigInteger blockTime;
    @JSONField(name = "blockhash")
    private String blockhash;
    @JSONField(name = "parentSlot")
    private Long parentSlot;
    @JSONField(name = "previousBlockhash")
    private String previousBlockhash;
    @JSONField(name = "signatures")
    private List<String> signatures;
    @JSONField(name = "transactions")
    private List<SolConfirmedTransaction> transactions;


}
