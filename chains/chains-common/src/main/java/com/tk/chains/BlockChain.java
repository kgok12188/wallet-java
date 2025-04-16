package com.tk.chains;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tk.chains.event.EventManager;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.chains.service.AddressChecker;
import com.tk.chains.service.BlockTransactionManager;
import com.tk.chains.service.CoinBalanceService;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.ChainTransactionService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.client.RestTemplate;

import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * louis
 */
public abstract class BlockChain<T> implements ApplicationContextAware {

    protected Logger log = LoggerFactory.getLogger(this.getClass());


    @Value("${log.inv:60000}")
    protected long logInv;
    private long logTime = 0;
    @Getter
    protected String chainId;

    protected EventManager eventManager;
    protected ChainScanConfigService chainScanConfigService;
    protected AddressChecker addressChecker;
    protected BlockTransactionManager blockTransactionManager;
    protected ApplicationContext applicationContext;
    protected CoinBalanceService coinBalanceService;
    protected ChainTransactionService chainTransactionService;
    @Getter
    @Setter
    private volatile boolean singleThread = false;

    private static final AtomicLong index = new AtomicLong(0);

    protected ChainScanConfig chainScanConfig;

    @Getter
    private volatile LinkedList<ChainClient> chainClients = new LinkedList<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    private final ThreadLocal<Boolean> updateBalanceLocal = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };


    protected SymbolConfig mainCoinConfig;


    public ChainClient getRandomChainClient() {
        if (CollectionUtils.isEmpty(chainClients)) {
            return null;
        }
        return chainClients.get((int) (index.getAndIncrement() % chainClients.size()));
    }

    /**
     * 配置是否ok
     *
     * @return
     */
    public boolean configIsOk() {
        return this.mainCoinConfig != null && !CollectionUtils.isEmpty(chainClients);
    }

    public SymbolConfig getMainCoinConfig() {
        return configIsOk() ? mainCoinConfig : null;
    }

    public SymbolConfig getTokenConfig(String contractAddress) {
        return null;
    }


    /**
     * 检查参数是否完成
     *
     * @param chainTransaction
     * @return
     */
    public abstract void checkChainTransaction(ChainTransaction chainTransaction) throws ChainParamsError;


    public void updateBalance(boolean flag) {
        updateBalanceLocal.set(flag);
    }

    public void removeUpdateBalance() {
        updateBalanceLocal.remove();
    }

    /**
     * 格式化地址
     *
     * @param address
     * @return
     */
    public String formatAddress(String address) {
        return this.isValidTronAddress(address) ? address : "";
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.eventManager = applicationContext.getBean(EventManager.class);
        this.chainScanConfigService = applicationContext.getBean(ChainScanConfigService.class);
        this.blockTransactionManager = applicationContext.getBean(BlockTransactionManager.class);
        this.addressChecker = applicationContext.getBean(AddressChecker.class);
        this.coinBalanceService = applicationContext.getBean(CoinBalanceService.class);
        this.chainTransactionService = applicationContext.getBean(ChainTransactionService.class);
    }


    public void scanMultiThread(ChainScanConfig chainScanConfig, Supplier<Boolean> supplier, int deep) {

    }

    public void scanMultiThread(ChainScanConfig chainScanConfig, Supplier<Boolean> supplier) {
        scanMultiThread(chainScanConfig, supplier, 0);
    }

    /**
     * 区块扫描
     *
     * @param chainScanConfig 链配置
     * @param supplier        会掉是否中断
     */
    public void scan(ChainScanConfig chainScanConfig, Supplier<Boolean> supplier) {
        BigInteger blockNumber = chainScanConfig.getBlockNumber();
        BigInteger blockHeight = chainScanConfig.getBlockHeight();
        if (blockNumber.compareTo(blockHeight) == 0) {
            ChainScanConfig updateChainScanConfig = new ChainScanConfig();
            updateChainScanConfig.setChainId(chainScanConfig.getChainId());
            chainScanConfig.setScanTime(new Date());
            chainScanConfigService.updateById(updateChainScanConfig);
            return;
        }
        int rows = 0;
        int block = 0;
        int delayBlocks = chainScanConfig.getDelayBlocks() == null ? 0 : chainScanConfig.getDelayBlocks();
        while (blockHeight.compareTo(blockNumber) > 0 && blockNumber.compareTo(blockHeight.subtract(new BigInteger(String.valueOf(delayBlocks)))) < 0) {
            if (supplier != null && supplier.get()) {
                log.info("stop_chain_1 : {}", getChainId());
                break;
            }
            ChainClient chainClient = getChainClient(null);
            if (chainClient == null) {
                return;
            }
            try {
                blockNumber = blockNumber.add(new BigInteger("1"));
                block++;
                ScanResult scanResult = scan(chainScanConfig, blockNumber, chainClient);
                List<ChainTransaction> chainTransactions = scanResult.getChainTransactions();
                if (CollectionUtils.isNotEmpty(chainTransactions)) {
                    blockTransactionManager.saveBlock(chainScanConfig, scanResult, this.mergeSave(), this);
                }
                rows += scanResult.getCount();
                if ((System.currentTimeMillis() - logTime) >= logInv) {
                    log.info("chainId = {},\tread {},\tblockHeight = {},\t{},\ttx  :   blocks = {},\ttransactions = {}", getChainId(), blockNumber, chainScanConfig.getBlockHeight(), chainClient.getUrl(), block, rows);
                    logTime = System.currentTimeMillis();
                    rows = 0;
                    block = 0;
                }
                chainScanConfigService.updateBlockNumber(getChainId(), blockNumber);
            } catch (Exception e) {
                log.error("chainId = {},\tread  {}/{},\turl = {}", getChainId(), blockNumber, chainScanConfig.getBlockHeight(), chainClient.getUrl(), e);
                markClientError(chainClient);
                break;
            }
        }
    }


    protected boolean mergeSave() {
        return false;
    }

    /**
     * 指定区块高度扫描
     *
     * @param chainScanConfig
     * @param blockNumber
     * @return block读取的行数，返回0 不会扫描下一个区块
     */
    public abstract ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) throws Exception;


    // 更新账本
    public void beforeSaveChainTransactions(ChainScanConfig chainScanConfig, String chainId, BigInteger blockHeight, Date blockTime, List<ChainTransaction> chainTransactions) throws Exception {
        if (!updateBalanceLocal.get()) {
            return;
        }
        if (CollectionUtils.isEmpty(chainTransactions) || blockHeight == null || blockHeight.compareTo(BigInteger.ZERO) <= 0 || StringUtils.isBlank(chainId) || blockTime == null) {
            return;
        }
        log.info("update_balance_blockHeight = {}, blockTime = {}", blockHeight, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(blockTime));
        Map<String, BigDecimal> coinIn = new HashMap<>();
        Map<String, BigDecimal> coinOut = new HashMap<>();
        Map<String, Map<String, BigDecimal>> tokenIn = new HashMap<>();
        Map<String, Map<String, BigDecimal>> tokenOut = new HashMap<>();
        for (ChainTransaction chainTransaction : chainTransactions) {
            if (ChainTransaction.TX_STATUS.PENDING.name().equals(chainTransaction.getTxStatus()) || ChainTransaction.TX_STATUS.SUCCESS.name().equals(chainTransaction.getTxStatus())) {
                String fromAddress = chainTransaction.getFromAddress();
                String toAddress = chainTransaction.getToAddress();
                BigDecimal amount = chainTransaction.getAmount();
                String contract = chainTransaction.getContract();
                BigDecimal actGas = chainTransaction.getActGas();
                String gasAddress = chainTransaction.getGasAddress();
                if (StringUtils.isNotBlank(gasAddress) && actGas != null && actGas.compareTo(BigDecimal.ZERO) > 0) {
                    if (coinOut.containsKey(gasAddress)) {
                        coinOut.put(gasAddress, actGas.add(coinOut.get(gasAddress)));
                    } else {
                        coinOut.put(gasAddress, actGas);
                    }
                }
                if (StringUtils.isNotBlank(contract)) {
                    Map<String, BigDecimal> in = tokenIn.containsKey(contract) ? tokenIn.get(contract) : new HashMap<>();
                    tokenIn.put(contract, in);
                    if (StringUtils.isNotBlank(toAddress)) {
                        if (in.containsKey(toAddress)) {
                            in.put(toAddress, amount.add(in.get(toAddress)));
                        } else {
                            in.put(toAddress, amount);
                        }
                    }
                    Map<String, BigDecimal> out = tokenOut.containsKey(contract) ? tokenOut.get(contract) : new HashMap<>();
                    tokenOut.put(contract, out);
                    if (StringUtils.isNotBlank(fromAddress)) {
                        if (out.containsKey(fromAddress)) {
                            out.put(fromAddress, amount.add(out.get(fromAddress)));
                        } else {
                            out.put(fromAddress, amount);
                        }
                    }
                } else {
                    if (StringUtils.isNotBlank(fromAddress)) {
                        if (coinOut.containsKey(fromAddress)) {
                            coinOut.put(fromAddress, amount.add(coinOut.get(fromAddress)));
                        } else {
                            coinOut.put(fromAddress, amount);
                        }
                    }
                    if (StringUtils.isNotBlank(toAddress)) {
                        if (coinIn.containsKey(toAddress)) {
                            coinIn.put(toAddress, amount.add(coinIn.get(toAddress)));
                        } else {
                            coinIn.put(toAddress, amount);
                        }
                    }
                }
            } else if (StringUtils.isNotBlank(chainTransaction.getHash()) && ChainTransaction.TX_STATUS.FAIL.name().equals(chainTransaction.getTxStatus()) &&
                    chainTransaction.getActGas() != null && chainTransaction.getActGas().compareTo(BigDecimal.ZERO) > 0) { // 链上交易失败，需要计算手续费
                BigDecimal actGas = chainTransaction.getActGas();
                String gasAddress = chainTransaction.getGasAddress();
                if (StringUtils.isNotBlank(gasAddress)) {
                    if (coinOut.containsKey(gasAddress)) {
                        coinOut.put(gasAddress, actGas.add(coinOut.get(gasAddress)));
                    } else {
                        coinOut.put(gasAddress, actGas);
                    }
                }
            }
        }
        // 总净流入流出
        mergeInOut(coinIn, coinOut);

        HashSet<String> tokenAddresses = new HashSet<>();
        tokenAddresses.addAll(tokenIn.keySet());
        tokenAddresses.addAll(tokenOut.keySet());
        for (String tokenAddress : tokenAddresses) {
            Map<String, BigDecimal> in = tokenIn.computeIfAbsent(tokenAddress, k -> new HashMap<>());
            Map<String, BigDecimal> out = tokenOut.computeIfAbsent(tokenAddress, k -> new HashMap<>());
            mergeInOut(in, out);
        }
        coinBalanceService.updateBalance(chainScanConfig, coinIn, tokenIn, blockHeight, blockTime, this);
    }


    private void mergeInOut(Map<String, BigDecimal> coinIn, Map<String, BigDecimal> coinOut) {
        for (Map.Entry<String, BigDecimal> kv : coinOut.entrySet()) {
            BigDecimal amount = coinIn.get(kv.getKey());
            if (amount == null) {
                coinIn.put(kv.getKey(), kv.getValue().multiply(new BigDecimal("-1")));
            } else {
                coinIn.put(kv.getKey(), kv.getValue().multiply(new BigDecimal("-1")).add(amount));
            }
        }
    }


    public int loadHash(String chainId, String hash) {
        Optional<ChainScanConfig> chainScanConfigOptional = chainScanConfigService.lambdaQuery().eq(ChainScanConfig::getChainId, chainId).oneOpt();
        if (chainScanConfigOptional.isPresent()) {
            return loadHash(chainScanConfigOptional.get(), hash);
        } else {
            return 0;
        }
    }

    public int loadHash(ChainScanConfig chainScanConfig, String hash) {
        List<ChainTransaction> chainTransactions = getChainTransaction(chainScanConfig, hash, null);
        if (CollectionUtils.isNotEmpty(chainTransactions)) {
            List<ChainTransaction> transactions = chainTransactions.stream().filter(chainTransaction -> StringUtils.equalsIgnoreCase(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name())).collect(Collectors.toList());
            if (transactions.size() == 1) {
                blockTransactionManager.txSaveOrUpdate(transactions.get(0));
            } else if (transactions.size() > 1) {
                blockTransactionManager.txSaveOrUpdate(transactions);
            }
            return transactions.size();
        }
        return 0;
    }

    /**
     * 指定区块高度扫描
     *
     * @param chainScanConfig 链配置
     * @param blockNumber     区块高度
     * @return block读取的行数，返回0 不会扫描下一个区块
     */
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber) throws Exception {
        return scan(chainScanConfig, blockNumber, getRandomChainClient());
    }

    /**
     * 查询区块高度
     *
     * @param chainScanConfig 链配置
     * @return 区块高度
     */
    public abstract String blockHeight(ChainScanConfig chainScanConfig);


    /**
     * 转账
     *
     * @param chainScanConfig   链配置
     * @param chainTransactions 交易
     */
    public abstract void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions);

    public void groupTransfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        // start 按照发送地址分组===================
        HashMap<String, List<ChainTransaction>> batchTransactions = new HashMap<>();
        for (ChainTransaction chainTransaction : chainTransactions) {
            if (StringUtils.isNotBlank(chainTransaction.getFromAddress())) {
                List<ChainTransaction> chainTransactionList = batchTransactions.computeIfAbsent(chainTransaction.getFromAddress(), k -> new ArrayList<>());
                chainTransactionList.add(chainTransaction);
            }
        }
        // end 按照发送地址分组===================
        for (Map.Entry<String, List<ChainTransaction>> kv : batchTransactions.entrySet()) {
            List<ChainTransaction> collect = kv.getValue().stream().filter(k -> k.getNonce().compareTo(BigInteger.ZERO) > 0).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                collect.sort(new Comparator<ChainTransaction>() {
                    @Override
                    public int compare(ChainTransaction o1, ChainTransaction o2) {
                        return o1.getNonce().compareTo(o2.getNonce());
                    }
                });
                this.reTransfer(chainScanConfig, collect);
            } else {
                this.transfer(chainScanConfig, kv.getValue());
            }
        }
    }

    // 推送重复交易
    public void reTransfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {

    }

    /**
     * 根据hash 查询链上交易
     *
     * @param hash hash
     * @return 交易
     */
    public abstract List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl);


    public List<ChainTransaction> getChainTransaction(String chainId, String hash, String excludeUrl) {
        ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(chainId);
        if (chainScanConfig == null) {
            return new ArrayList<>(0);
        }
        return getChainTransaction(chainScanConfig, hash, excludeUrl);
    }

    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash) {
        return getChainTransaction(chainScanConfig, hash, null);
    }

    /**
     * 交叉验证交易
     *
     * @param chainScanConfig 链配置
     * @param chainTransaction 交易
     */
    public abstract void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction);


    // ==========================start 远程签名====================================
    protected String sign(Object signObj) {
        String signUrl = chainScanConfig.getSignUrl();
        try {
            JSONObject jsonObject = new RestTemplate().postForObject(signUrl, signObj, JSONObject.class);
            if (jsonObject != null && jsonObject.containsKey("data")) {
                return jsonObject.getString("data");
            }
        } catch (Exception e) {
            log.error("sign : {}", chainId, e);
        }
        return "";
    }
    // ==========================end 远程签名====================================

    /**
     * 查询合约余额
     *
     * @param chainScanConfig 链配置
     * @param address         地址
     * @param tokenAddress    合约地址()
     * @return 金额
     */
    public abstract BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress);

    /**
     * 查询主币余额
     *
     * @param chainScanConfig 链配置
     * @param address         地址
     * @return coin 余额
     */
    public abstract BigDecimal getBalance(ChainScanConfig chainScanConfig, String address);


    /**
     * 查询主币余额
     *
     * @param address 地址
     * @return 指定高度查询余额
     */
    protected abstract BigDecimal getBalance(String address, BigInteger blockNumber);


    /**
     * 查询代币余额
     *
     * @param address      地址
     * @param tokenAddress 合约地址
     * @return 金额
     */
    protected abstract BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber);

    /**
     * 查询转账gas
     *
     * @param chainScanConfig 链配置
     * @param coinConfig      币种配置
     * @return gas
     */
    public abstract BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig);

    /**
     * 定时刷新配置 60s 刷新一次
     * 更新客户端配置
     *
     * @param chainScanConfig 链配置
     */
    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        if (StringUtils.isBlank(chainId)) {
            this.chainId = chainScanConfig.getChainId();
        }
        this.chainScanConfig = chainScanConfig;
        List<SymbolConfig> coinConfigs = chainScanConfig.getCoinConfigs();
        for (SymbolConfig coinConfig : coinConfigs) {
            if (StringUtils.isBlank(coinConfig.getContractAddress())) {
                this.mainCoinConfig = coinConfig;
            }
        }
        String endpoints = chainScanConfig.getEndpoints();
        JSONObject jsonObject = JSON.parseObject(endpoints);
        JSONArray value = jsonObject.getJSONArray("value");
        HashMap<String, JSONObject> configMap = new HashMap<>();
        LinkedList<ChainClient> newChainClients = new LinkedList<>(this.chainClients);
        LinkedList<ChainClient> existList = new LinkedList<>();
        for (int i = 0; i < value.size(); i++) {
            JSONObject item = value.getJSONObject(i);
            String url = item.getString("url").trim();
            Integer sort = item.getInteger("sort");
            sort = sort == null ? 0 : sort;
            configMap.put(url, item);
            for (ChainClient chainClient : newChainClients) {
                if (StringUtils.equals(chainClient.getUrl(), url)) {
                    chainClient.sort = sort;
                    existList.add(chainClient);
                }
            }
        }
        for (ChainClient client : existList) {
            configMap.remove(client.getUrl());
        }
        Iterator<ChainClient> iterator = newChainClients.iterator();
        while (iterator.hasNext()) {
            ChainClient client = iterator.next();
            for (ChainClient chainClient : existList) {
                if (chainClient.getUrl().equalsIgnoreCase(client.getUrl())) {
                    iterator.remove();
                    break;
                }
            }
        }
        for (ChainClient chainClient : newChainClients) {
            log.info("{} remove url : {}", getChainId(), chainClient.getUrl());
        }
        if (!configMap.isEmpty()) {
            for (Map.Entry<String, JSONObject> kv : configMap.entrySet()) {
                String enable = kv.getValue().getString("enable");
                boolean enableFlag = StringUtils.equals("1", enable) || StringUtils.equals("true", enable);
                if (enableFlag) {
                    log.info("create {} client : {}", this.getChainId(), kv.getKey());
                }
                ChainClient client = enableFlag ? create(kv.getValue()) : null;
                if (client != null) {
                    Integer sort = kv.getValue().getInteger("sort");
                    sort = sort == null ? 0 : sort;
                    client.sort = sort;
                    existList.add(client);
                }
            }
        }
        if (!configMap.isEmpty() || CollectionUtils.isNotEmpty(newChainClients)) {
            writeLock.lock();
            try {
                this.chainClients = existList;
            } finally {
                writeLock.unlock();
            }
        }
        // 将高优先级的客户端，排在前面
        writeLock.lock();
        try {
            if (CollectionUtils.isNotEmpty(chainClients) && chainClients.size() > 1) {
                ChainClient client = chainClients.peek();
                ChainClient selectedClient = null;
                for (int i = 1; i < chainClients.size(); i++) {
                    ChainClient chainClient = chainClients.get(i);
                    if (chainClient.markErrorTime < (System.currentTimeMillis() - chainClient.interval)) {
                        if (selectedClient == null) {
                            selectedClient = chainClient;
                        } else {
                            selectedClient = selectedClient.sort > chainClient.sort ? selectedClient : chainClient;
                        }
                    }
                }
                if (selectedClient != null && selectedClient.sort > client.sort) {
                    Iterator<ChainClient> it = chainClients.iterator();
                    while (it.hasNext()) {
                        ChainClient next = it.next();
                        if (next == selectedClient) {
                            it.remove();
                            break;
                        }
                    }
                    chainClients.push(selectedClient);
                    log.info("freshChainScanConfig 调整客户端优先级 {} : {}", getChainId(), chainClients.stream().map(ChainClient::getUrl).collect(Collectors.toList()));
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 格式化地址
     *
     * @param chainTransaction 交易
     */
    public void formatAddress(ChainTransaction chainTransaction) {

    }

    public boolean isValidTronAddress(String address) {
        return true;
    }

    public void speedUp(ChainTransaction chainTransaction) {

    }

    public List<ChainTransaction> queryWaitToChain(String chainId) {
        return blockTransactionManager.queryWaitToChain(chainId);
    }

    public abstract class ChainClient implements Closeable {
        @Getter
        private final String url;
        private final Object client;
        private int sort = 0;
        // 默认5分钟
        private final long interval = 300_000;

        private long markErrorTime = 0L;

        public ChainClient(String url, Object client) {
            this.url = url;
            this.client = client;
        }

        @SuppressWarnings("unchecked")
        public T getClient() {
            return (T) client;
        }
    }

    /**
     * 创建客户端
     *
     * @param item 创建客户端的配置
     * @return ChainClient
     */
    protected abstract ChainClient create(JSONObject item);


    /**
     * 获取客户端，从队列头开始获取
     *
     * @param excludeUrl 排除特定url的客户端
     * @return ChainClient
     */
    protected ChainClient getChainClient(Set<String> excludeUrl) {
        if (CollectionUtils.isEmpty(chainClients)) {
            throw new RuntimeException("client 没有配置 chain = " + chainId);
        }
        readLock.lock();
        try {
            if (CollectionUtils.isEmpty(excludeUrl)) {
                return chainClients.peek();
            } else {
                for (ChainClient chainClient : chainClients) {
                    if (!excludeUrl.contains(chainClient.getUrl())) {
                        return chainClient;
                    }
                }
            }
            return chainClients.peek();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 客户端执行出错，并将放置到队尾
     *
     * @param chainClient 客户端
     */
    protected void markClientError(ChainClient chainClient) {
        if (chainClient == null || chainClients == null || chainClients.size() <= 1) {
            return;
        }
        writeLock.lock();
        chainClient.markErrorTime = System.currentTimeMillis();
        try {
            boolean flag = false;
            Iterator<ChainClient> iterator = chainClients.iterator();
            while (iterator.hasNext()) {
                ChainClient client = iterator.next();
                if (client == chainClient) {
                    flag = true;
                    iterator.remove();
                    break;
                }
            }
            if (flag) {
                chainClients.add(chainClient);
                if (chainClients.size() > 1) {
                    log.info("markClientError 调整客户端优先级 {} : {}", getChainId(), chainClients.stream().map(ChainClient::getUrl).collect(Collectors.toList()));
                }
            }
        } finally {
            writeLock.unlock();
        }
    }


    @Setter
    @Getter
    public static class LastBlockInfo {
        private BigInteger blockNumber;
        private Date blockTime;
    }

    public abstract LastBlockInfo getLastBlockInfo() throws Exception;


    @Getter
    public static class ScanResult {
        private int count = 0;
        List<ChainTransaction> chainTransactions;
        private final BigInteger blockNumber;
        private final Date blockTime;

        public ScanResult(int count, List<ChainTransaction> chainTransactions, BigInteger blockNumber, Date blockTime) {
            this.count = count;
            this.chainTransactions = chainTransactions;
            this.blockNumber = blockNumber;
            this.blockTime = blockTime;
        }

    }

}
