package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTransaction {
    private String fromAddress;
    private String toAddress;
    private int precision;
    private String contractAddress;
    private String amount;
    private String gasPrice;
    private String gasLimit;
    private String memo;
    private String fee;
    private Long nonce;
    private String privateKey;
    private String publicKey;
    private Boolean hotDecrypt;
    private SignRequestAdditional additional;
    private String recentBlockHash;
}
