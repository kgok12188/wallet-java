package com.tk.chain.thirdPart;

import lombok.Data;

@Data
public class RawData {
    private Long   nonce;
    private String from;
    private String to;

    private String symbol;
    private int precision;
    private String contractAddress;

    private String amount;
    private String gasPrice;
    private String gasLimit;
    private String memo;
}
