package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NftDepositTransaction {

    private String txHash;

    private Long blockHeight;

    private String blockHash;

    private Long txTime;

    private String fromUser;

    private String toUser;

    private String nftAddress;

    private String tokenId;

    private String status;

}
