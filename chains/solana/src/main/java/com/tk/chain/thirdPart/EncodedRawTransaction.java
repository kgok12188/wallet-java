package com.tk.chain.thirdPart;

import lombok.Data;

@Data
public class EncodedRawTransaction {
    private String encodedRawData;
    private String inputSignData;
}
