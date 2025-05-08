package com.tk.chain.sol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tk.chain.sol.model.*;
import com.tk.chains.BlockChain;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.chains.service.BlockTransactionManager;
import com.tk.wallet.common.entity.*;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service("SOL")
public class SolanaBlockChain extends BlockChain<SolanaRpcClient> {

    private final BlockTransactionManager blockTransactionManager;
    private final WalletAddressService walletAddressService;
    private final WalletSymbolConfigService walletSymbolConfigService;

    private volatile Map<String, SymbolConfig> configHashMap = new HashMap<>();

    public SolanaBlockChain(BlockTransactionManager blockTransactionManager, WalletAddressService walletAddressService, WalletSymbolConfigService walletSymbolConfigService) {
        super();
        this.blockTransactionManager = blockTransactionManager;
        this.walletAddressService = walletAddressService;
        this.walletSymbolConfigService = walletSymbolConfigService;
    }

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
        if (StringUtils.isBlank(chainTransaction.getGasAddress())) {
            throw new ChainParamsError("gasConfig_gas_address is empty : " + gasConfig);
        }
        if (!addressChecker.owner(chainTransaction.getGasAddress(), chainId)) {
            throw new ChainParamsError("gasConfig_gas_address is not owner : " + gasConfig);
        }
    }

    public void beforeSaveChainTransactions(ChainScanConfig chainScanConfig, String chainId, BigInteger blockHeight, Date blockTime, List<ChainTransaction> chainTransactions) {

    }

    @Override
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, BlockChain<SolanaRpcClient>.ChainClient chainClient) {
        BlockTx block = chainClient.getClient().getBlockTx(blockNumber.longValue());
        List<ScanResultTx> txList = new ArrayList<>();
        HashMap<String, CoinBalance> upsertCoinBalances = new HashMap<>();
        for (Transaction tx : block.getTxs()) {
            List<ChainTransaction> chainTransactions = new ArrayList<>();
            for (Transaction.Action action : tx.getActions()) {
                if (addressChecker.owner(action.getFromAddress(), action.getToAddress(), this.chainId, tx.getTxHash())) {
                    chainTransactions.addAll(parseChainTransaction(tx, chainClient, new Date(block.getBlockTime() * 1000)));
                }
            }
            if (CollectionUtils.isNotEmpty(chainTransactions)) {
                txList.add(new ScanResultTx(tx.getTxHash(), null, tx.getFeePayer(), tx.getFee(), chainTransactions, chainTransactions.get(0).getTxStatus()));
                for (Map.Entry<String, Map<String, BigDecimal>> kv : tx.getPostBalances().entrySet()) {
                    String owner = kv.getKey();
                    Map<String, BigDecimal> value = kv.getValue();
                    if (addressChecker.owner(owner, this.chainId)) {
                        for (Map.Entry<String, BigDecimal> mintAmount : value.entrySet()) {
                            String contract = mintAmount.getKey();
                            BigDecimal amount = mintAmount.getValue();
                            SymbolConfig symbolConfig = null;
                            if (StringUtils.isNotBlank(contract)) {
                                if (!this.configHashMap.containsKey(contract)) {
                                    symbolConfig = configHashMap.get(contract);
                                }
                            } else {
                                symbolConfig = this.mainCoinConfig;
                            }
                            if (symbolConfig != null) {
                                CoinBalance coinBalance = new CoinBalance();
                                coinBalance.setChainId(getChainId());
                                coinBalance.setCoin(symbolConfig.getTokenSymbol());
                                coinBalance.setApiCoin(symbolConfig.getSymbol());
                                coinBalance.setContractAddress(symbolConfig.getContractAddress());
                                coinBalance.setAddress(owner);
                                coinBalance.setBalance(amount);
                                coinBalance.setBlockHeight(blockNumber);
                                coinBalance.setBlockTime(new Date(block.getBlockTime() * 1000));
                                coinBalance.setMtime(new Date());
                                upsertCoinBalances.put(coinBalance.getAddress() + "^^^^" + coinBalance.getContractAddress(), coinBalance);
                            }
                        }
                    }
                }
            }
        }

        // 合并整个块后更新余额
        for (Map.Entry<String, CoinBalance> kv : upsertCoinBalances.entrySet()) {
            coinBalanceService.upsert(kv.getValue()); // 更新余额
        }

        return new ScanResult(txList.size(), txList, blockNumber, new Date(block.getBlockTime() * 1000));
    }

    private List<ChainTransaction> parseChainTransaction(Transaction transaction, BlockChain<SolanaRpcClient>.ChainClient chainClient, Date blockTime) {
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        if (transaction.getActions() != null && !transaction.getActions().isEmpty()) {
            for (int i = 0; i < transaction.getActions().size(); i++) {
                Transaction.Action action = transaction.getActions().get(i);
                if (StringUtils.isNotEmpty(action.getContractAddress())) {
                    if (configHashMap.containsKey(action.getContractAddress())) {
                        SymbolConfig symbolConfig = configHashMap.get(action.getContractAddress());
                        ChainTransaction chainTransaction = parseChainTransaction(transaction, chainClient, action, symbolConfig, blockTime);
                        chainTransaction.setContract(action.getContractAddress());
                        chainTransactions.add(chainTransaction);
                    }
                } else {
                    ChainTransaction chainTransaction = parseChainTransaction(transaction, chainClient, action, this.mainCoinConfig, blockTime);
                    chainTransactions.add(chainTransaction);
                }
            }
        }
        return chainTransactions;
    }

    private ChainTransaction parseChainTransaction(Transaction transaction, BlockChain<SolanaRpcClient>.ChainClient chainClient, Transaction.Action action, SymbolConfig symbolConfig, Date blockTime) {
        BigDecimal amount = action.getAmount().divide(symbolConfig.precision(), symbolConfig.getSymbolPrecision(), RoundingMode.DOWN);
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
        chainTransaction.setAmount(amount.stripTrailingZeros());
        chainTransaction.setTokenSymbol(symbolConfig.getTokenSymbol());
        chainTransaction.setContract(action.getContractAddress());
        chainTransaction.setSymbol(symbolConfig.getSymbol());
        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
        chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
        chainTransaction.setBlockTime(blockTime);
        return chainTransaction;
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        Long blockHeight = getChainClient(null).getClient().getHeight().getBlockHeight();
        return blockHeight == null ? "0" : String.valueOf(blockHeight);
    }

    /**
     * 安装gasAddress 分组
     *
     * @param chainScanConfig
     * @param chainTransactions
     */
    @Override
    public void groupTransfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        HashMap<String, List<ChainTransaction>> chainTransactionList = new HashMap<>();
        for (ChainTransaction chainTransaction : chainTransactions) {
            List<ChainTransaction> list = chainTransactionList.computeIfAbsent(chainTransaction.getGasAddress(), k -> new ArrayList<>());
            list.add(chainTransaction);
        }
        for (Map.Entry<String, List<ChainTransaction>> kv : chainTransactionList.entrySet()) {
            transfer(chainScanConfig, kv.getValue());
        }
    }

    @Override
    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        String gasAddress = chainTransactions.get(0).getGasAddress();
        Optional<ChainTransaction> chainTransactionOptional = chainTransactionService.lambdaQuery().eq(ChainTransaction::getGasAddress, gasAddress)//
                .eq(ChainTransaction::getChainId, chainId).eq(ChainTransaction::getTxStatus, ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name()).last("limit 1").oneOpt();
        if (chainTransactionOptional.isPresent()) {
            return;
        }
        BlockChain<SolanaRpcClient>.ChainClient chainClient = getChainClient(null);
        HashMap<String, Map<String, BigDecimal>> addressBalances = new HashMap<>();
        AddressBalance gasAddressBalance = chainClient.getClient().getBalance(gasAddress, "");
        if (gasAddressBalance == null) {
            return;
        } else {
            Map<String, BigDecimal> balances = addressBalances.computeIfAbsent(gasAddress, k -> new HashMap<>());
            balances.put("", gasAddressBalance.getBalance());
        }
        ArrayList<ChainTransaction> okList = new ArrayList<>();
        ArrayList<ChainTransaction> gasLowerList = new ArrayList<>();
        ArrayList<ChainTransaction> amountLowerList = new ArrayList<>();
        for (ChainTransaction chainTransaction : chainTransactions) {
            Map<String, BigDecimal> balances = addressBalances.computeIfAbsent(chainTransaction.getFromAddress(), k -> new HashMap<>());
            BigDecimal balance = balances.get(chainTransaction.getContract());
            if (balance == null) {
                AddressBalance addressBalance = chainClient.getClient().getBalance(chainTransaction.getFromAddress(), chainTransaction.getContract());
                balance = addressBalance.getBalance();
            }
            if (balance.compareTo(chainTransaction.getAmount()) >= 0) {
                balances.put(chainTransaction.getContract(), balance.subtract(chainTransaction.getAmount()));
                Map<String, BigDecimal> gasBalances = addressBalances.computeIfAbsent(gasAddress, k -> new HashMap<>());
                BigDecimal gas = JSON.parseObject(chainTransaction.getGasConfig()).getBigDecimal("gas");
                BigDecimal gasBalance = gasBalances.get("");
                if (gasBalance.compareTo(gas) >= 0) {
                    gasBalances.put("", gasBalance.subtract(gas));
                    okList.add(chainTransaction);
                } else {
                    gasLowerList.add(chainTransaction);
                }
            } else {
                amountLowerList.add(chainTransaction);
            }
        }
        if (!CollectionUtils.isEmpty(okList)) {
            ArrayList<Map<String, Object>> instructions = new ArrayList<>();
            List<Long> ids = okList.stream().map(ChainTransaction::getId).collect(Collectors.toList());
            for (ChainTransaction chainTransaction : okList) {
                HashMap<String, Object> instruction = new HashMap<>();
                instruction.put("from", chainTransaction.getFromAddress());
                instruction.put("to", chainTransaction.getToAddress());
                if (StringUtils.isBlank(chainTransaction.getContract())) {
                    BigDecimal amount = chainTransaction.getAmount().multiply(mainCoinConfig.precision());
                    instruction.put("amount", amount.stripTrailingZeros().toPlainString());
                } else {
                    BigDecimal amount = chainTransaction.getAmount().multiply(this.configHashMap.get(chainTransaction.getContract()).precision());
                    instruction.put("amount", amount.stripTrailingZeros().toPlainString());
                    instruction.put("mint", chainTransaction.getContract());
                    instruction.put("decimals", this.configHashMap.get(chainTransaction.getContract()).getSymbolPrecision());
                    SolTokenAccounts fromTokenAccounts = chainClient.getClient().getTokenAccountsByOwner(chainTransaction.getFromAddress(), chainTransaction.getContract());
                    String ataFrom = "";
                    if (fromTokenAccounts != null && CollectionUtils.isNotEmpty(fromTokenAccounts.getValue())) {
                        ataFrom = fromTokenAccounts.getValue().get(0).getPubkey();
                    }
                    if (StringUtils.isBlank(ataFrom)) {
                        throw new RuntimeException("ataFrom not found : " + chainTransaction.getFromAddress());
                    }
                    SolTokenAccounts ToTokenAccounts = chainClient.getClient().getTokenAccountsByOwner(chainTransaction.getToAddress(), chainTransaction.getContract());
                    String ataTo = "";
                    if (fromTokenAccounts != null && CollectionUtils.isNotEmpty(fromTokenAccounts.getValue())) {
                        ataTo = ToTokenAccounts.getValue().get(0).getPubkey();
                    }
                    instruction.put("ataFrom", ataFrom);
                    instruction.put("ataTo", ataTo);
                }
                instructions.add(instruction);
            }
            String latestBlocHash = chainClient.getClient().getLatestBlocHash();
            HashMap<String, Object> requestParams = new HashMap<>();
            requestParams.put("blockhash", latestBlocHash);
            requestParams.put("instructions", instructions);
            requestParams.put("feePayer", gasAddress);
            JSONObject ret = new JSONObject();
            sign(requestParams, ret::putAll);
            String hash = ret.getString("hash");
            String rawTx = ret.getString("rawTx");
            if (StringUtils.isBlank(hash) || StringUtils.isBlank(rawTx)) {
                log.error("requestParams = {}", JSON.toJSONString(requestParams));
                throw new RuntimeException("签名失败：" + StringUtils.join(ids, ","));
            }
            log.info("广播交易{} ：{},\t{}", StringUtils.join(ids, ","), hash, rawTx);
            if (blockTransactionManager.prepareTransfer(chainScanConfig.getBlockNumber(), chainClient.getUrl(), BigInteger.ZERO, ids)) {
                ChainWithdraw chainWithdraw = new ChainWithdraw();
                chainWithdraw.setGasAddress(gasAddress);
                chainWithdraw.setTransferId(hash);
                chainWithdraw.setRowData(rawTx);
                chainWithdraw.setIds(JSON.toJSONString(ids));
                chainWithdraw.setChainId(chainScanConfig.getChainId());
                chainWithdraw.setGas(BigDecimal.ZERO);
                chainWithdraw.setStatus(ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name());
                chainWithdraw.setHash(hash);
                chainWithdrawService.save(chainWithdraw);
                for (ChainTransaction transaction : okList) {
                    ChainTransaction update = new ChainTransaction();
                    update.setId(transaction.getId());
                    update.setTxStatus(ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name());
                    update.setHash(hash);
                    chainTransactionService.updateById(update);
                    blockTransactionManager.emit(transaction.getId());
                }
                try {
                    chainClient.getClient().sendTransaction(rawTx);
                } catch (SolanaRpcClient.ResponseError responseError) {
                    log.error("广播交易失败：", responseError);
                    for (Long id : ids) {
                        blockTransactionManager.releaseWaitingHash(id);
                    }
                } catch (Exception e) {
                    blockTransactionManager.networkError(chainWithdraw);
                }
            }
        }
        log.info("amountLowerList : {},gasLowerList = \t{}", amountLowerList.stream().map(ChainTransaction::getId).collect(Collectors.toList()), gasLowerList.stream().map(ChainTransaction::getId).collect(Collectors.toList()));
    }


    @Override
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        if (StringUtils.isNotBlank(hash)) {
            HashSet<String> urls = new HashSet<>();
            urls.add(excludeUrl);
            BlockChain<SolanaRpcClient>.ChainClient chainClient = getChainClient(urls);
            Transaction tx = chainClient.getClient().getTx(hash);
            return parseChainTransaction(tx, chainClient, new Date(tx.getBlockTime() * 1000));
        }
        return Collections.emptyList();
    }

    @Override
    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
        Transaction transaction = getChainClient(new HashSet<String>() {
            {
                add(chainTransaction.getUrlCode());
            }
        }).getClient().getTx(chainTransaction.getHash());
        if (transaction != null) {
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
        } else {
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
        }
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
        JSONArray headers = item.getJSONArray("headers");
        SolanaRpcClient solanaRpcClient;
        if (headers != null) {
            List<String> javaList = headers.toJavaList(String.class);
            solanaRpcClient = new SolanaRpcClient(url, javaList.toArray(new String[]{}));
        } else {
            solanaRpcClient = new SolanaRpcClient(url);
        }
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

    // 代支付gas
    @Override
    public String feePayer(String address) {
        WalletAddress walletAddress = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, address).one();
        Integer walletId = walletAddress.getWalletId();
        WalletSymbolConfig walletSymbolConfig = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, walletId).eq(WalletSymbolConfig::getSymbolConfigId, mainCoinConfig.getId()).one();
        return walletSymbolConfig.getEnergyAddress();
    }

}
