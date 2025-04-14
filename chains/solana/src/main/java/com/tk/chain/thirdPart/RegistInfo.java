package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistInfo {
    private String address;
    private String privateKey;
    private String contractAddress;
    private Boolean hotDecrypt;
}
