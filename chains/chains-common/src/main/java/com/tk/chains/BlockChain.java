package com.tk.chains;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tk.chains.event.EventManager;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.exceptions.ChainParamsError;
import com.tk.chains.service.AddressChecker;
import com.tk.chains.service.BlockTransactionManager;
import com.tk.chains.service.CoinBalanceService;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.ChainWithdraw;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.mapper.ChainTransactionMapper;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.ChainTransactionService;
import com.tk.wallet.common.service.ChainWithdrawService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * louis
 */
public abstract class BlockChain<T> implements ApplicationContextAware {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    private static final int nThread = Runtime.getRuntime().availableProcessors() * 3;

    private static final AtomicInteger scanMultiIndex = new AtomicInteger(1);

    protected static volatile ThreadPoolExecutor threadPoolExecutor;

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
    protected ChainTransactionMapper chainTransactionMapper;
    protected ChainWithdrawService chainWithdrawService;

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
        this.chainTransactionMapper = applicationContext.getBean(ChainTransactionMapper.class);
        this.chainWithdrawService = applicationContext.getBean(ChainWithdrawService.class);
    }

    public void scanMultiThread(ChainScanConfig chainScanConfig, Supplier<Boolean> supplier) {
        if (threadPoolExecutor == null) {
            synchronized (BlockChain.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(nThread, nThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "scanMulti-" + scanMultiIndex.getAndIncrement()));
                }
            }
        }
        Integer multiThreadNumbers = chainScanConfig.getMultiThreadNumbers();
        multiThreadNumbers = multiThreadNumbers == null ? 2 : multiThreadNumbers;
        BigInteger blockNumber = chainScanConfig.getBlockNumber();
        BigInteger blockHeight = chainScanConfig.getBlockHeight();
        Integer delayBlocks = chainScanConfig.getDelayBlocks();
        BigInteger lastBlock = blockHeight.subtract(new BigInteger(delayBlocks + ""));
        int size = multiThreadNumbers * 3 * getChainClients().size();
        List<BigInteger> batchBlockNumbers = new ArrayList<>(size);
        while (lastBlock.compareTo(blockNumber) > 0) {
            blockNumber = blockNumber.add(new BigInteger("1"));
            batchBlockNumbers.add(blockNumber);
            if (batchBlockNumbers.size() >= size) {
                if (batchScan(batchBlockNumbers, chainScanConfig)) {
                    if (supplier != null && supplier.get()) {
                        log.info("stop_chain_2 : {}", getChainId());
                        return;
                    }
                    batchBlockNumbers = new ArrayList<>(size);
                } else {
                    return;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(batchBlockNumbers)) {
            batchScan(batchBlockNumbers, chainScanConfig);
        }
    }

    private boolean batchScan(List<BigInteger> batchBlockNumbers, ChainScanConfig chainScanConfig) {
        CountDownLatch countDownLatch = new CountDownLatch(batchBlockNumbers.size());
        ConcurrentHashMap<BigInteger, ScanResult> scanResultMap = new ConcurrentHashMap<>();
        for (BigInteger batchBlockNumber : batchBlockNumbers) {
            threadPoolExecutor.execute(() -> {
                try {
                    ScanResult scanResult = scan(chainScanConfig, batchBlockNumber, getRandomChainClient());
                    scanResultMap.put(batchBlockNumber, scanResult);
                } catch (Exception e) {
                    log.error("batchScan {}", batchBlockNumber, e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (Exception e) {
            return false;
        }
        for (BigInteger blockNumber : batchBlockNumbers) {
            ScanResult scanResult = scanResultMap.get(blockNumber);
            if (scanResult == null) {
                log.warn("并发拉取区块部分成功 lastBlockNumber = {}", blockNumber);
                return false;
            }
            try {
                if (CollectionUtils.isNotEmpty(scanResult.getTxList())) {
                    log.info("保存交易 blockNumber = {},\t size = {}", blockNumber, scanResult.getTxList().size());
                    BlockChain<?> blockChain = applicationContext.getBean(this.getChainId(), BlockChain.class);
                    blockChain.saveBlock(chainScanConfig, scanResult);
                }
                chainScanConfigService.updateBlockNumber(getChainId(), blockNumber);
            } catch (Exception e) {
                log.warn("saveBlock error {}", chainId, e);
                return false;
            }
        }
        return true;
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
            if (block >= 100) { // 单次最大扫描次数
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
                List<ScanResultTx> txList = scanResult.getTxList();
                if (CollectionUtils.isNotEmpty(txList)) {
                    log.info("保存交易 chainId = {},\tblockNumber = {},\t size = {}", chainId, blockNumber, scanResult.getTxList().size());
                    BlockChain<?> blockChain = applicationContext.getBean(this.getChainId(), BlockChain.class);
                    blockChain.saveBlock(chainScanConfig, scanResult);
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
            coinIn.merge(kv.getKey(), kv.getValue().multiply(new BigDecimal("-1")), (a, b) -> b.add(a));
        }
    }


    public int loadHash(String chainId, String hash) {
        Optional<ChainScanConfig> chainScanConfigOptional = chainScanConfigService.lambdaQuery().eq(ChainScanConfig::getChainId, chainId).oneOpt();
        return chainScanConfigOptional.map(scanConfig -> loadHash(scanConfig, hash)).orElse(0);
    }

    public int loadHash(ChainScanConfig chainScanConfig, String hash) {
        List<ChainTransaction> chainTransactions = getChainTransaction(chainScanConfig, hash, null);
        if (CollectionUtils.isNotEmpty(chainTransactions)) {
            List<ChainTransaction> transactions = chainTransactions.stream().filter(chainTransaction -> StringUtils.equalsIgnoreCase(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name())).collect(Collectors.toList());
            txSaveOrUpdate(hash, transactions);
            return transactions.size();
        }
        return 0;
    }

    /**
     * 指定区块高度扫描
     *
     * @param chainScanConfig 链配置
     * @param blockNumber     区块高度
     *                        block读取的行数，返回0 不会扫描下一个区块
     */
    public void scan(ChainScanConfig chainScanConfig, BigInteger blockNumber) throws Exception {
        scan(chainScanConfig, blockNumber, getRandomChainClient());
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
            this.transfer(chainScanConfig, kv.getValue());
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
     * @param chainScanConfig  链配置
     * @param chainTransaction 交易
     */
    public abstract void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction);


    public void sign(Object params, Consumer<JSONObject> consumer) {
        String signUrl = chainScanConfig.getSignUrl();
        try {
            JSONObject jsonObject = new RestTemplate().postForObject(signUrl, params, JSONObject.class);
            consumer.accept(jsonObject);
        } catch (Exception e) {
            log.error("sign : {}", chainId, e);
        }
    }

    // ==========================start 远程签名====================================
    protected String sign(Object signObj) {
        String signUrl = chainScanConfig.getSignUrl();
        try {
            ResponseEntity<JSONObject> response = new RestTemplate().postForEntity(signUrl, signObj, JSONObject.class);
            JSONObject jsonObject = response.getBody();
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
        List<ScanResultTx> txList;
        private final BigInteger blockNumber;
        private final Date blockTime;

        public ScanResult(int count, List<ScanResultTx> txList, BigInteger blockNumber, Date blockTime) {
            this.count = count;
            this.txList = txList;
            this.blockNumber = blockNumber;
            this.blockTime = blockTime;
        }
    }

    @Data
    public static class ScanResultTx {
        private String hash;
        private String transferId;
        private String gasPayer;
        private BigDecimal gas;
        List<ChainTransaction> chainTransactions;
        private String txStatus;

        public ScanResultTx(String hash, String transferId, String gasPayer, BigDecimal gas, List<ChainTransaction> chainTransactions, String txStatus) {
            this.hash = hash;
            this.gasPayer = gasPayer;
            this.gas = gas;
            this.chainTransactions = chainTransactions;
            this.transferId = transferId;
            this.txStatus = txStatus;
        }
    }

    // 代支付gas
    public String feePayer(String address) {
        return "";
    }

    @Transactional
    public void saveBlock(ChainScanConfig chainScanConfig, BlockChain.ScanResult scanResult) throws Exception {
        List<ChainTransaction> chainTransactions = scanResult.getTxList().stream().flatMap(tx -> tx.getChainTransactions().stream()).collect(Collectors.toList());
        beforeSaveChainTransactions(chainScanConfig, chainScanConfig.getChainId(), scanResult.getBlockNumber(), scanResult.getBlockTime(), chainTransactions);
        for (ScanResultTx tx : scanResult.getTxList()) {
            saveScanResultTx(tx, scanResult.getBlockTime(), scanResult.getBlockNumber());
        }
    }

    private void saveScanResultTx(ScanResultTx tx, Date blockTime, BigInteger blockNumber) {
        ChainWithdraw dbChainWithdraw = chainWithdrawService.lambdaQuery().eq(ChainWithdraw::getHash, tx.getHash()).last("limit 1").one();
        if (dbChainWithdraw == null && StringUtils.isNotBlank(tx.getTransferId())) {
            dbChainWithdraw = chainWithdrawService.lambdaQuery().eq(ChainWithdraw::getTransferId, tx.getTransferId()).last("limit 1").one();
        }
        if (dbChainWithdraw == null) {
            ChainTransaction chainTransaction = chainTransactionService.lambdaQuery().eq(ChainTransaction::getChainId, chainId).eq(ChainTransaction::getHash, tx.getHash()).last("limit 1").one();
            if (chainTransaction == null) {
                for (ChainTransaction transaction : tx.getChainTransactions()) {
                    chainTransactionService.save(transaction);
                }
            }
        } else {
            ChainWithdraw update = new ChainWithdraw();
            update.setId(dbChainWithdraw.getId());
            update.setStatus(tx.getTxStatus());
            update.setGasAddress(tx.getGasPayer());
            update.setGas(tx.getGas());
            update.setBlockTime(blockTime);
            update.setBlockHeight(blockNumber);
            chainWithdrawService.updateById(update);
            List<Long> ids = JSON.parseArray(dbChainWithdraw.getIds()).toJavaList(Long.class);
            for (Long id : ids) {
                ChainTransaction updateChainTransaction = new ChainTransaction();
                updateChainTransaction.setId(id);
                updateChainTransaction.setTxStatus(tx.getTxStatus());
                updateChainTransaction.setBlockNum(blockNumber);
                updateChainTransaction.setBlockTime(blockTime);
                chainTransactionService.updateById(updateChainTransaction);
            }
        }
    }

    /**
     * 链上区块扫到后
     * 1、与体现交易合并
     * 2、充值直接保存
     *
     * @param chainTransactions 交易
     */
    public void txSaveOrUpdate(String hash, List<ChainTransaction> chainTransactions) {
        LambdaQueryWrapper<ChainTransaction> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ChainTransaction::getHash, hash);
        lqw.eq(ChainTransaction::getChainId, chainId);
        List<ChainTransaction> list = chainTransactionService.list(lqw);
        // 提现交易，链上交易合并
        if (CollectionUtils.isNotEmpty(list)) {
            chainTransactionMapper.updateBlockNum(chainId, hash, chainTransactions.get(0).getBlockNum());
            chainTransactionMapper.updateGas(chainTransactions.get(0));
            chainTransactionMapper.updateTxStatus(chainId, hash, chainTransactions.get(0).getTxStatus(), chainTransactions.get(0).getFailCode(), chainTransactions.get(0).getMessage());
            if (eventManager != null) {
                eventManager.emit(new TransactionEvent(chainId, null, chainTransactionService.list(lqw)));
            }
        } else if (CollectionUtils.isEmpty(list)) {
            chainTransactionService.saveBatch(chainTransactions);
        }
    }

}
