package com.tk.chain.sol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tk.chain.thirdPart.BlockTx;
import com.tk.chain.thirdPart.PaymentStatusEnum;
import com.tk.chain.thirdPart.Transaction;
import com.tk.chains.BlockChain;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Base58;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service("SOL")
public class SolBlockChain extends BlockChain<SolChainClient> {
    protected volatile Map<String, SymbolConfig> configHashMap = new HashMap<>();

    @Override
    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        HashMap<String, SymbolConfig> configHashMap = new HashMap<>();
        List<SymbolConfig> coinConfigs = chainScanConfig.getCoinConfigs();
        if (CollectionUtils.isNotEmpty(coinConfigs)) {
            for (SymbolConfig coinConfig : coinConfigs) {
                if (StringUtils.isNotBlank(coinConfig.getContractAddress())) {
                    configHashMap.put(coinConfig.getContractAddress().toLowerCase(), coinConfig);
                }
            }
        }
        super.freshChainScanConfig(chainScanConfig);
        this.configHashMap = configHashMap;
    }

    @Override
    protected ChainClient create(JSONObject endpoint) {
        String url = endpoint.getString("url");
        SolChainClient solChainClient = new SolChainClient(url);
        BlockChain<SolChainClient>.ChainClient chainClient = new ChainClient(url, solChainClient) {
            @Override
            public void close() {
            }
        };
        return chainClient;
    }

    @Override
    public LastBlockInfo getLastBlockInfo() throws Exception {
        BlockChain<SolChainClient>.ChainClient chainClient = getChainClient(null);
        return chainClient.getClient().getLastBlockInfo();
    }

    @Override
    public SymbolConfig getTokenConfig(String contractAddress) {
        return configHashMap.get(contractAddress.toLowerCase());
    }

    @Override
    public void checkChainTransaction(ChainTransaction chainTransaction) throws ChainParamsError {

    }

