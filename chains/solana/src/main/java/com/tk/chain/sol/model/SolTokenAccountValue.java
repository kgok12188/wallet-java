package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
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

    private String mint;

    private TokenAmount tokenAmount;

    @Data
    public static class  TokenAmount {
        private BigDecimal amount;
        private Integer decimals;
        private BigDecimal uiAmount;
        private String uiAmountString;
    }

}
