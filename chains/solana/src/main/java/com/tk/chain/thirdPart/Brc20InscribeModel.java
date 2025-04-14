package com.tk.chain.thirdPart;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@NoArgsConstructor
@Data
public class Brc20InscribeModel {

    private List<CommitTxPrevOutput> commitTxPrevOutputList;
    private Integer commitFeeRate;
    private Integer revealFeeRate;
    private List<InscriptionData> inscriptionDataList;
    private Integer revealOutValue;
    private String changeAddress;
    private Boolean hotDecrypt;


    @NoArgsConstructor
    @Data
    public static class CommitTxPrevOutput {
        private String txId;
        private Integer vout;
        private BigDecimal amount;
        private String address;
        private String privateKey;
    }

    @NoArgsConstructor
    @Data
    public static class InscriptionData {
        private String contentType;
        private String body;
        private String revealAddr;
    }

}
