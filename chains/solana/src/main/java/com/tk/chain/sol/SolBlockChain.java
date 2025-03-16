//package com.tk.chain.sol;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.chainup.wallet.api.pojo.dto.thirdPart.*;
//import com.chainup.wallet.common.entity.WalletWithdrawRecord;
//import com.tk.chain.sol.core.Transaction;
//import com.tk.chains.BlockChain;
//import com.tk.chains.exceptions.ChainParamsError;
//import com.tk.wallet.common.entity.ChainScanConfig;
//import com.tk.wallet.common.entity.ChainTransaction;
//import com.tk.wallet.common.entity.CoinConfig;
//import org.apache.commons.collections.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.assertj.core.util.Lists;
//import org.bitcoinj.core.Base58;
//import org.bouncycastle.util.encoders.Hex;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.math.RoundingMode;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicReference;
//
//@Service("SOL")
//public class SolBlockChain extends BlockChain<SolChainClient> {
//    protected volatile Map<String, CoinConfig> configHashMap = new HashMap<>();
//
//    @Override
//    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
//        HashMap<String, CoinConfig> configHashMap = new HashMap<>();
//        List<CoinConfig> coinConfigs = chainScanConfig.getCoinConfigs();
//        if (CollectionUtils.isNotEmpty(coinConfigs)) {
//            for (CoinConfig coinConfig : coinConfigs) {
//                if (StringUtils.isNotBlank(coinConfig.getContractAddress())) {
//                    configHashMap.put(coinConfig.getContractAddress().toLowerCase(), coinConfig);
//                }
//            }
//        }
//        super.freshChainScanConfig(chainScanConfig);
//        this.configHashMap = configHashMap;
//    }
//
//    @Override
//    protected ChainClient create(JSONObject endpoint) {
//        String url = endpoint.getString("url");
//        SolChainClient solChainClient = new SolChainClient(url);
//        BlockChain<SolChainClient>.ChainClient chainClient = new ChainClient(url, solChainClient) {
//            @Override
//            public void close() {
//            }
//        };
//        return chainClient;
//    }
//
//    @Override
//    public LastBlockInfo getLastBlockInfo() throws Exception {
//        BlockChain<SolChainClient>.ChainClient chainClient = getChainClient(null);
//        return chainClient.getClient().getLastBlockInfo();
//    }
//
//    @Override
//    public CoinConfig getTokenConfig(String contractAddress) {
//        return configHashMap.get(contractAddress.toLowerCase());
//    }
//
//    @Override
//    public void checkChainTransaction(ChainTransaction chainTransaction) throws ChainParamsError {
//
//    }
//
//    @Override
//    public List<ChainTransaction> queryWaitToChain(String chainId) {
//        return chainTransactionService.getTransactionByStatus(chainId, Arrays.asList(ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name(), ChainTransaction.TX_STATUS.INIT.name()));
//    }
//
//    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) throws Exception {
//        BlockTx block = chainClient.getClient().getBlockTx(blockNumber.longValue());
//        List<Transaction> txs = new ArrayList<>();
//        block.getTxs().forEach(transaction -> {
//            transaction.getActions().removeIf(action -> !addressManager.owner(action.getFromAddress(), action.getToAddress(), getChainId(), transaction.getTxHash()));
//            if (!transaction.getActions().isEmpty()) {
//                transaction.setTxTime(block.getBlockTime());
//                txs.add(transaction);
//            }
//        });
//        List<ChainTransaction> chainTransactions = Lists.newArrayList();
//
//        for (Transaction tx : txs) {
//            List<ChainTransaction> transactionList = getChainTransaction(tx);
//            if (CollectionUtils.isNotEmpty(transactionList)) {
//                chainTransactions.addAll(transactionList);
//            }
//        }
//        return new ScanResult(txs.size(), chainTransactions, blockNumber, new Date(block.getBlockTime() == null ? 0 : block.getBlockTime()));
//    }
//
//    public Mono<ScanResult> scanMono(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) {
//        return chainClient.getClient().getBlockTx(blockNumber.longValue(), "").map(
//                block -> {
//                    List<Transaction> txs = Lists.newArrayList();
//                    block.getTxs().forEach(transaction -> {
//                        transaction.getActions().removeIf(action -> !addressManager.owner(action.getFromAddress(), action.getToAddress(), getChainId(), transaction.getTxHash()));
//                        if (!transaction.getActions().isEmpty()) {
//                            transaction.setTxTime(block.getBlockTime());
//                            txs.add(transaction);
//                        }
//                    });
//                    List<ChainTransaction> chainTransactions = Lists.newArrayList();
//
//                    for (Transaction tx : txs) {
//                        List<ChainTransaction> transactionList = getChainTransaction(tx);
//                        if (CollectionUtils.isNotEmpty(transactionList)) {
//                            chainTransactions.addAll(transactionList);
//                        }
//                    }
//                    return new ScanResult(txs.size(), chainTransactions, blockNumber, new Date(block.getBlockTime() == null ? 0 : block.getBlockTime()));
//                }
//        );
//    }
//
//    public List<ChainTransaction> getChainTransaction(Transaction transaction) {
//        List<ChainTransaction> chainTransactions = new ArrayList<>();
//        if (transaction.getActions() != null && !transaction.getActions().isEmpty()) {
//            transaction.getActions().forEach(action -> {
//                if (StringUtils.isNotEmpty(action.getContractAddress())) {
//                    if (configHashMap.containsKey(action.getContractAddress().toLowerCase())) {
//                        CoinConfig coinConfig = configHashMap.get(action.getContractAddress().toLowerCase());
//                        ChainTransaction chainTransaction = new ChainTransaction();
//                        chainTransaction.setChainId(getChainId());
//                        chainTransaction.setHash(transaction.getTxHash());
//                        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getTxTime() == null ? 0 : transaction.getTxTime()));
//                        chainTransaction.setCtime(new Date());
//                        chainTransaction.setContract(action.getContractAddress());
//                        chainTransaction.setActGas(BigDecimal.ZERO);
//                        chainTransaction.setTxStatus(PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus()) ? ChainTransaction.TX_STATUS.PENDING.name() : ChainTransaction.TX_STATUS.FAIL.name());
//                        chainTransaction.setFromAddress(action.getFromAddress());
//                        chainTransaction.setGasAddress(action.getFromAddress());
//                        chainTransaction.setToAddress(action.getToAddress());
//                        chainTransaction.setAmount(action.getFromAmount().divide(BigDecimal.TEN.pow(coinConfig.getSymbolPrecision()), coinConfig.getSymbolPrecision(), RoundingMode.DOWN));
//                        chainTransaction.setCoin(coinConfig.getCoin());
//                        chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
//                        chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmNum());
//                        chainTransaction.setBlockTime(new Date(transaction.getTxTime()));
//                        chainTransactions.add(chainTransaction);
//                    }
//                } else {
//                    ChainTransaction chainTransaction = new ChainTransaction();
//                    chainTransaction.setChainId(getChainId());
//                    chainTransaction.setHash(transaction.getTxHash());
//                    chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getTxTime() == null ? 0 : transaction.getTxTime()));
//                    chainTransaction.setCtime(new Date());
//                    chainTransaction.setActGas(BigDecimal.ZERO);
//                    chainTransaction.setTxStatus(PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus()) ? ChainTransaction.TX_STATUS.PENDING.name() : ChainTransaction.TX_STATUS.FAIL.name());
//                    chainTransaction.setFromAddress(action.getFromAddress());
//                    chainTransaction.setToAddress(action.getToAddress());
//                    chainTransaction.setGasAddress(action.getFromAddress());
//                    chainTransaction.setAmount(action.getFromAmount().divide(BigDecimal.TEN.pow(mainCoinConfig.getSymbolPrecision()), mainCoinConfig.getSymbolPrecision(), RoundingMode.DOWN));
//                    chainTransaction.setCoin(this.mainCoinConfig.getCoin());
//                    chainTransaction.setBlockNum(BigInteger.valueOf(transaction.getBlockHeight()));
//                    chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmNum());
//                    chainTransaction.setBlockTime(new Date(transaction.getTxTime()));
//                    chainTransactions.add(chainTransaction);
//                }
//            });
//        }
//        return chainTransactions;
//    }
//
//    @Override
//    public String blockHeight(ChainScanConfig chainScanConfig) {
//        return getChainClient(null).getClient().getHeight().getBlockHeight().toString();
//    }
//
//    @Override
//    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
//        return Lists.newArrayList();
//    }
//
//    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
//        Transaction transaction;
//        try {
//            transaction = getChainClient(null).getClient().getTx(chainTransaction.getHash());
//        } catch (Exception e) {
//            log.error(chainTransaction.getChainId() + "\tconfirmTransaction = " + chainTransaction.getHash(), e);
//            return;
//        }
//        if (transaction != null && PaymentStatusEnum.CONFIRMED.getCode().equals(transaction.getStatus())) {
//            chainTransactionService.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
//        } else {
//            log.error("chain = {}\tconfirmTransaction = {},\tnot found", getChainId(), chainTransaction.getHash());
//            chainTransactionService.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
//        }
//    }
//
//    @Override
//    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
//        CoinConfig coinConfig = configHashMap.get(tokenAddress.toLowerCase());
//        return getChainClient(null).getClient().getBalance(address, tokenAddress).getBalance().divide(coinConfig.precision());
//    }
//
//    @Override
//    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
//        return getChainClient(null).getClient().getBalance(address, null).getBalance().divide(this.mainCoinConfig.precision());
//    }
//
//    private BigInteger getBalanceOrigin(String address, String contract) {
//        return getChainClient(null).getClient().getBalance(address, contract).getBalance().toBigInteger();
//    }
//
//    @Override
//    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
//        return null;
//    }
//
//    @Override
//    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
//        return null;
//    }
//
//    @Override
//    public BigDecimal gas(ChainScanConfig chainScanConfig, CoinConfig coinConfig) {
//        JSONObject jsonObject = JSON.parseObject(coinConfig.getConfigJson());
//        if (jsonObject.containsKey("gasLimit") && jsonObject.getBigDecimal("gasLimit").compareTo(BigDecimal.ZERO) > 0) {
//            return jsonObject.getBigDecimal("gasLimit").divide(mainCoinConfig.precision());
//        }
//        return new BigDecimal("5000").divide(mainCoinConfig.precision());
//    }
//
//    public void reTransfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
//        for (ChainTransaction transaction : chainTransactions) {
//            List<WalletWithdrawRecord> list = walletWithdrawRecordService.findByBusinessId(transaction.getBusinessId());
//            AtomicReference<Date> latestDate = new AtomicReference<>(new Date(0L));
//            AtomicReference<String> recentBlockHash = new AtomicReference<>();
//
//            list.forEach(walletWithdraw -> {
//                // 比较时间，把最新的创建时间保存到latestDate
//                if (walletWithdraw.getTxTime().after(latestDate.get())) {
//                    latestDate.set(walletWithdraw.getTxTime());
//                    recentBlockHash.set(walletWithdraw.getRecentBlockHash());
//                }
//                Transaction tx;
//                try {
//                    tx = getChainClient(null).getClient().getTx(walletWithdraw.getTxid());
//                } catch (Exception e) {
//                    log.error(transaction.getChainId() + "\t getTx:" + walletWithdraw.getTxid(), e);
//                    return;
//                }
//                if (tx != null && PaymentStatusEnum.CONFIRMED.getCode().equals(tx.getStatus())) {
//                    checkWithdrawStatus(walletWithdraw);
//                    chainTransactionService.updateHash(transaction.getId(), walletWithdraw.getTxid(), null);
//                    chainTransactionService.updateTxStatus(transaction.getChainId(), transaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, transaction.getId(), true);
//                } else if (tx != null) {
//                    log.error("chain = {}\tconfirmTransaction = {},\tnot found", getChainId(), transaction.getHash());
//                    chainTransactionService.updateTxStatus(transaction.getChainId(), transaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, transaction.getId(), true);
//                }
//            });
//            CoinConfig coinConfig;
//            if (StringUtils.isNotBlank(transaction.getContract())) {
//                coinConfig = configHashMap.get(transaction.getContract().toLowerCase());
//            } else {
//                coinConfig = mainCoinConfig;
//            }
//            if (coinConfig == null) {
//                log.error("合约配置错误 contract address = {},id = \t{}", transaction.getContract(), transaction.getId());
//                chainTransactionService.updateTxStatus(transaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.GAS_NOT_ENOUGH.name(), null, BigInteger.ZERO, true);
//                continue;
//            }
//            Long blockHeight = getChainClient(null).getClient().getHeight().getBlockHeight();
//
//            String[] heightAndHash = recentBlockHash.get().split("_");
//            Long lastSendHeight = null;
//            String hash = null;
//            if (heightAndHash.length == 2) {
//                lastSendHeight = Long.parseLong(heightAndHash[0]);
//                hash = heightAndHash[1];
//            } else {
//                log.info("SOL 提现任务没有看到块高和hash,不重试, businessId:{}", transaction.getBusinessId());
//                continue;
//            }
//            if ((blockHeight - lastSendHeight) < 600) {
//                //距离上次发送，时间不到5分钟，不处理，等时间到
//                log.info("距离上次发送，时间不到10分钟，不处理，等时间到：{}, orderId:{}", blockHeight - lastSendHeight, transaction.getBusinessId());
//                continue;
//            }
//            if (new Date().getTime() - latestDate.get().getTime() > 5 * 60 * 1000) {
//                if (list.size() > 30) {
//                    chainTransactionService.updateTxStatus(transaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.GAS_NOT_ENOUGH.name(), null, BigInteger.ZERO, true);
//                } else {
//                    log.info("blockHeight：{}", blockHeight);
//                    log.info("getChainClient(null).getClient()：{}", getChainClient(null).getClient());
//                    Block block = getChainClient(null).getClient().getBlock(blockHeight.longValue() + 120);
//                    log.info("block：{}", block);
//                    String latestHash = block.getBlockHash();
//                    String blockHeightAndHash = blockHeight + "_" + latestHash;
//                    BigDecimal amount = transaction.getAmount().multiply(coinConfig.precision());
//                    transaction.setAmount(amount);
//                    RpcResponse<String> rpcResponse = submitTransaction(transaction, coinConfig, latestHash);
//                    WalletWithdrawRecord walletWithdrawRecord = new WalletWithdrawRecord();
//                    walletWithdrawRecord.setCreatedAt(new Date());
//                    walletWithdrawRecord.setUpdatedAt(new Date());
//                    walletWithdrawRecord.setChainTransactionId(transaction.getId());
//                    walletWithdrawRecord.setBusinessId(transaction.getBusinessId());
//                    walletWithdrawRecord.setChain(chainId);
//                    walletWithdrawRecord.setCoin(coinConfig.getCoin());
//                    walletWithdrawRecord.setFromAddr(transaction.getFromAddress());
//                    walletWithdrawRecord.setToAddr(transaction.getToAddress());
//                    BigDecimal accuracy = new BigDecimal(10).pow(coinConfig.getSymbolPrecision());
//                    walletWithdrawRecord.setAmount(transaction.getAmount().divide(accuracy, 18, RoundingMode.DOWN));
//                    walletWithdrawRecord.setState(0);
//                    walletWithdrawRecord.setTxTime(new Date());
//                    walletWithdrawRecord.setFromAddr(transaction.getFromAddress());
//                    walletWithdrawRecord.setTxMemo(transaction.getMemo());
//                    walletWithdrawRecord.setId(null);
//                    walletWithdrawRecord.setCoin(transaction.getCoin());
//                    walletWithdrawRecord.setRecentBlockHash(blockHeightAndHash);
//                    if (rpcResponse.isSuccess()) {
//                        walletWithdrawRecord.setTxid(rpcResponse.getData());
//                        walletWithdrawRecordService.saveOrUpdate(walletWithdrawRecord);
//                    } else {
//                        log.error("Send ton transaction fail : {},\t{}", transaction.getId() + ",\t" + transaction.getBusinessId(), rpcResponse.getMessage());
//                        walletWithdrawRecord.setState(2);
//                        walletWithdrawRecordService.saveOrUpdate(walletWithdrawRecord);
//                        chainTransactionService.releaseWaitingHash(transaction.getId());
//                    }
//                }
//            }
//
//        }
//    }
//
//
//    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
//        // 转为小写
//        for (ChainTransaction chainTransaction : chainTransactions) {
//            formatAddress(chainTransaction);
//        }
//        BlockChain.ChainClient chainClient = getChainClient(null);
//        if (org.apache.commons.collections4.CollectionUtils.isEmpty(chainTransactions)) {
//            return;
//        }
//        // 当前区块高度
//        BigInteger blockHeight = new BigInteger(blockHeight(chainScanConfig));
//        String fromAddress = chainTransactions.get(0).getFromAddress();
//        List<ChainTransaction> value = chainTransactions;
//        BigInteger mainCoinBalance = getBalanceOrigin(fromAddress, null);
//        // 涉及到转账的token 余额
//        HashMap<String, BigInteger> tokenBalances = new HashMap<>();
//        // 涉及到转账的token 精度
//        for (ChainTransaction chainTransaction : value) {
//            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
//                if (!tokenBalances.containsKey(chainTransaction.getContract())) {
//                    BigInteger erc20Balance = getBalanceOrigin(fromAddress, chainTransaction.getContract());
//                    tokenBalances.put(chainTransaction.getContract(), erc20Balance);
//                }
//            }
//        }
//        LinkedList<ChainTransaction> chainTransactionLinkedList = new LinkedList<>(value);
//        Iterator<ChainTransaction> iterator = chainTransactionLinkedList.iterator();
//        while (iterator.hasNext()) {
//            ChainTransaction chainTransaction = iterator.next();
//            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
//                CoinConfig coinConfig = configHashMap.get(chainTransaction.getContract().toLowerCase());
//                // 无法转换精度，排除本次转账
//                if (coinConfig == null) {
//                    log.error("合约配置错误 contract address = {},id = \t{}", chainTransaction.getContract(), chainTransaction.getId());
//                    chainTransactionService.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "合约配置错误", chainScanConfig.getBlockHeight(), true);
//                    iterator.remove();
//                } else {
//                    // 转为链上uint256 数值（链上没有小数），金额*精度
//                    BigDecimal amount = chainTransaction.getAmount().multiply(coinConfig.precision());
//                    chainTransaction.setAmount(amount);
//                }
//            } else {
//                BigDecimal amount = chainTransaction.getAmount().multiply(mainCoinConfig.precision());
//                chainTransaction.setAmount(amount);
//            }
//        }
//
//        // 可以发起的交易
//        List<ChainTransaction> okList = new ArrayList<>();
//        // 燃料不足的交易
//        List<ChainTransaction> gasLowerList = new ArrayList<>();
//        // 余额不足的交易
//        List<ChainTransaction> balanceNotEnoughList = new ArrayList<>();
//        // 旷工报价
//        BigInteger gasPrice = null;
//        // 经过精度转换后的交易
//        for (ChainTransaction chainTransaction : chainTransactionLinkedList) {
//            chainTransaction.setNonce(BigInteger.ONE);
//            JSONObject gasConfig = JSON.parseObject(chainTransaction.getGasConfig());
//            BigInteger transferGas = gasConfig.getBigInteger("gasLimit");
//            if (transferGas == null) {
//                transferGas = new BigInteger("50000000");
//            }
//            // erc-20 转账
//            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
//                if (mainCoinBalance.compareTo(transferGas) < 0) {
//                    gasLowerList.add(chainTransaction);
//                } else {
//                    BigInteger amount = tokenBalances.get(chainTransaction.getContract());
//                    if (amount.compareTo(chainTransaction.getAmount().toBigInteger()) < 0) {
//                        balanceNotEnoughList.add(chainTransaction);
//                    } else {
//                        chainTransaction.setCoinBalance(mainCoinBalance);
//                        chainTransaction.setTransferGas(transferGas);
//                        chainTransaction.setTransferPrice(BigInteger.ONE);
//                        chainTransaction.setTokenBalance(amount);
//                        chainTransaction.setLimit(transferGas);
//                        chainTransaction.setNonce(BigInteger.ONE);
//                        tokenBalances.put(chainTransaction.getContract(), amount.subtract(chainTransaction.getAmount().toBigInteger()));
//                        mainCoinBalance = mainCoinBalance.subtract(transferGas);
//                        okList.add(chainTransaction);
//                    }
//                }
//            } else {// 主币
//                BigInteger amount = chainTransaction.getAmount().toBigInteger();
//                if (mainCoinBalance.compareTo(transferGas.add(amount)) >= 0) {
//                    chainTransaction.setCoinBalance(mainCoinBalance);
//                    chainTransaction.setTransferGas(transferGas);
//                    chainTransaction.setTransferPrice(BigInteger.ONE);
//                    chainTransaction.setLimit(transferGas);
//                    chainTransaction.setNonce(BigInteger.ONE);
//                    okList.add(chainTransaction);
//                    mainCoinBalance = mainCoinBalance.subtract(transferGas.add(amount));
//                } else {
//                    balanceNotEnoughList.add(chainTransaction);
//                }
//            }
//        }
//        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(okList)) {
//            for (ChainTransaction transaction : okList) {
//                CoinConfig coinConfig;
//                if (StringUtils.isNotBlank(transaction.getContract())) {
//                    coinConfig = configHashMap.get(transaction.getContract().toLowerCase());
//                } else {
//                    coinConfig = mainCoinConfig;
//                }
//                if (coinConfig == null) {
//                    log.error("合约配置错误 contract address = {},id = \t{}", transaction.getContract(), transaction.getId());
//                    chainTransactionService.updateTxStatus(transaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.GAS_NOT_ENOUGH.name(), null, BigInteger.ZERO, true);
//                    continue;
//                }
//                if (chainTransactionService.prepareTransfer(transaction.getId(), blockHeight, chainClient.getUrl(), transaction.getNonce())) {
//                    String latestHash = getChainClient(null).getClient().getBlock(blockHeight.longValue() + 120).getBlockHash();
//                    String blockHeightAndHash = blockHeight + "_" + latestHash;
//                    log.info("start_transfer_1 : id={},\tBusinessId={}", transaction.getId(), transaction.getBusinessId());
//                    RpcResponse<String> rpcResponse = submitTransaction(transaction, coinConfig, latestHash);
//                    WalletWithdrawRecord walletWithdraw = new WalletWithdrawRecord();
//                    walletWithdraw.setCreatedAt(new Date());
//                    walletWithdraw.setUpdatedAt(new Date());
//                    walletWithdraw.setChainTransactionId(transaction.getId());
//                    walletWithdraw.setBusinessId(transaction.getBusinessId());
//                    walletWithdraw.setChain(chainId);
//                    walletWithdraw.setCoin(transaction.getCoin());
//                    walletWithdraw.setFromAddr(transaction.getFromAddress());
//                    walletWithdraw.setToAddr(transaction.getToAddress());
//                    BigDecimal accuracy = new BigDecimal(10).pow(coinConfig.getSymbolPrecision());
//                    walletWithdraw.setAmount(transaction.getAmount().divide(accuracy, 18, RoundingMode.DOWN));
//                    walletWithdraw.setState(0);
//                    walletWithdraw.setTxTime(new Date());
//                    walletWithdraw.setFromAddr(fromAddress);
//                    walletWithdraw.setTxMemo(transaction.getMemo());
//                    walletWithdraw.setId(null);
//                    walletWithdraw.setRecentBlockHash(blockHeightAndHash);
//                    if (rpcResponse.isSuccess()) {
//                        log.info("Send ton transaction success : {}, {}", transaction.getId(), rpcResponse.getData());
//                        walletWithdraw.setTxid(rpcResponse.getData());
//                        walletWithdrawRecordService.save(walletWithdraw);
//                        chainTransactionService.updateHash(transaction.getId(), rpcResponse.getData(), null);
//                    } else {
//                        log.error("Send ton transaction fail : {},\t{}", transaction.getId() + ",\t" + transaction.getBusinessId(), rpcResponse.getMessage());
//                        walletWithdraw.setState(2);
//                        walletWithdrawRecordService.save(walletWithdraw);
//                        chainTransactionService.releaseWaitingHash(transaction.getId());
//                    }
//                }
//            }
//        } else {
//            if (org.apache.commons.collections4.CollectionUtils.isEmpty(gasLowerList) && org.apache.commons.collections4.CollectionUtils.isNotEmpty(balanceNotEnoughList)) {
//                ChainTransaction chainTransaction = balanceNotEnoughList.get(0);
//                chainTransactionService.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.BALANCE_NOT_ENOUGH.name(), null, blockHeight, true);
//            } else if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(gasLowerList)) {
//                ChainTransaction chainTransaction = gasLowerList.get(0);
//                chainTransactionService.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.GAS_NOT_ENOUGH.name(), null, blockHeight, true);
//            }
//        }
//    }
//
//    public void checkWithdrawStatus(WalletWithdrawRecord walletWithdraw) {
//        List<ChainTransaction> chainTransactionList = chainTransactionService.getByHash(walletWithdraw.getTxid());
//        if (chainTransactionList.size() > 1) {
//            ChainTransaction scanTransaction = chainTransactionList.stream().filter(item -> StringUtils.isBlank(item.getBusinessId())).findFirst().orElse(null);
//            ChainTransaction withdrawTransaction = chainTransactionList.stream().filter(item -> !StringUtils.isBlank(item.getBusinessId())).findFirst().orElse(null);
//            if (scanTransaction != null && withdrawTransaction != null) {
//                withdrawTransaction.setTxStatus(scanTransaction.getTxStatus());
//                withdrawTransaction.setGasAddress(scanTransaction.getGasAddress());
//                withdrawTransaction.setNeedConfirmNum(scanTransaction.getNeedConfirmNum());
//                withdrawTransaction.setBlockNum(scanTransaction.getBlockNum());
//                withdrawTransaction.setCoin(scanTransaction.getCoin());
//                chainTransactionService.txSaveOrUpdate(withdrawTransaction);
//                chainTransactionService.removeById(scanTransaction.getId());
//                walletWithdrawRecordService.completeByBusinessId(withdrawTransaction.getBusinessId());
//            }
//        }
//    }
//
//
//    private RpcResponse<String> submitTransaction(ChainTransaction chainTransaction, CoinConfig coinConfig, String recentBlockHash) {
//        SubmitTransaction submitTransaction = new SubmitTransaction();
//        submitTransaction.setFromAddress(chainTransaction.getFromAddress());
//        submitTransaction.setToAddress(chainTransaction.getToAddress());
//        submitTransaction.setContractAddress(chainTransaction.getContract());
//        submitTransaction.setAmount(chainTransaction.getAmount().toString());
//        submitTransaction.setMemo(chainTransaction.getMemo());
//        submitTransaction.setPrecision(coinConfig.getSymbolPrecision());
//        submitTransaction.setGasLimit(chainTransaction.getLimit() == null ? "" : chainTransaction.getLimit().toString());
//        submitTransaction.setNonce(chainTransaction.getNonce().longValue());
//        submitTransaction.setHotDecrypt(true);
//        submitTransaction.setRecentBlockHash(recentBlockHash);
//        String rawMessage = getChainClient(null).getClient().getRawMessage(submitTransaction);
//        Map<String, Object> signMap = new HashMap<>();
//        signMap.put("payload", rawMessage);
//        signMap.put("fromAddress", chainTransaction.getFromAddress());
//        log.info("Sign sol transaction:{}", submitTransaction);
//        log.info("Sign sol transaction ,payload:{}", rawMessage);
//        log.info("Sign sol transaction ,fromAddress:{}", chainTransaction.getFromAddress());
//        String signature = sign(signMap);
//        log.info("Sign sol transaction ,signature:{}", signature);
//        try {
//            for (int i = 0; i < 10; i++) {
//                getChainClient(null).getClient().submitTransaction(submitTransaction, signature);
//            }
//        } catch (Exception e) {
//            log.error("submitTransaction error:{}", e.getMessage());
//        }
//        return new RpcResponse<>(200, "", Base58.encode(Hex.decode(signature)));
//    }
//}
