package com.tk.chains.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tk.chains.BlockChain;
import com.tk.chains.event.EventManager;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.event.ConfirmEvent;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.fingerprint.CalcFingerprintService;
import com.tk.wallet.common.mapper.ChainTransactionMapper;
import com.tk.wallet.common.service.ChainTransactionService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Service
public class BlockTransactionManager {

    @Autowired
    private ChainTransactionService chainTransactionService;

    @Autowired
    private ChainTransactionMapper chainTransactionMapper;

    @Autowired
    protected EventManager eventManager;
    @Autowired
    private CalcFingerprintService calcFingerprintService;



    public boolean hasPendingChainTransaction(String chainId, @Param("blockNumber") BigInteger blockNumber) {
        List<Long> ids = chainTransactionMapper.queryPendingChainTransaction(chainId, blockNumber, 1);
        return CollectionUtils.isNotEmpty(ids);
    }

    public List<Long> queryPendingChainTransaction(ConfirmEvent event) {
        return chainTransactionMapper.queryPendingChainTransaction(event.getChainId(), event.getBlockHeight(), 100);
    }

    public void updateTxStatus(String chainId, String hash, String txStatus, String failCode, String message, Long id, boolean emit) {
        chainTransactionMapper.updateTxStatus(chainId, hash, txStatus, failCode, message);
        if (emit && id != null && id > 0) {
            eventManager.emit(new TransactionEvent(chainId, chainTransactionService.getById(id), null));
        }
    }

    public void updateTxStatus(Long id, String txStatus, String failCode, String message, BigInteger transferBlockNumber, boolean emit) {
        chainTransactionMapper.updateTxStatusById(id, txStatus, failCode, message, transferBlockNumber);
        if (emit && id != null && id > 0) {
            ChainTransaction chainTransaction = chainTransactionService.getById(id);
            eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), chainTransaction, null));
        }
    }

    public ChainTransaction getBusinessTransaction(String chainId, String hash) {
        List<ChainTransaction> transactions = chainTransactionService.lambdaQuery().eq(ChainTransaction::getHash, hash).eq(ChainTransaction::getChainId, chainId).list();
        if (CollectionUtils.isNotEmpty(transactions)) {
            for (ChainTransaction transaction : transactions) {
                if (StringUtils.isNotBlank(transaction.getBusinessId())) {
                    return transaction;
                }
            }
        }
        return null;
    }

    public List<ChainTransaction> getTransactionByStatus(String chainId, List<String> statusList) {
        return chainTransactionService.lambdaQuery().in(ChainTransaction::getTxStatus, statusList).eq(ChainTransaction::getChainId, chainId).list();
    }

    public void updateHash(Long id, String hash, String chainInfo) {
        chainTransactionMapper.updateHash(id, hash, chainInfo);
        ChainTransaction chainTransaction = chainTransactionService.getById(id);
        eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), chainTransactionService.getById(id), null));
    }

    public List<ChainTransaction> queryWaitToChain(String chainId) {
        List<Long> ids = chainTransactionMapper.queryWaitToChain(chainId);
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        for (Long id : ids) {
            chainTransactions.add(chainTransactionService.getById(id));
        }
        return chainTransactions;
    }

    public boolean prepareTransfer(Long id, BigInteger transferBlockNumber, String url) {
        return prepareTransfer(id, transferBlockNumber, url, BigInteger.ZERO);
    }

    public boolean prepareTransfer(Long id, BigInteger transferBlockNumber, String url, BigInteger nonce) {
        Integer rows = chainTransactionMapper.prepareTransfer(id, transferBlockNumber, url, nonce);
        return rows != null && Objects.equals(rows, 1);
    }

    public boolean prepareTransfer(BigInteger transferBlockNumber, String url, BigInteger nonce, List<Long> ids) {
        String idsStr = StringUtils.join(ids, ",");
        Integer rows = chainTransactionMapper.prepareTransferList(idsStr, transferBlockNumber, url, nonce);
        return rows != null && Objects.equals(rows, ids.size());
    }

    public void releaseWaitingHash(Long id) {
        chainTransactionMapper.releaseWaitingHash(id);
    }

    public void updateTooLongTimeGetHash(String chainId, Long resetToINITInterval) {
        chainTransactionMapper.updateTooLongTimeGetHash(chainId, new Date(System.currentTimeMillis() - resetToINITInterval));
    }

    public List<ChainTransaction> getByHash(String hash) {
        return chainTransactionMapper.getByHash(hash);
    }

    public void emit(Long id) {
        ChainTransaction chainTransaction = chainTransactionService.getById(id);
        eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), chainTransaction, null));
    }

}
