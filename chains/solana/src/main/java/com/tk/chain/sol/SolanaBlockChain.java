package com.tk.chain.sol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tk.chain.sol.model.AddressBalance;
import com.tk.chain.sol.model.BlockTx;
import com.tk.chain.sol.model.PaymentStatusEnum;
import com.tk.chain.sol.model.Transaction;
import com.tk.chains.BlockChain;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.CoinBalance;
import com.tk.wallet.common.entity.SymbolConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

@Service("SOL")
public class SolanaBlockChain extends BlockChain<SolanaRpcClient> {

    private volatile Map<String, SymbolConfig> configHashMap = new HashMap<>();


    @Override
    public void checkChainTransaction(ChainTransaction chainTransaction) throws ChainParamsError {
        String gasConfig = chainTransaction.getGasConfig();
        if (StringUtils.isBlank(gasConfig)) {
            throw new ChainParamsError("gasConfig error : " + gasConfig);
        }
        String gas = JSON.parseObject(gasConfig).getString("gas");
        try {
            if (new BigDecimal(gas).compareTo(BigDecimal.ZERO) <= 0) {
                throw new ChainParamsError("gasConfig error : " + gasConfig);
            }
        } catch (Exception e) {
            throw new ChainParamsError("gasConfig error : " + gasConfig);
        }
    }

    public void beforeSaveChainTransactions(ChainScanConfig chainScanConfig, String chainId, BigInteger blockHeight, Date blockTime, List<ChainTransaction> chainTransactions) {

    }

