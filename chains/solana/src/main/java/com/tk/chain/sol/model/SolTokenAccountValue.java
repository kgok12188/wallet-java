package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolTokenAccountValue {

    @JSONField(name = "data")
    private SolTokenAccountValueData data;

    @JSONField(name = "executable")
    private Boolean executable;

    @JSONField(name = "lamports")
    private BigInteger lamports;

    @JSONField(name = "owner")
    private String owner;

    @JSONField(name = "rentEpoch")
    private BigInteger rentEpoch;
}
