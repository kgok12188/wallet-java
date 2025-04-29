package com.tk.chain.sol.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecentBlockHash {

    @Getter
    @ToString
    public static class FeeCalculator {

        @JSONField(name = "lamportsPerSignature")
        private long lamportsPerSignature;
    }

    @Getter
    @ToString
    public static class Value {
        @JSONField(name = "blockhash")
        private String blockhash;

        @JSONField(name = "feeCalculator")
        private FeeCalculator feeCalculator;
    }

    private Value value;
}
