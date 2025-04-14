package com.tk.chains.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tk.chains.BlockChain;
import com.tk.chains.event.ChainEventListener;
import com.tk.chains.event.Event;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.exceptions.DuplicateBusinessIdException;
import com.tk.chains.service.AggTaskService;
import com.tk.chains.service.ChainJobManager;
import com.tk.chains.service.ChainService;
import com.tk.chains.service.CoinBalanceService;
import com.tk.wallet.common.entity.*;
import com.tk.wallet.common.mapper.AggTaskMapper;
import com.tk.wallet.common.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AggTaskServiceImpl extends ServiceImpl<AggTaskMapper, AggTask> implements AggTaskService, ChainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AggTaskServiceImpl.class);

    @Autowired
    private WalletAddressService walletAddressService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private CoinBalanceService coinBalanceService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SymbolConfigService symbolConfigService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private ChainJobManager chainJobManager;
    @Autowired
    private ChainService chainService;

    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService;

    private final static String BUSINESS_ID_PRE = "aggTask";

    /**
     * 归集入口
     *
     * @param walletId
     * @param chainId
     * @param gasAddress    燃料地址, 假设燃料地址可以支付足够的地址，确认gas 会导致归集暂停
     * @param targetAddress 归集目标地址
     */
    public void agg(Integer walletId, String chainId, String gasAddress, String targetAddress) {
        this.agg(walletId, chainId, gasAddress, targetAddress, Lists.newArrayList(), Lists.newArrayList());
    }

    /**
     * 归集入口
     *
     * @param walletId      钱包商户id
     * @param chainId       链
     * @param gasAddress    燃料地址, 假设燃料地址可以支付足够的地址，确认gas 会导致归集暂停
     * @param targetAddress 归集目标地址
     * @param inContracts   需要归集的合约 , 主币的合约是 "" 空字符串
     * @param addresses     需要归集的地址 不传入，就是全部地址
     */
    public void agg(Integer walletId, String chainId, String gasAddress, String targetAddress, List<String> inContracts, List<String> addresses) {
        List<String> chainIds = chainJobManager.circuitBreaker();
        if (CollectionUtils.isNotEmpty(chainIds) && chainIds.contains(chainId)) {
            log.error("chain = {} 处于熔断状态", chainId);
            return;
        }
        long batchId = System.currentTimeMillis() / 1000;
        if (!addressService.owner(gasAddress, chainId)) {
            log.error("非管理地址，无法支付燃料 chain = {},\t{}", chainId, gasAddress);
            return;
        }
        BlockChain blockChain = applicationContext.getBean(chainId, BlockChain.class);
        ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(chainId);
        if (chainScanConfig == null) {
            log.error("找不到链的配置 chain = {}", chainId);
            return;
        }
        gasAddress = blockChain.formatAddress(gasAddress);
        targetAddress = blockChain.formatAddress(targetAddress);
        if (StringUtils.isBlank(gasAddress) || StringUtils.isBlank(targetAddress)) {
            log.error("地址配置错误 chain = {},\tgasAddress = {},\ttargetAddress = {}", chainId, gasAddress, targetAddress);
            return;
        }
        Optional<SymbolConfig> optionalCoinConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId).eq(SymbolConfig::getContractAddress, "").oneOpt();
        if (!optionalCoinConfig.isPresent()) {
            log.error("缺少主币配置 chain = {}", chainId);
            return;
        }
        int start = 0, limit = 100;
        do {
            LambdaQueryChainWrapper<WalletAddress> queryChainWrapper = walletAddressService.lambdaQuery();
            if (CollectionUtils.isNotEmpty(addresses)) {
                queryChainWrapper.in(WalletAddress::getAddress, addresses);
            }
            List<WalletAddress> walletAddresses = queryChainWrapper.eq(WalletAddress::getWalletId, walletId)
                    .notIn(WalletAddress::getAddress, Lists.newArrayList(gasAddress, targetAddress))
                    .inSql(WalletAddress::getAddress, "select address from coin_balance where balance > 0")
                    .gt(WalletAddress::getId, start)
                    .orderByAsc(WalletAddress::getId).last(" limit " + limit).list();
            if (CollectionUtils.isEmpty(walletAddresses)) {
                break;
            }
            for (WalletAddress walletAddress : walletAddresses) {
                start = walletAddress.getId();
                dealWalletAddress(blockChain, chainId, batchId, targetAddress, gasAddress, chainScanConfig, optionalCoinConfig.get(), inContracts, walletAddress.getAddress(), walletId);
            }
        } while (true);
    }


    private void dealWalletAddress(BlockChain<?> blockChain, String chainId, long batchId, String targetAddress, String gasAddress, ChainScanConfig chainScanConfig, SymbolConfig coinConfig, List<String> inContracts, String address, Integer walletId) {
        address = blockChain.formatAddress(address);
        if (StringUtils.isBlank(address)) {
            return;
        }
        boolean containCoin = CollectionUtils.isEmpty(inContracts) || inContracts.contains("");
        Optional<AggTask> lastBatchAggTaskOptional = this.lambdaQuery().eq(AggTask::getChainId, chainId).eq(AggTask::getFromAddress, address).orderByDesc(AggTask::getBatchId).last(" limit 1").oneOpt();
        if (lastBatchAggTaskOptional.isPresent()) {
            List<AggTask> lastAggTaskList = this.lambdaQuery().eq(AggTask::getChainId, chainId)
                    .eq(AggTask::getFromAddress, address).eq(AggTask::getBatchId, lastBatchAggTaskOptional.get().getBatchId()).in(AggTask::getStatus, AggTask.notEndList)
                    .list();
            if (CollectionUtils.isNotEmpty(lastAggTaskList)) {
                log.info("最新一次归集未完成 chain = {},\taddress = {},\tbatchId = {}", chainId, address, lastAggTaskList.get(0).getBatchId());
                return;
            }
        }
        LambdaQueryChainWrapper<CoinBalance> chainWrapper = coinBalanceService.lambdaQuery();
        List<CoinBalance> balances = chainWrapper.eq(CoinBalance::getChainId, chainId)
                .eq(CoinBalance::getAddress, address)
                .in(CoinBalance::getContractAddress, inContracts)
                .gt(CoinBalance::getBalance, 0).list();
        if (CollectionUtils.isEmpty(balances)) {
            return;
        }
        // 主币余额
        List<CoinBalance> coinBalances = coinBalanceService.lambdaQuery().eq(CoinBalance::getChainId, chainId)
                .eq(CoinBalance::getAddress, address)
                .eq(CoinBalance::getContractAddress, "")
                .gt(CoinBalance::getBalance, 0).list();
        // 主币余额; 主币余额 - 归集gas, 小于0的部分就是需要从 gasAddress 补充的金额
        BigDecimal coinBalance = CollectionUtils.isEmpty(coinBalances) ? BigDecimal.ZERO : coinBalances.get(0).getBalance();

        log.info("autoAgg_balances = {},\t{}", JSON.toJSONString(balances), JSON.toJSONString(inContracts));
        // 代币余额
        List<CoinBalance> tokenBalances = balances.stream()
                .filter(item -> StringUtils.isNotBlank(item.getContractAddress()))
                .filter(this::filterAgg)
                .filter(item -> this.filterMinimumAmount(item, walletId, blockChain, chainScanConfig)).collect(Collectors.toList());

        if (!tokenBalances.isEmpty()) { // 有代币，不归集主币，主币需要等到下一个归集任务
            Iterator<CoinBalance> coinBalanceIterator = tokenBalances.iterator();
            List<AggTask> tasks = new ArrayList<>();
            // 处理token 代币
            while (coinBalanceIterator.hasNext()) {
                CoinBalance tokenBalance = coinBalanceIterator.next();
                // 生成归集任务，并计算缺少的gas
                BigDecimal gas = blockChain.gas(chainScanConfig, tokenBalance.getCoinConfig());
                // 归集完成后剩余金额
                coinBalance = coinBalance.subtract(gas);
                AggTask aggTask = new AggTask();
                // 手续费
                aggTask.setGas(gas);
                // 转账金额
                aggTask.setAmount(tokenBalance.getBalance());
                aggTask.setContractAddress(tokenBalance.getContractAddress());
                aggTask.setFromAddress(tokenBalance.getAddress());
                aggTask.setToAddress(targetAddress);
                aggTask.setChainId(chainId);
                aggTask.setType(AggTask.TYPE_AGG);
                aggTask.setStatus(AggTask.STATUS_WAIT_PARENT_JOB);
                aggTask.setBatchId(batchId);
                aggTask.setContainCoin(containCoin ? 1 : 0);
                aggTask.setWalletId(walletId);
                tasks.add(aggTask);
            }
            // 处理 coin 主币
            AggTask firstAggTask = new AggTask();
            firstAggTask.setContainCoin(containCoin ? 1 : 0);
            // 本身有足够的gas
            if (coinBalance.compareTo(BigDecimal.ZERO) >= 0) {
                firstAggTask.setGas(BigDecimal.ZERO);
                firstAggTask.setAmount(BigDecimal.ZERO);
                firstAggTask.setFromAddress(gasAddress);
                firstAggTask.setToAddress(address);
                firstAggTask.setChainId(chainId);
                firstAggTask.setType(AggTask.TYPE_GAS);
                firstAggTask.setBatchId(batchId);
                firstAggTask.setStatus(AggTask.STATUS_SUCCESS);
                firstAggTask.setWalletId(walletId);
            } else {
                firstAggTask.setGas(blockChain.gas(chainScanConfig, coinConfig));
                firstAggTask.setAmount(coinBalance.multiply(new BigDecimal("-1")));
                firstAggTask.setFromAddress(gasAddress);
                firstAggTask.setToAddress(address);
                firstAggTask.setChainId(chainId);
                firstAggTask.setType(AggTask.TYPE_GAS);
                firstAggTask.setBatchId(batchId);
                firstAggTask.setStatus(AggTask.STATUS_WAIT_TO_CHAIN);
                firstAggTask.setWalletId(walletId);
            }
            applicationContext.getBean(AggTaskServiceImpl.class).saveAggTask(firstAggTask, tasks);
        } else {
            BigDecimal gas = blockChain.gas(chainScanConfig, coinConfig);
            CoinBalance coinBalanceEntity = balances.get(0);
            coinBalanceEntity.setBalance(coinBalance);
            coinBalanceEntity.setCoinConfig(coinConfig);
            // 没有代币余额，直接归集
            if (coinBalance.compareTo(BigDecimal.ZERO) > 0 && coinBalance.compareTo(gas) > 0 && filterMinimumAmount(coinBalanceEntity, walletId, blockChain, chainScanConfig)) {
                AggTask firstAggTask = new AggTask();
                firstAggTask.setWalletId(walletId);
                firstAggTask.setGas(gas);
                firstAggTask.setAmount(coinBalance.subtract(gas));
                firstAggTask.setFromAddress(address);
                firstAggTask.setToAddress(targetAddress);
                firstAggTask.setChainId(chainId);
                firstAggTask.setType(AggTask.TYPE_AGG);
                firstAggTask.setStatus(AggTask.STATUS_WAIT_TO_CHAIN);
                firstAggTask.setBatchId(batchId);
                applicationContext.getBean(AggTaskServiceImpl.class).saveAggTask(firstAggTask, Lists.newArrayList());
            }
        }
    }

    /**
     * 设置币种配置
     *
     * @param coinBalance
     * @return
     */
    private boolean filterAgg(CoinBalance coinBalance) {
        Optional<SymbolConfig> optionalCoinConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, coinBalance.getChainId())
                .eq(SymbolConfig::getContractAddress, coinBalance.getContractAddress()).eq(SymbolConfig::getStatus, 1).oneOpt();
        if (optionalCoinConfig.isPresent()) {
            SymbolConfig coinConfig = optionalCoinConfig.get();
            coinBalance.setCoinConfig(coinConfig);
            return true;
        }
        return false;
    }

    /**
     * 检查是否满足最小金额
     *
     * @param coinBalance
     * @return
     */
    private boolean filterMinimumAmount(CoinBalance coinBalance, Integer walletId, BlockChain blockChain, ChainScanConfig chainScanConfig) {
        BigDecimal gas = blockChain.gas(chainScanConfig, coinBalance.getCoinConfig());
        SymbolConfig symbolConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, coinBalance.getChainId()).eq(SymbolConfig::getContractAddress, coinBalance.getContractAddress()).one();
        if (symbolConfig == null) {
            return false;
        }
        Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                .eq(WalletSymbolConfig::getSymbolConfigId, symbolConfig.getId())
                .eq(WalletSymbolConfig::getWalletId, walletId)
                .last(" limit 1 ").oneOpt();
        if (optionalWalletSymbolConfig.isPresent()) {
            BigDecimal aggMinAmount = optionalWalletSymbolConfig.get().getAggMinAmount();
            return aggMinAmount == null || aggMinAmount.compareTo(coinBalance.getBalance()) <= 0 || coinBalance.getBalance().compareTo(gas) <= 0;
        }
        return true;
    }

    @Transactional
    public void saveAggTask(AggTask firstAggTask, List<AggTask> tasks) {
        if (this.save(firstAggTask)) {
            AggTask updateFirstAggTask = new AggTask();
            updateFirstAggTask.setId(firstAggTask.getId());
            updateFirstAggTask.setBusinessId(BUSINESS_ID_PRE + "-" + firstAggTask.getId());
            this.updateById(updateFirstAggTask);
            Long id = firstAggTask.getId();
            for (AggTask task : tasks) {
                this.save(task);
                AggTask updateTask = new AggTask();
                updateTask.setId(task.getId());
                updateTask.setBusinessId(BUSINESS_ID_PRE + "-" + task.getId());
                this.updateById(updateTask);
                // 保存依赖关系
                this.baseMapper.saveAggTaskDependency(task.getId(), id);
            }
        }
        updateAggTaskStatus(firstAggTask, false);
    }

    /**
     * @param aggTask
     * @param transferCoin token 归集完成后是否归集主币
     */
    private synchronized void updateAggTaskStatus(AggTask aggTask, boolean transferCoin) {
        this.updateById(aggTask);
        if (Objects.equals(aggTask.getStatus(), AggTask.STATUS_SUCCESS)) {
            this.baseMapper.updateNextTaskToChain(aggTask.getId());
            // 代币转账完成后，对主币 生成一次归集任务
            if (transferCoin && StringUtils.isNotBlank(aggTask.getContractAddress()) && Objects.equals(aggTask.getContainCoin(), 1)) {
                this.dealWalletAddress(applicationContext.getBean(aggTask.getChainId(), BlockChain.class),
                        aggTask.getChainId(),
                        aggTask.getBatchId(),
                        aggTask.getToAddress(),
                        "",
                        chainScanConfigService.getByChainId(aggTask.getChainId()),
                        symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, aggTask.getChainId()).eq(SymbolConfig::getContractAddress, "").one(),
                        Lists.newArrayList(""),
                        aggTask.getFromAddress(),
                        aggTask.getWalletId()
                );
                AggTask update = new AggTask();
                update.setId(aggTask.getId());
                update.setType(AggTask.TYPE_AGG);
                this.updateById(update);
            }
        } else if (Objects.equals(aggTask.getStatus(), AggTask.STATUS_FAIL)) {
            log.info("归集任务失败 taskId = {},\t{}", aggTask.getId(), JSON.toJSONString(aggTask));
            this.baseMapper.updateNextTaskFail(aggTask.getId());
        }
    }

    /**
     * 异步接收链上交易数据
     *
     * @param event
     */
    public void process(Event event) {
        List<ChainTransaction> list = new ArrayList<>();
        // 交易事件
        if (event instanceof TransactionEvent) {
            TransactionEvent transactionEvent = (TransactionEvent) event;
            ChainTransaction chainTransaction = transactionEvent.getChainTransaction();
            if (chainTransaction != null && StringUtils.isNotBlank(chainTransaction.getBusinessId()) && StringUtils.startsWith(chainTransaction.getBusinessId(), BUSINESS_ID_PRE)) {
                list.add(chainTransaction);
            }
            if (CollectionUtils.isNotEmpty(transactionEvent.getChainTransactions())) {
                list = transactionEvent.getChainTransactions().stream().filter(item -> (StringUtils.isNotBlank(item.getBusinessId()) && StringUtils.startsWith(item.getBusinessId(), BUSINESS_ID_PRE))).collect(Collectors.toList());
            }
        }
        for (ChainTransaction transaction : list) {
            Long taskId = Long.valueOf(transaction.getBusinessId().split("-")[1].trim());
            // 交易上了并且被扫描到了
            if (StringUtils.isNotBlank(transaction.getHash()) && Lists.newArrayList(ChainTransaction.TX_STATUS.SUCCESS.name(), ChainTransaction.TX_STATUS.PENDING.name()).contains(transaction.getTxStatus())) {
                AggTask aggTask = this.getById(taskId);
                if (aggTask != null) {
                    aggTask.setStatus(AggTask.STATUS_SUCCESS);
                    aggTask.setMtime(new Date());
                    updateAggTaskStatus(aggTask, StringUtils.equals(ChainTransaction.TX_STATUS.SUCCESS.name(), transaction.getTxStatus()));
                }
            } else if (StringUtils.equals(transaction.getTxStatus(), ChainTransaction.TX_STATUS.FAIL.name())) { // 没有上链重新提交
                AggTask aggTask = this.getById(taskId);
                if (aggTask != null) {
                    if (StringUtils.isBlank(transaction.getHash())) {
                        int retryCount = aggTask.getRetryCount() == null ? 0 : aggTask.getRetryCount();
                        retryCount++;
                        aggTask.setRetryCount(retryCount);
                        if (retryCount > 4) {
                            aggTask.setStatus(AggTask.STATUS_FAIL);
                        } else {
                            aggTask.setStatus(AggTask.STATUS_WAIT_TO_CHAIN);
                            // 30s 后重试
                            aggTask.setRunTime(new Date(System.currentTimeMillis() + (1000 * 30)));
                        }
                        aggTask.setMtime(new Date());
                        updateAggTaskStatus(aggTask, false);
                    } else {
                        aggTask.setStatus(AggTask.STATUS_FAIL);
                        updateAggTaskStatus(aggTask, false);
                    }
                }
            }
        }
    }

    @Scheduled(initialDelay = 10000, fixedRate = 10000)
    public void taskToChain() {
        List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
        for (ChainScanConfig chainScanConfig : chainScanConfigs) {
            do {
                List<AggTask> list = this.lambdaQuery().le(AggTask::getRunTime, new Date()).eq(AggTask::getChainId, chainScanConfig.getChainId()).eq(AggTask::getStatus, AggTask.STATUS_WAIT_TO_CHAIN).orderByAsc(AggTask::getId)
                        .last("limit 100").list();
                if (CollectionUtils.isEmpty(list)) {
                    break;
                }
                for (AggTask aggTask : list) {
                    ChainTransaction transaction = new ChainTransaction();
                    transaction.setChainId(chainScanConfig.getChainId());
                    transaction.setBusinessId(BUSINESS_ID_PRE + "-" + aggTask.getId());
                    transaction.setGas(aggTask.getGas());
                    transaction.setFromAddress(aggTask.getFromAddress());
                    transaction.setToAddress(aggTask.getToAddress());
                    transaction.setContract(aggTask.getContractAddress());
                    transaction.setAmount(aggTask.getAmount());
                    SymbolConfig coinConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getContractAddress, StringUtils.isBlank(aggTask.getContractAddress()) ? "" : aggTask.getContractAddress())
                            .eq(SymbolConfig::getBaseSymbol, chainScanConfig.getChainId()).one();
                    transaction.setGasConfig(coinConfig.getConfigJson());
                    try {
                        chainService.addChainTransaction(transaction);
                        aggTask.setStatus(AggTask.STATUS_PENDING);
                        updateAggTaskStatus(aggTask, false);
                    } catch (DuplicateBusinessIdException duplicateBusinessIdException) {
                        log.error("重复的业务主键 {}", transaction.getBusinessId());
                        aggTask.setStatus(AggTask.STATUS_FAIL);
                        updateAggTaskStatus(aggTask, false);
                    }
                }
            } while (true);
        }
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 120_000)
    public void autoAgg() {
        List<Integer> walletIds = walletSymbolConfigService.distinctWalletIds();
        if (CollectionUtils.isNotEmpty(walletIds)) {
            for (Integer walletId : walletIds) {
                List<ChainScanConfig> chainScanConfigs = chainJobManager.getChainScanConfigs();
                for (ChainScanConfig chainScanConfig : chainScanConfigs) {
                    Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                            .eq(WalletSymbolConfig::getWalletId, walletId)
                            .inSql(WalletSymbolConfig::getSymbolConfigId, "select id from symbol_config where base_symbol = '" + chainScanConfig.getChainId() + "'")
                            .eq(WalletSymbolConfig::getAggPolice, 0).last(" limit 1").oneOpt();  // 至少存在一个币，是自动归集
                    if (optionalWalletSymbolConfig.isPresent() && StringUtils.isNotBlank(optionalWalletSymbolConfig.get().getAggAddress())) {

                        SymbolConfig symbolConfig = symbolConfigService.lambdaQuery()
                                .eq(SymbolConfig::getBaseSymbol, chainScanConfig.getChainId()).eq(SymbolConfig::getContractAddress, "").one();

                        WalletSymbolConfig mainWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                                .eq(WalletSymbolConfig::getWalletId, walletId)
                                .eq(WalletSymbolConfig::getSymbolConfigId, symbolConfig.getId())
                                .one(); // 归集地址配置在主币
                        if (mainWalletSymbolConfig == null || StringUtils.isBlank(mainWalletSymbolConfig.getAggAddress()) ||
                                StringUtils.isBlank(mainWalletSymbolConfig.getEnergyAddress())) {
                            continue;
                        }
                        // 找出所有自动归集的币
                        List<WalletSymbolConfig> list = walletSymbolConfigService.lambdaQuery()
                                .eq(WalletSymbolConfig::getWalletId, walletId)
                                .inSql(WalletSymbolConfig::getSymbolConfigId, "select id from symbol_config where base_symbol = '" + chainScanConfig.getChainId() + "'")
                                .eq(WalletSymbolConfig::getAggPolice, 0)
                                .list();
                        List<Integer> symbolIdList = list.stream().filter(item -> Objects.equals(item.getAggPolice(), 0)).map(WalletSymbolConfig::getSymbolConfigId).collect(Collectors.toList());
                        log.info("autoAgg : {},\t{},\t{},\t{}", chainScanConfig.getChainId(), walletId, symbolIdList, symbolIdList.size());
                        List<SymbolConfig> contractList = symbolConfigService.lambdaQuery().in(SymbolConfig::getId, symbolIdList).list();
                        if (CollectionUtils.isEmpty(contractList)) {
                            continue;
                        }

                        this.agg(walletId, chainScanConfig.getChainId(), mainWalletSymbolConfig.getEnergyAddress(), mainWalletSymbolConfig.getAggAddress(),
                                contractList.stream().map(SymbolConfig::getContractAddress).collect(Collectors.toList()),
                                Lists.newArrayList());
                    }
                }
            }
        }
    }

}
