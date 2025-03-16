package com.tk.chains.event;


import com.tk.chains.BlockChain;
import com.tk.chains.service.BlockTransactionManager;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.ChainTransactionService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class UpdatePendingStatusTransactionChainEventListener implements ChainEventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdatePendingStatusTransactionChainEventListener.class);

    @Autowired
    private BlockTransactionManager blockTransactionManager;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private ChainTransactionService chainTransactionService;
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    protected EventManager eventManager;

    private ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void process(Event event) {
        if (event instanceof UpdatePendingStatusTransactionEvent) {
            UpdatePendingStatusTransactionEvent heightUpdateEvent = (UpdatePendingStatusTransactionEvent) event;
            ReentrantLock reentrantLock = locks.get(heightUpdateEvent.getChainId());
            if (reentrantLock == null) {
                locks.putIfAbsent(heightUpdateEvent.getChainId(), new ReentrantLock());
                reentrantLock = locks.get(heightUpdateEvent.getChainId());
            }
            if (reentrantLock.tryLock()) {
                log.info("process chainId = {},\tblockHeight = {}", heightUpdateEvent.getChainId(), heightUpdateEvent.getBlockHeight());
                try {
                    while (true) {
                        List<Long> ids = blockTransactionManager.queryPendingChainTransaction(heightUpdateEvent);
                        if (CollectionUtils.isNotEmpty(ids)) {
                            ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(heightUpdateEvent.getChainId());
                            log.info("start chainId = {},\tblockHeight = {},\tupdate size = {}", heightUpdateEvent.getChainId(), heightUpdateEvent.getBlockHeight(), ids.size());
                            for (Long id : ids) {
                                ChainTransaction chainTransaction = chainTransactionService.getById(id);
                                if (chainTransaction == null) {
                                    continue;
                                }
                                if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.PENDING.name())) {
                                    BlockChain<?> blockChain = applicationContext.getBean(heightUpdateEvent.getChainId(), BlockChain.class);
                                    // 区块确认
                                    try {
                                        blockChain.confirmTransaction(chainScanConfig, chainTransaction);
                                    } catch (Exception e) {
                                        log.warn("chainId = {},\tconfirmTransaction({}) error {}", chainTransaction.getChainId(), chainTransaction.getHash(), e.getMessage());
                                    }
                                }
                            }
                            log.info("end chainId = {},\tblockHeight = {},\tupdate size = {}", heightUpdateEvent.getChainId(), heightUpdateEvent.getBlockHeight(), ids.size());
                        } else {
                            break;
                        }
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }
        }
    }

}
