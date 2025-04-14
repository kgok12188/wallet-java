package com.tk.chain.thirdPart;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@NoArgsConstructor
@Data
public class Brc20TransferModel {

    private List<TxInput> txInputs;
    private List<TxOutput> txOutputs;
    private Boolean hotDecrypt;


    @NoArgsConstructor
    @Data
    public static class TxInput {
        private String txId;
        private Integer vout;
        private BigDecimal amount;
        private String address;
        private String privateKey;
        private String nonWitnessUtxo;
    }

    @NoArgsConstructor
    @Data
    public static class TxOutput {
        private String address;
        private BigDecimal amount;
    }
}
