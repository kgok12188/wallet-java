package com.tk.chains.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tk.chains.BlockChain;
import com.tk.chains.event.EventManager;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.event.UpdatePendingStatusTransactionEvent;
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

    @Transactional
    public void saveBlock(ChainScanConfig chainScanConfig, BlockChain.ScanResult scanResult, boolean merge, BlockChain<?> blockChain) throws Exception {
        if (merge) {
            blockChain.beforeSaveChainTransactions(chainScanConfig, chainScanConfig.getChainId(), scanResult.getBlockNumber(), scanResult.getBlockTime(), scanResult.getChainTransactions());
            HashMap<String, List<ChainTransaction>> hash2ChainTransactions = new HashMap<>();
            for (ChainTransaction chainTransaction : scanResult.getChainTransactions()) {
                List<ChainTransaction> transactions = hash2ChainTransactions.getOrDefault(chainTransaction.getFromAddress(), new ArrayList<>());
                transactions.add(chainTransaction);
                hash2ChainTransactions.put(chainTransaction.getFromAddress(), transactions);
            }
            for (Map.Entry<String, List<ChainTransaction>> kv : hash2ChainTransactions.entrySet()) {
                txSaveOrUpdate(kv.getValue());
            }
        } else {
            blockChain.beforeSaveChainTransactions(chainScanConfig, chainScanConfig.getChainId(), scanResult.getBlockNumber(), scanResult.getBlockTime(), scanResult.getChainTransactions());
            for (ChainTransaction chainTransaction : scanResult.getChainTransactions()) {
                txSaveOrUpdate(chainTransaction);
            }
        }
    }

    /**
     * 链上区块扫到后
     * 1、与体现交易合并
     * 2、充值直接保存
     *
     * @param chainTransaction
     */
    public void txSaveOrUpdate(ChainTransaction chainTransaction) {
        LambdaQueryWrapper<ChainTransaction> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ChainTransaction::getHash, chainTransaction.getHash());
        lqw.eq(ChainTransaction::getChainId, chainTransaction.getChainId());
        ChainTransaction transaction = chainTransactionService.getOne(lqw);
        if (transaction == null) {
            lqw = new LambdaQueryWrapper<>();
            lqw.eq(ChainTransaction::getChainId, chainTransaction.getChainId());
            lqw.eq(ChainTransaction::getFromAddress, chainTransaction.getFromAddress());
            lqw.eq(ChainTransaction::getNonce, chainTransaction.getNonce());
            transaction = chainTransactionService.getOne(lqw);
            if (transaction != null) { // 上链成功，没有同步hash
                ChainTransaction updateChainTransaction = new ChainTransaction();
                updateChainTransaction.setId(transaction.getId());
                updateChainTransaction.setHash(chainTransaction.getHash());
                chainTransactionService.updateById(updateChainTransaction);
            }
        }
        if (transaction == null) {
            chainTransactionService.save(chainTransaction);
            calcFingerprintService.calcFingerprint(chainTransaction, chainTransactionService, new ChainTransaction());
            if (eventManager != null) {
                eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), chainTransaction, null));
            }
        } else {
            if (StringUtils.isNotBlank(transaction.getBusinessId())) {
                chainTransactionMapper.mergeChainTransaction(chainTransaction);
            }
            if (eventManager != null) {
                eventManager.emit(new TransactionEvent(chainTransaction.getChainId(), chainTransactionService.getOne(lqw), null));
            }
        }
    }

    public void txSaveOrUpdate(List<ChainTransaction> chainTransactions) {
        LambdaQueryWrapper<ChainTransaction> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ChainTransaction::getHash, chainTransactions.get(0).getHash());
        lqw.eq(ChainTransaction::getChainId, chainTransactions.get(0).getChainId());
        ChainTransaction businessTransaction = getBusinessTransaction(chainTransactions.get(0).getChainId(), chainTransactions.get(0).getHash());
        List<ChainTransaction> list = chainTransactionService.list(lqw);
        // 提现交易，链上交易合并
        if (businessTransaction != null) {
            HashMap<String, ChainTransaction> removeList = new HashMap<>();
            for (ChainTransaction chainTransaction : list) {
                removeList.put(chainTransaction.getFromAddress() + "-" + chainTransaction.getToAddress(), chainTransaction);
                for (ChainTransaction transaction : chainTransactions) {
                    if (StringUtils.equals(chainTransaction.getFromAddress() + "-" + chainTransaction.getToAddress(),
                            transaction.getFromAddress() + "-" + transaction.getToAddress())) {
                        transaction.setBusinessId(chainTransaction.getBusinessId());
                    }
                }
            }
            LinkedList<ChainTransaction> chainTransactionLinkedList = new LinkedList<>(chainTransactions);
            Iterator<ChainTransaction> iterator = chainTransactionLinkedList.iterator();
            while (iterator.hasNext()) {
                ChainTransaction chainTransaction = iterator.next();
                String key = chainTransaction.getFromAddress() + "-" + chainTransaction.getToAddress();
                if (removeList.containsKey(key)) {
                    ChainTransaction transaction = removeList.get(key);
                    chainTransaction.setId(transaction.getId());
                    iterator.remove();
                }
            }
            if (CollectionUtils.isNotEmpty(chainTransactionLinkedList)) {
                for (ChainTransaction chainTransaction : chainTransactionLinkedList) {
                    chainTransaction.setGas(BigDecimal.ZERO);
                    chainTransaction.setActGas(BigDecimal.ZERO);
                }
                chainTransactionService.saveBatch(chainTransactionLinkedList);
            }
            chainTransactionMapper.updateBlockNum(chainTransactions.get(0).getChainId(), chainTransactions.get(0).getHash(), chainTransactions.get(0).getBlockNum());
            chainTransactionMapper.updateGas(chainTransactions.get(0));
            chainTransactionMapper.updateTxStatus(chainTransactions.get(0).getChainId(), chainTransactions.get(0).getHash(), chainTransactions.get(0).getTxStatus(), chainTransactions.get(0).getFailCode(), chainTransactions.get(0).getMessage());
            if (eventManager != null) {
                eventManager.emit(new TransactionEvent(chainTransactions.get(0).getChainId(), null, chainTransactionService.list(lqw)));
            }
        } else if (CollectionUtils.isEmpty(list)) {
            chainTransactionService.saveBatch(chainTransactions);
        }
    }

    public boolean hasPendingChainTransaction(String chainId, @Param("blockNumber") BigInteger blockNumber) {
        List<Long> ids = chainTransactionMapper.queryPendingChainTransaction(chainId, blockNumber, 1);
        return CollectionUtils.isNotEmpty(ids);
    }

    public List<Long> queryPendingChainTransaction(UpdatePendingStatusTransactionEvent event) {
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
        List<ChainTransaction> transactions = chainTransactionService.lambdaQuery()
                .eq(ChainTransaction::getHash, hash)
                .eq(ChainTransaction::getChainId, chainId).list();
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
        return chainTransactionService.lambdaQuery()
                .in(ChainTransaction::getTxStatus, statusList)
                .eq(ChainTransaction::getChainId, chainId).list();
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

    public void releaseWaitingHash(Long id) {
        chainTransactionMapper.releaseWaitingHash(id);
    }

    public void updateTooLongTimeGetHash(String chainId, Long resetToINITInterval) {
        chainTransactionMapper.updateTooLongTimeGetHash(chainId, new Date(System.currentTimeMillis() - resetToINITInterval));
    }

    public List<ChainTransaction> getByHash(String hash) {
        return chainTransactionMapper.getByHash(hash);
    }
}
