package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Arc20TransferModel {
    private String fromAddress;
    private String toAddress;
    private int precision;
    private String contractAddress;
    private Integer amount;
    private int satsbyte;
    private String privateKey;
    private String fundPrivateKey;
    private Boolean hotDecrypt;
}
