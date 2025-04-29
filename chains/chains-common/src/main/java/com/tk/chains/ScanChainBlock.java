package com.tk.chains;

import com.tk.chains.event.BlockHeightUpdate;
import com.tk.chains.event.EventManager;
import com.tk.chains.event.ConfirmEvent;
import com.tk.chains.service.AggTaskService;
import com.tk.chains.service.BlockTransactionManager;
import com.tk.chains.service.ChainJobManager;
import com.tk.chains.service.ChainService;
import com.tk.wallet.common.entity.*;
import com.tk.wallet.common.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 扫描区块
 */
@EnableScheduling
public class ScanChainBlock implements ApplicationContextAware, SmartLifecycle {


    private volatile boolean start = false;
    @Value("${chainIds:}")
    private String chainIds;
    private static final Logger log = LoggerFactory.getLogger(ScanChainBlock.class);

    private static final AtomicInteger chainBlockIndex = new AtomicInteger(0);

    private static final ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 3, Runtime.getRuntime().availableProcessors() * 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "chain-block-" + chainBlockIndex.incrementAndGet()));

    private static final ConcurrentHashMap<String, ReentrantLock> locksWithDraw = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> locksScan = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> removeChainList = new ConcurrentHashMap<>();
    private ChainScanConfigService chainScanConfigService;
    private EventManager eventManager;
    private ChainTransactionService chainTransactionService;
    private BlockTransactionManager blockTransactionManager;
    private ChainJobManager chainJobManager;
    private AggTaskService aggTaskService;
    private AggQueueService aggQueueService;
    private WalletSymbolConfigService walletSymbolConfigService;
    private SymbolConfigService symbolConfigService;
    private ApplicationContext applicationContext;
    private WalletWithdrawService walletWithdrawService;
    private ChainService chainService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        chainScanConfigService = applicationContext.getBean(ChainScanConfigService.class);
        eventManager = applicationContext.getBean(EventManager.class);
        chainTransactionService = applicationContext.getBean(ChainTransactionService.class);
        chainJobManager = applicationContext.getBean(ChainJobManager.class);
        aggTaskService = applicationContext.getBean(AggTaskService.class);
        aggQueueService = applicationContext.getBean(AggQueueService.class);
        walletSymbolConfigService = applicationContext.getBean(WalletSymbolConfigService.class);
        symbolConfigService = applicationContext.getBean(SymbolConfigService.class);
        blockTransactionManager = applicationContext.getBean(BlockTransactionManager.class);
        walletWithdrawService = applicationContext.getBean(WalletWithdrawService.class);
        chainService = applicationContext.getBean(ChainService.class);
    }

    /**
     * 扫描区块
     */
    @Scheduled(initialDelay = 5000, fixedRate = 3000)
    public void scan() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        if (CollectionUtils.isNotEmpty(chainScanConfigs)) {
            for (ChainScanConfig chainScanConfig : chainScanConfigs) {
                BlockChain<?> blockChain = applicationContext.getBean(chainScanConfig.getChainId(), BlockChain.class);
                if (blockChain.getMainCoinConfig() == null) {
                    return;
                }
                if (chainScanConfig.getBlockHeight().compareTo(chainScanConfig.getBlockNumber()) > 0) {
                    if (!locksScan.containsKey(chainScanConfig.getChainId())) {
                        locksScan.putIfAbsent(chainScanConfig.getChainId(), new ReentrantLock());
                    }
                    if (StringUtils.equalsIgnoreCase(chainScanConfig.getMultiThread(), "true")) {
                        executorService.execute(() -> {
                            ReentrantLock lock = locksScan.get(chainScanConfig.getChainId());
                            if (chainScanConfig.checkScan() && lock.tryLock()) {
                                try {
                                    blockChain.updateBalance(true);// 扫快的时候，会更新账本
                                    blockChain.scanMultiThread(chainScanConfig, () -> isStopChain(chainScanConfig.getChainId()));
                                } catch (Exception e) {
                                    log.error("MultiThread_scan " + chainScanConfig.getChainId() + ",\t" + chainScanConfig.getBlockNumber(), e);
                                } finally {
                                    lock.unlock();
                                }
                            }
                        });
                    } else {
                        executorService.execute(() -> {
                            ReentrantLock lock = locksScan.get(chainScanConfig.getChainId());
                            if (chainScanConfig.checkScan() && lock.tryLock()) {
                                try {
                                    blockChain.updateBalance(true);// 扫快的时候，会更新账本
                                    blockChain.scan(chainScanConfig, () -> isStopChain(chainScanConfig.getChainId()));
                                } catch (Exception e) {
                                    log.error("scan {},\t{}", chainScanConfig.getChainId(), chainScanConfig.getBlockNumber(), e);
                                } finally {
                                    lock.unlock();
                                }
                            }
                        });
                    }
                }
            }
        }
    }


    /**
     * 监控区块高度
     */
    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void monitorBlockHeight() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        if (CollectionUtils.isNotEmpty(chainScanConfigs)) {
            for (ChainScanConfig chainScanConfig : chainScanConfigs) {
                long blockInterval = chainScanConfig.getBlockInterval() == null ? 0 : chainScanConfig.getBlockInterval();
                if (System.currentTimeMillis() <= (chainScanConfig.getLastBlockTime().getTime() + (blockInterval * 1000))) {
                    continue;
                }
                BlockChain<?> blockChain = applicationContext.getBean(chainScanConfig.getChainId(), BlockChain.class);
                if (!blockChain.configIsOk()) {
                    log.warn("config is not ok  chainId = {}", chainScanConfig.getChainId());
                    return;
                }
                try {
                    BlockChain.LastBlockInfo lastBlockInfo = blockChain.getLastBlockInfo();
                    BigInteger blockHeight = lastBlockInfo.getBlockNumber();
                    if (blockHeight.compareTo(BigInteger.ZERO) <= 0) {
                        continue;
                    }
                    if (chainScanConfig.getBlockNumber().compareTo(BigInteger.ZERO) <= 0 && chainScanConfig.getBlockHeight().compareTo(BigInteger.ZERO) <= 0) {
                        log.info("init {} BlockNumber, BlockHeight({})", chainScanConfig.getChainId(), blockHeight);
                        chainScanConfigService.updateBlockNumber(chainScanConfig.getChainId(), blockHeight);
                    }
                    if (chainScanConfig.getBlockHeight().compareTo(blockHeight) < 0) {
                        ChainScanConfig updateChainScanConfig = new ChainScanConfig();
                        updateChainScanConfig.setLastBlockTime(lastBlockInfo.getBlockTime());
                        updateChainScanConfig.setBlockHeight(lastBlockInfo.getBlockNumber());
                        updateChainScanConfig.setChainId(chainScanConfig.getChainId());
                        chainScanConfigService.updateById(updateChainScanConfig);
                        if (log.isDebugEnabled()) {
                            log.debug("update {} BlockHeight({})", chainScanConfig.getChainId(), blockHeight);
                        }
                        boolean hasPendingTransaction = blockTransactionManager.hasPendingChainTransaction(chainScanConfig.getChainId(), blockHeight);
                        if (hasPendingTransaction) {
                            eventManager.emit(new ConfirmEvent(chainScanConfig.getChainId(), blockHeight));
                        }
                        eventManager.emit(new BlockHeightUpdate(chainScanConfig.getChainId(), blockHeight));
                    }
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        log.error(String.format("scan : %s", chainScanConfig.getChainId()), e);
                    }
                }
            }
        }
    }

    //读取等待上链的数据

    /**
     * 扫描 ChainTransaction 表 并将交易上链
     */
    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void transactionToChain() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            List<WalletWithdraw> walletWithdraws = walletWithdrawService.lambdaQuery().eq(WalletWithdraw::getBaseSymbol, chainScanConfig.getChainId()).eq(WalletWithdraw::getStatus, 2).list();
            if (CollectionUtils.isNotEmpty(walletWithdraws)) {
                for (WalletWithdraw walletWithdraw : walletWithdraws) {
                    ChainTransaction chainTransaction = new ChainTransaction();
                    chainTransaction.setChainId(chainScanConfig.getChainId());
                    chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.INIT.name());
                    chainTransaction.setBusinessId("withdraw-" + walletWithdraw.getId());
                    chainTransaction.setFromAddress(walletWithdraw.getAddressFrom());
                    chainTransaction.setToAddress(walletWithdraw.getAddressTo());
                    chainTransaction.setAmount(walletWithdraw.getAmount());

                    SymbolConfig symbolConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainScanConfig.getChainId()).eq(SymbolConfig::getSymbol, walletWithdraw.getSymbol()).one();
                    chainTransaction.setTokenSymbol(symbolConfig.getTokenSymbol());
                    chainTransaction.setSymbol(symbolConfig.getSymbol());
                    chainTransaction.setContract(symbolConfig.getContractAddress());
                    chainTransaction.setGasConfig(symbolConfig.getConfigJson());

                    try {
                        chainService.addChainTransaction(chainTransaction);
                        WalletWithdraw update = new WalletWithdraw();
                        update.setId(walletWithdraw.getId());
                        update.setStatus(2);
                        walletWithdrawService.updateById(update);
                    } catch (Exception e) {
                        log.error("addChainTransaction", e);
                    }
                }
            }
        }
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            String chainId = chainScanConfig.getChainId();
            if (!locksWithDraw.containsKey(chainId)) {
                locksWithDraw.putIfAbsent(chainId, new ReentrantLock());
            }
            executorService.execute(() -> {
                ReentrantLock lock = locksWithDraw.get(chainId);
                if (lock.tryLock()) {
                    try {
                        BlockChain<?> blockChain = applicationContext.getBean(chainScanConfig.getChainId(), BlockChain.class);
                        if (!blockChain.configIsOk()) {
                            log.warn("getMainCoinConfig({}) is null ", chainScanConfig.getChainId());
                            return;
                        }
                        List<ChainTransaction> chainTransactions = blockChain.queryWaitToChain(chainId);
                        if (CollectionUtils.isNotEmpty(chainTransactions)) {
                            blockChain.groupTransfer(chainScanConfig, chainTransactions);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
    }

    // 定期更新链上配置，刷新客户端配置
    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    @PostConstruct
    public void freshChainScanConfig() {
        if (StringUtils.isBlank(chainIds)) {
            return;
        }
        String[] chainList = chainIds.trim().split(",");
        List<ChainScanConfig> chainScanConfigs = chainScanConfigService.lambdaQuery().in(ChainScanConfig::getChainId, Arrays.asList(chainList)).list();
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            freshChainScanConfig(chainScanConfig);
        }
    }


    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        List<SymbolConfig> list = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainScanConfig.getChainId()).list();
        chainScanConfig.setCoinConfigs(list);
        BlockChain<?> blockChain = applicationContext.getBean(chainScanConfig.getChainId(), BlockChain.class);
        try {
            blockChain.freshChainScanConfig(chainScanConfig);
        } catch (Exception e) {
            log.warn("doFreshChainScanConfig", e);
        }
    }

    /**
     * 多个jvm 部署的情况
     * 1、启动jvm时候，生成taskId
     * 2、尝试将taskId 更新到 ChainScanConfig( task_update_time 超过 1分钟没有更新 )
     * 3、ChainScanConfig的taskId 和当前的taskId 相等，当前jvm 获得对应链的 扫描区块，交易上链,监控区块高度 的job
     */
    // 定期更新锁时间
    @Scheduled(initialDelay = 5000, fixedRate = 15000)
    public void watchDog() {
        if (StringUtils.isBlank(chainIds)) {
            return;
        }
        String[] chainList = chainIds.trim().split(",");
        chainScanConfigService.taskUpdateTime(chainJobManager.getTaskId(), chainIds);
        // 清理下线的jvm,并返回当前运行的jvm 个数
        Integer hosts = chainScanConfigService.hosts(chainIds);
        List<ChainScanConfig> chainScanConfigs = chainScanConfigService.lambdaQuery().eq(ChainScanConfig::getStatus, 1).in(ChainScanConfig::getChainId, Arrays.asList(chainList)).orderByAsc(ChainScanConfig::getChainId).list();
        int max = (chainScanConfigs.size() / hosts) + 1;
        int current = (int) chainScanConfigs.stream().filter(item -> StringUtils.equals(chainJobManager.getTaskId(), item.getTaskId())).count();
        if (chainScanConfigs.size() > current && current < max) {
            List<String> chainIds = chainScanConfigService.updateTaskId(chainJobManager.getTaskId(), Math.min(max - current, chainScanConfigs.size() - current), Arrays.asList(chainList));
            if (CollectionUtils.isNotEmpty(chainIds)) {
                for (String chainId : chainIds) {
                    removeChainList.remove(chainId);
                    Optional<ChainScanConfig> chainScanConfigOptional = chainScanConfigService.lambdaQuery().eq(ChainScanConfig::getStatus, 1).eq(ChainScanConfig::getChainId, chainId).oneOpt();
                    chainScanConfigOptional.ifPresent(this::freshChainScanConfig);
                }
                log.info("add chainId = {}", chainIds);
            }
        } else if (current > max) {
            int count = current - max;
            for (int i = 0; i < count; i++) {
                removeChainList.put(chainScanConfigs.get(i).getChainId(), 0);
            }
            log.info("wait remove chainId = {}", removeChainList.keySet());
        }
    }

    // 每次扫快结束后，回调该方法
    private boolean isStopChain(String chainId) {
        boolean flag = removeChainList.containsKey(chainId);
        if (flag) {
            chainScanConfigService.removeTaskId(chainId, chainJobManager.getTaskId());
            removeChainList.remove(chainId);
            log.info("do_remove_chainId = {}", chainId);
            return true;
        }
        ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(chainId);
        if (StringUtils.equals(chainScanConfig.getStatus(), "0")) {
            chainScanConfigService.removeTaskId(chainId, chainJobManager.getTaskId());
            log.info("stop_chain : {}", chainId);
        }
        return StringUtils.equals(chainScanConfig.getStatus(), "0");
    }

    /**
     * 交易断路器
     * 过去 5 分钟，链上交易失败
     */
    @Scheduled(initialDelay = 5000, fixedRate = 10000)
    public void circuitBreaker() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        List<String> chainIds = chainJobManager.circuitBreaker();
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            if (CollectionUtils.isNotEmpty(chainIds) && chainIds.contains(chainScanConfig.getChainId())) {
                continue;
            }
            Integer errorCount = chainTransactionService.lambdaQuery().eq(ChainTransaction::getChainId, chainScanConfig.getChainId()).isNotNull(ChainTransaction::getBusinessId).eq(ChainTransaction::getTxStatus, ChainTransaction.TX_STATUS.FAIL.name()).gt(ChainTransaction::getMtime, new Date(System.currentTimeMillis() - (1000 * 60 * 10))).count();
            if (errorCount != null && errorCount >= 5) {
                log.error("circuitBreaker chain = {}", chainScanConfig.getChainId());
                chainJobManager.circuitBreaker(chainScanConfig.getChainId());
            }
        }
    }

    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void scanAggQueue() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            List<AggQueue> aggQueues = aggQueueService.lambdaQuery().eq(AggQueue::getChainId, chainScanConfig.getChainId()).orderByAsc(AggQueue::getId).last(" limit 100").list();
            for (AggQueue aggQueue : aggQueues) {
                SymbolConfig config = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainScanConfig.getChainId()).eq(SymbolConfig::getContractAddress, "").one();
                Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, aggQueue.getWalletId()).eq(WalletSymbolConfig::getSymbolConfigId, config.getId()).isNotNull(WalletSymbolConfig::getAggAddress).isNotNull(WalletSymbolConfig::getEnergyAddress).last(" limit 1").oneOpt();
                if (optionalWalletSymbolConfig.isPresent()) {
                    String aggAddress = optionalWalletSymbolConfig.get().getAggAddress();
                    String energyAddress = optionalWalletSymbolConfig.get().getEnergyAddress();
                    String symbolList = aggQueue.getSymbolList();
                    List<String> contractList = new ArrayList<>();
                    if (StringUtils.isNotBlank(symbolList)) {
                        for (String id : symbolList.split(",")) {
                            if (StringUtils.isNumeric(id.trim())) {
                                SymbolConfig symbolConfig = symbolConfigService.getById(Integer.valueOf(id.trim()));
                                if (symbolConfig != null) {
                                    contractList.add(symbolConfig.getContractAddress());
                                }
                            }
                        }
                    }
                    if (CollectionUtils.isEmpty(contractList)) {
                        List<Integer> collect = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, aggQueue.getWalletId()).inSql(WalletSymbolConfig::getSymbolConfigId, "select id from symbol_config where base_symbol = '" + chainScanConfig.getChainId() + "'").list().stream().map(WalletSymbolConfig::getSymbolConfigId).collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(collect)) {
                            contractList = symbolConfigService.lambdaQuery().in(SymbolConfig::getId, collect).list().stream().map(SymbolConfig::getContractAddress).collect(Collectors.toList());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(contractList)) {
                        aggTaskService.agg(aggQueue.getWalletId(), aggQueue.getChainId(), energyAddress, aggAddress, contractList, new ArrayList<>(0));
                    }
                }
                aggQueueService.removeById(aggQueue.getId());
            }
        }
    }

    @Override
    public void start() {
        start = true;
    }

    @Override
    public void stop() {
        chainJobManager.stop();
        executorService.shutdown();
        start = false;
    }

    @Override
    public boolean isRunning() {
        return start;
    }

}