package com.tk.chain.thirdPart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;


@Data
@Builder
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    private String txHash;

    private Long blockHeight;

    private String blockHash;

    private Long txTime;

    private List<input> inputs;

    private List<Action> actions;

    private BigInteger gasPrice;

    private BigInteger gasUsed;

    private String status;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class input {

        private String txHash;
        private Integer index;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private Integer index;
        private String fromAddress;
        private String toAddress;
        private String toNewAddress;
        private String symbol;
        private BigDecimal fromAmount;
        private BigDecimal toAmount;
        private BigDecimal fee;
        private String memo;
        private String contractAddress;
        private String script;
    }
}
