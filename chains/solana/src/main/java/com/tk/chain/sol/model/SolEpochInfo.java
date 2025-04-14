package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;


@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolEpochInfo {

    @JSONField(name = "absoluteSlot")
    private BigInteger absoluteSlot;
    @JSONField(name = "blockHeight")
    private BigInteger blockHeight;
    @JSONField(name = "epoch")
    private BigInteger epoch;
    @JSONField(name = "slotIndex")
    private BigInteger slotIndex;
    @JSONField(name = "slotsInEpoch")
    private BigInteger slotsInEpoch;
    @JSONField(name = "transactionCount")
    private BigInteger transactionCount;
}