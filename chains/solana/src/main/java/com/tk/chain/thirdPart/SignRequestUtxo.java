package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignRequestUtxo {

    private String coinType;

    private String chainType;

    private String address;

    private String txHash;

    private Integer vout;

    private BigDecimal amount;

    private String scriptPubkey;

    private Long blockHeight;
}