//    @Override
//    public List<ChainTransaction> queryWaitToChain(String chainId) {
//        return chainTransactionService.getTransactionByStatus(chainId, Arrays.asList(ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name(), ChainTransaction.TX_STATUS.INIT.name()));
//    }

    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) {
        BlockTx block = chainClient.getClient().getBlockTx(blockNumber.longValue());
        List<Transaction> txs = new ArrayList<>();
        block.getTxs().forEach(transaction -> {
            transaction.getActions().removeIf(action -> !addressChecker.owner(action.getFromAddress(), action.getToAddress(), getChainId(), transaction.getTxHash()));
            if (!transaction.getActions().isEmpty()) {
                transaction.setTxTime(block.getBlockTime());
                txs.add(transaction);
            }
        });
        List<ChainTransaction> chainTransactions = Lists.newArrayList();

        for (Transaction tx : txs) {
            List<ChainTransaction> transactionList = getChainTransaction(tx);
            if (CollectionUtils.isNotEmpty(transactionList)) {
                chainTransactions.addAll(transactionList);
            }
        }
        return new ScanResult(txs.size(), chainTransactions, blockNumber, new Date(block.getBlockTime() == null ? 0 : block.getBlockTime()));
    }

    public Mono<ScanResult> scanMono(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) {
        return chainClient.getClient().getBlockTx(blockNumber.longValue(), "").map(
                block -> {
                    List<Transaction> txs = Lists.newArrayList();
                    block.getTxs().forEach(transaction -> {
                        transaction.getActions().removeIf(action -> !addressChecker.owner(action.getFromAddress(), action.getToAddress(), getChainId(), transaction.getTxHash()));
                        if (!transaction.getActions().isEmpty()) {
                            transaction.setTxTime(block.getBlockTime());
                            txs.add(transaction);
                        }
                    });
                    List<ChainTransaction> chainTransactions = Lists.newArrayList();

                    for (Transaction tx : txs) {
                        List<ChainTransaction> transactionList = getChainTransaction(tx);
                        if (CollectionUtils.isNotEmpty(transactionList)) {
                            chainTransactions.addAll(transactionList);
                        }
                    }
                    return new ScanResult(txs.size(), chainTransactions, blockNumber, new Date(block.getBlockTime() == null ? 0 : block.getBlockTime()));
                }
        );
    }

    public List<ChainTransaction> getChainTransaction(Transaction transaction) {
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        if (transaction.getActions() != null && !transaction.getActions().isEmpty()) {
            transaction.getActions().forEach(action -> {
                if (StringUtils.isNotEmpty(action.getContractAddress())) {
                    if (configHashMap.containsKey(action.getContractAddress().toLowerCase())) {
                        SymbolConfig coinConfig = configHashMap.get(action.getContractAddress().toLowerCase());
                        ChainTransaction chainTransaction = getChainTransaction(transaction, action, coinConfig);
                        chainTransactions.add(chainTransaction);
                    }
                } else {
                    ChainTransaction chainTransaction = new ChainTransaction();
                    chainTransaction.setChainId(getChainId());
                    chainTransaction.setHash(transaction.getTxHash());
                    chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getTxTime() == null ? 0 : transaction.getTxTime()));
                    chainTransaction.setCtime(new Date());
                    chainTransaction.setActGas(BigDecimal.ZERO);
                    chainTransaction.setTxStatus(PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus()) ? ChainTransaction.TX_STATUS.PENDING.name() : ChainTransaction.TX_STATUS.FAIL.name());
                    chainTransaction.setFromAddress(action.getFromAddress());
                    chainTransaction.setToAddress(action.getToAddress());
                    chainTransaction.setGasAddress(action.getFromAddress());
                    chainTransaction.setAmount(action.getFromAmount().divide(BigDecimal.TEN.pow(mainCoinConfig.getSymbolPrecision()), mainCoinConfig.getSymbolPrecision(), RoundingMode.DOWN));
                    chainTransaction.setCoin(this.mainCoinConfig.getSymbol());
                    chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
                    chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
                    chainTransaction.setBlockTime(new Date(transaction.getTxTime()));
                    chainTransactions.add(chainTransaction);
                }
            });
        }
        return chainTransactions;
    }

    private ChainTransaction getChainTransaction(Transaction transaction, Transaction.Action action, SymbolConfig coinConfig) {
        ChainTransaction chainTransaction = new ChainTransaction();
        chainTransaction.setChainId(getChainId());
        chainTransaction.setHash(transaction.getTxHash());
        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getTxTime() == null ? 0 : transaction.getTxTime()));
        chainTransaction.setCtime(new Date());
        chainTransaction.setContract(action.getContractAddress());
        chainTransaction.setActGas(BigDecimal.ZERO);
        chainTransaction.setTxStatus(PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus()) ? ChainTransaction.TX_STATUS.PENDING.name() : ChainTransaction.TX_STATUS.FAIL.name());
        chainTransaction.setFromAddress(action.getFromAddress());
        chainTransaction.setGasAddress(action.getFromAddress());
        chainTransaction.setToAddress(action.getToAddress());
        chainTransaction.setAmount(action.getFromAmount().divide(BigDecimal.TEN.pow(coinConfig.getSymbolPrecision()), coinConfig.getSymbolPrecision(), RoundingMode.DOWN));
        chainTransaction.setCoin(coinConfig.getSymbol());
        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
        chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
        chainTransaction.setBlockTime(new Date(transaction.getTxTime()));
        return chainTransaction;
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        return getChainClient(null).getClient().getHeight().getBlockHeight().toString();
    }

    @Override
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        return Lists.newArrayList();
    }

    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
        Transaction transaction;
        try {
            transaction = getChainClient(null).getClient().getTx(chainTransaction.getHash());
        } catch (Exception e) {
            log.error(chainTransaction.getChainId() + "\tconfirmTransaction = " + chainTransaction.getHash(), e);
            return;
        }
        if (transaction != null && PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus())) {
            // chainTransactionService.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
        } else {
            log.error("chain = {}\tconfirmTransaction = {},\tnot found", getChainId(), chainTransaction.getHash());
            // chainTransactionService.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
        }
    }

    @Override
    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
        SymbolConfig coinConfig = configHashMap.get(tokenAddress.toLowerCase());
        return getChainClient(null).getClient().getBalance(address, tokenAddress).getBalance().divide(coinConfig.precision(), RoundingMode.DOWN);
    }

    @Override
    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
        return getChainClient(null).getClient().getBalance(address, null).getBalance().divide(this.mainCoinConfig.precision());
    }

    private BigInteger getBalanceOrigin(String address, String contract) {
        return getChainClient(null).getClient().getBalance(address, contract).getBalance().toBigInteger();
    }

    @Override
    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
        return null;
    }

    @Override
    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
        return null;
    }

    @Override
    public BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig) {
        JSONObject jsonObject = JSON.parseObject(coinConfig.getConfigJson());
        if (jsonObject.containsKey("gasLimit") && jsonObject.getBigDecimal("gasLimit").compareTo(BigDecimal.ZERO) > 0) {
            return jsonObject.getBigDecimal("gasLimit").divide(mainCoinConfig.precision(), RoundingMode.DOWN);
        }
        return new BigDecimal("5000").divide(mainCoinConfig.precision(), RoundingMode.DOWN);
    }

    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {

    }


}
