package com.tk.wallet.common.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Date;

@Setter
@Getter
@Data
public class ScanChainQueue {
    private Long id;
    private String chainId;
    private BigInteger blockNumber;
    private Integer status;
    private java.util.Date ctime;
}
