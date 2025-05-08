package com.tk.chains.service.impl;

import com.alibaba.fastjson.JSON;
import com.tk.chains.BlockChain;
import com.tk.chains.event.EventManager;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.chains.exceptions.DuplicateBusinessIdException;
import com.tk.chains.service.ChainService;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.service.ChainTransactionService;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.SymbolConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Service
public class ChainServiceImpl implements ChainService {


    private static final Logger log = LoggerFactory.getLogger(ChainServiceImpl.class);


    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private ChainTransactionService chainTransactionService;
    @Autowired
    private SymbolConfigService symbolConfigService;

    @Autowired
    private EventManager eventManager;

    @Autowired
    @Qualifier("chainBlock")
    private ExecutorService executorService;

    /**
     * 指定区块扫描
     *
     * @param chainId
     * @param blockNumber
     */
    @Override
    public void scan(String chainId, BigInteger blockNumber) {
        if (executorService == null || eventManager == null) {
            return;
        }
        BlockChain<?> blockChain = applicationContext.getBean(chainId, BlockChain.class);
        ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(chainId);
        if (Objects.equals("1", chainScanConfig.getStatus())) {
            if (blockNumber != null && blockNumber.compareTo(new BigInteger("0")) > 0) {
                executorService.execute(
                        () -> {
                            blockChain.updateBalance(false); // 扫快的时候不更新账本
                            try {
                                blockChain.scan(chainScanConfig, blockNumber);
                            } catch (Exception e) {
                                log.error(chainId + "\tscan\t" + blockNumber, e);
                            } finally {
                                blockChain.removeUpdateBalance();
                            }
                        }
                );
            }
        }
    }

    /**
     * 新增提现
     *
     * @param chainTransaction
     * @return
     */
    @Transactional
    public Long addChainTransaction(ChainTransaction chainTransaction) {
        if (Objects.isNull(chainTransaction.getChainId()) || Objects.isNull(chainTransaction.getFromAddress())
                || Objects.isNull(chainTransaction.getToAddress()) || Objects.isNull(chainTransaction.getAmount()) || StringUtils.isBlank(chainTransaction.getBusinessId())) {
            throw new ChainParamsError("chain, businessId, amount , fromAddress or toAddress is null ");
        }
        chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.INIT.name());
        if (!applicationContext.containsBean(chainTransaction.getChainId())) {
            throw new ChainParamsError("chainId config error : " + chainTransaction.getChainId());
        }
        if (StringUtils.isBlank(chainTransaction.getBusinessId())) {
            throw new ChainParamsError("businessId config error : " + chainTransaction.getBusinessId());
        }

        Optional<ChainTransaction> optionalChainTransaction = chainTransactionService.lambdaQuery().eq(ChainTransaction::getBusinessId, chainTransaction.getBusinessId()).oneOpt();
        if (optionalChainTransaction.isPresent()) {
            ChainTransaction oldChainTransaction = optionalChainTransaction.get();
            if (StringUtils.isBlank(oldChainTransaction.getHash()) && ChainTransaction.TX_STATUS.FAIL.name().equals(oldChainTransaction.getTxStatus())) {
                log.info("删除重复交易 业务id = {},\tchainId = {},\t{}", chainTransaction.getBusinessId(), chainTransaction.getChainId(), JSON.toJSONString(oldChainTransaction));
                chainTransactionService.removeById(oldChainTransaction.getId());
            } else {
                eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), optionalChainTransaction.get(), null));
                throw new DuplicateBusinessIdException(chainTransaction.getBusinessId());
            }
        }
        BlockChain<?> blockChain = applicationContext.getBean(chainTransaction.getChainId(), BlockChain.class);
        String gasAddress = blockChain.feePayer(chainTransaction.getFromAddress());
        chainTransaction.setGasAddress(gasAddress);
        blockChain.checkChainTransaction(chainTransaction);
        chainTransactionService.save(chainTransaction);
        return chainTransaction.getId();
    }

    @Override
    public boolean isValidTronAddress(String chainId, String address) {
        BlockChain<?> blockChain = applicationContext.getBean(chainId, BlockChain.class);
        return blockChain.isValidTronAddress(address);
    }

    /**
     * 查询转账gas
     *
     * @param coinConfig
     * @return
     */
    public BigDecimal gas(SymbolConfig coinConfig) {
        ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(coinConfig.getBaseSymbol());
        if (Objects.equals("1", chainScanConfig.getStatus())) {
            BlockChain<?> blockChain = applicationContext.getBean(coinConfig.getBaseSymbol(), BlockChain.class);
            return blockChain.gas(chainScanConfig, coinConfig);
        }
        return new BigDecimal("-1");
    }

}