    @Override
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, BlockChain<SolanaRpcClient>.ChainClient chainClient) {
        BlockTx block = chainClient.getClient().getBlockTx(blockNumber.longValue());
        ArrayList<ChainTransaction> chainTransactions = new ArrayList<>();
        for (Transaction tx : block.getTxs()) {
            for (Transaction.Action action : tx.getActions()) {
                if (addressChecker.owner(action.getFromAddress(), action.getToAddress(), this.chainId, tx.getTxHash())) {
                    chainTransactions.addAll(getChainTransaction(tx, chainClient, new Date(block.getBlockTime())));
                }
            }
        }
        return new ScanResult(block.getTxs().size(), chainTransactions, blockNumber, new Date(block.getBlockTime()));
    }

    private List<ChainTransaction> getChainTransaction(Transaction transaction, BlockChain<SolanaRpcClient>.ChainClient chainClient, Date blockTime) {
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        if (transaction.getActions() != null && !transaction.getActions().isEmpty()) {
            transaction.getActions().forEach(action -> {
                if (StringUtils.isNotEmpty(action.getContractAddress())) {
                    if (configHashMap.containsKey(action.getContractAddress())) {
                        SymbolConfig symbolConfig = configHashMap.get(action.getContractAddress());
                        ChainTransaction chainTransaction = getChainTransaction(transaction, chainClient, action, symbolConfig, blockTime);
                        chainTransaction.setContract(action.getContractAddress());
                        chainTransactions.add(chainTransaction);
                    }
                } else {
                    ChainTransaction chainTransaction = getChainTransaction(transaction, chainClient, action, this.mainCoinConfig, blockTime);
                    chainTransactions.add(chainTransaction);
                }
            });
        }
        return chainTransactions;
    }

    private ChainTransaction getChainTransaction(Transaction transaction, BlockChain<SolanaRpcClient>.ChainClient chainClient,
                                                 Transaction.Action action, SymbolConfig symbolConfig, Date blockTime) {
        BigDecimal amount = action.getFromAmount().divide(symbolConfig.precision(), symbolConfig.getSymbolPrecision(), RoundingMode.DOWN);
        ChainTransaction chainTransaction = new ChainTransaction();
        chainTransaction.setChainId(getChainId());
        chainTransaction.setHash(transaction.getTxHash());
        chainTransaction.setUrlCode(chainClient.getUrl());
        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getTxTime() == null ? 0 : transaction.getTxTime()));
        chainTransaction.setCtime(new Date());
        chainTransaction.setMtime(new Date());
        chainTransaction.setTxStatus(PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus()) ? ChainTransaction.TX_STATUS.PENDING.name() : ChainTransaction.TX_STATUS.FAIL.name());
        chainTransaction.setFromAddress(action.getFromAddress());
        chainTransaction.setToAddress(action.getToAddress());
        chainTransaction.setGasAddress(action.getFromAddress());
        chainTransaction.setAmount(amount);
        chainTransaction.setTokenSymbol(symbolConfig.getTokenSymbol());
        chainTransaction.setSymbol(symbolConfig.getSymbol());
        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
        chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
        chainTransaction.setBlockTime(new Date(transaction.getTxTime()));
        if (addressChecker.owner(action.getFromAddress(), getChainId())) {
            chainTransaction.setActGas(transaction.getFee().divide(mainCoinConfig.precision(), mainCoinConfig.getSymbolPrecision(), RoundingMode.DOWN));
        } else {
            chainTransaction.setActGas(BigDecimal.ZERO);
        }
        CoinBalance coinBalance = new CoinBalance();
        coinBalance.setChainId(getChainId());
        coinBalance.setCoin(symbolConfig.getTokenSymbol());
        coinBalance.setApiCoin(symbolConfig.getSymbol());
        coinBalance.setContractAddress(StringUtils.isBlank(action.getContractAddress()) ? "" : action.getContractAddress());
        coinBalance.setAddress(action.getToAddress());
        coinBalance.setBalance(action.getPostBalance().divide(symbolConfig.precision(), symbolConfig.getSymbolPrecision(), RoundingMode.DOWN));
        coinBalance.setBlockHeight(new BigInteger(String.valueOf(transaction.getBlockHeight())));
        coinBalance.setBlockTime(blockTime);
        coinBalance.setMtime(new Date());
        coinBalanceService.upsert(coinBalance); // 更新余额
        return chainTransaction;
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        Long blockHeight = getChainClient(null).getClient().getHeight().getBlockHeight();
        return blockHeight == null ? "0" : String.valueOf(blockHeight);
    }

    @Override
    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {

    }

    @Override
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        return Collections.emptyList();
    }

    @Override
    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {

    }

    @Override
    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
        return getTokenBalance(address, tokenAddress, null);
    }

    @Override
    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
        return getBalance(address, null);
    }

    @Override
    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
        return getTokenBalance(address, null, blockNumber);
    }

    @Override
    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
        AddressBalance addressBalance = getChainClient(null).getClient().getBalance(address, tokenAddress);
        return addressBalance == null ? BigDecimal.ZERO : addressBalance.getBalance();
    }

    @Override
    public BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig) {
        return JSON.parseObject(coinConfig.getConfigJson()).getBigDecimal("gas");
    }

    @Override
    protected BlockChain<SolanaRpcClient>.ChainClient create(JSONObject item) {
        String url = item.getString("url");
        SolanaRpcClient solanaRpcClient = new SolanaRpcClient(url);
        return new ChainClient(url, solanaRpcClient) {
            @Override
            public void close() {
                log.info("close_sol_client");
            }
        };
    }

    @Override
    public LastBlockInfo getLastBlockInfo() {
        return this.getChainClient(null).getClient().getLastBlockInfo();
    }

    @Override
    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        super.freshChainScanConfig(chainScanConfig);
        HashMap<String, SymbolConfig> configHashMap = new HashMap<>();
        List<SymbolConfig> coinConfigs = chainScanConfig.getCoinConfigs();
        if (CollectionUtils.isNotEmpty(coinConfigs)) {
            for (SymbolConfig coinConfig : coinConfigs) {
                if (StringUtils.isNotBlank(coinConfig.getContractAddress())) {
                    configHashMap.put(this.formatAddress(coinConfig.getContractAddress()), coinConfig);
                }
            }
        }
        this.configHashMap = configHashMap;
    }

    @Override
    public SymbolConfig getTokenConfig(String contractAddress) {
        return configHashMap.get(contractAddress.toLowerCase());
    }

}
