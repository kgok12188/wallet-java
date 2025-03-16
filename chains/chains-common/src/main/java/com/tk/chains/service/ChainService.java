package com.tk.chains.service;


import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;

import java.math.BigDecimal;
import java.math.BigInteger;


public interface ChainService {

    /**
     * 指定区块扫描
     *
     * @param chainId
     * @param blockNumber
     */
    void scan(String chainId, BigInteger blockNumber);

    /**
     * 新增提现
     *
     * @param chainTransaction
     * @return
     */
    Long addChainTransaction(ChainTransaction chainTransaction);

    //  Long addChainTransaction(TransferReqDto transferReqDto);


    boolean isValidTronAddress(String chainId, String address);

    /**
     * 查询转账gas
     *
     * @param coinConfig
     * @return
     */
    BigDecimal gas(SymbolConfig coinConfig);

}
