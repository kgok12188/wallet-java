package com.tk.chains.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tk.wallet.common.entity.AggTask;

import java.util.List;


public interface AggTaskService extends IService<AggTask> {
    /**
     * 归集
     *
     * @param walletId
     * @param chainId
     * @param gasAddress    燃料地址, 假设燃料地址可以支付足够的地址，确认gas 会导致归集暂停
     * @param targetAddress 归集目标地址
     */
    void agg(Integer walletId, String chainId, String gasAddress, String targetAddress);

    /**
     * 归集入口
     *
     * @param walletId
     * @param chainId
     * @param gasAddress    燃料地址, 假设燃料地址可以支付足够的地址，确认gas 会导致归集暂停
     * @param targetAddress 归集目标地址
     * @param inContracts   需要归集的合约
     * @param addresses     需要归集的地址
     */
    void agg(Integer walletId, String chainId, String gasAddress, String targetAddress, List<String> inContracts, List<String> addresses);

}
