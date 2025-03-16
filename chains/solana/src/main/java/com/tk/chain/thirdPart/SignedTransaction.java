package com.tk.chain.thirdPart;

import lombok.Data;

@Data
public class SignedTransaction {
    private String encodedRawTransaction;
    private String publicKey;
    private String address;
}
