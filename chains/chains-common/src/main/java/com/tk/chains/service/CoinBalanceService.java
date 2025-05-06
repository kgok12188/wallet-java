package com.tk.chains.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.chains.BlockChain;
import com.tk.chains.event.BalanceEvent;
import com.tk.chains.event.EventManager;
import com.tk.wallet.common.entity.*;
import com.tk.wallet.common.mapper.CoinBalanceMapper;
import com.tk.wallet.common.service.AddressService;
import com.tk.wallet.common.service.ChainScanConfigService;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletAddressService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CoinBalanceService extends ServiceImpl<CoinBalanceMapper, CoinBalance> {

    private static final Logger log = LoggerFactory.getLogger(CoinBalanceService.class);

    @Autowired
    private EventManager eventManager;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private AddressService addressService;
    @Autowired
    private WalletAddressService walletAddressService;
    @Autowired
    private ChainScanConfigService chainScanConfigService;

    @Autowired
    private SymbolConfigService symbolConfigService;
    //
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public BigDecimal incrementAmount(Long id, BigDecimal value) {
        this.baseMapper.incrementAmount(id, value);
        return getById(id).getBalance();
    }


    @Transactional
    public void updateBalance(ChainScanConfig chainScanConfig, Map<String, BigDecimal> coinIn, Map<String, Map<String, BigDecimal>> tokenIn, BigInteger blockHeight, Date blockTime, BlockChain blockChain) throws Exception {
        reentrantLock.lock();
        try {
            updateBalance(chainScanConfig, coinIn, blockHeight, blockTime, "", blockChain);
            for (Map.Entry<String, Map<String, BigDecimal>> kv : tokenIn.entrySet()) {
                String contractAddress = kv.getKey();
                updateBalance(chainScanConfig, kv.getValue(), blockHeight, blockTime, contractAddress, blockChain);
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    private void updateBalance(ChainScanConfig chainScanConfig, Map<String, BigDecimal> coinIn, BigInteger blockHeight, Date blockTime, String contractAddress, BlockChain blockChain) throws Exception {
        for (Map.Entry<String, BigDecimal> kv : coinIn.entrySet()) {
            if (addressService.owner(kv.getKey(), chainScanConfig.getChainId())) {
                Optional<CoinBalance> coinBalanceOptional = this.lambdaQuery().eq(CoinBalance::getAddress, kv.getKey()).eq(CoinBalance::getContractAddress, contractAddress).eq(CoinBalance::getChainId, chainScanConfig.getChainId()).oneOpt();
                if (coinBalanceOptional.isPresent()) {
                    CoinBalance coinBalance = coinBalanceOptional.get();
                    if (coinBalance.getBlockHeight().compareTo(blockHeight) <= 0 && coinBalance.getBlockTime().getTime() < blockTime.getTime()) {
                        // 增加余额
                        log.info("incrementAmount : chain = {},\tcontract = {},\taddress = {},\t balance = {}", chainScanConfig.getChainId(), contractAddress, coinBalance.getAddress(), kv.getValue());
                        BigDecimal balance = this.incrementAmount(coinBalance.getId(), kv.getValue());
                        if (balance.compareTo(BigDecimal.ZERO) < 0) {
                            log.warn("balance_update_error : chain = {},\tcontract = {},\taddress = {},\t balance = {}", chainScanConfig.getChainId(), contractAddress, coinBalance.getAddress(), balance);
                            initCoinBalance(chainScanConfig, kv.getKey(), contractAddress, blockChain);
                        }
                    } else if (Objects.equals(coinBalance.getBlockHeight(), blockHeight) && !Objects.equals(coinBalance.getBlockTime().getTime(), blockTime.getTime())) {
                        initCoinBalance(chainScanConfig, kv.getKey(), contractAddress, blockChain);
                    } else if (!Objects.equals(coinBalance.getBlockHeight(), blockHeight) && Objects.equals(coinBalance.getBlockTime().getTime(), blockTime.getTime())) {
                        initCoinBalance(chainScanConfig, kv.getKey(), contractAddress, blockChain);
                    }
                } else {
                    initCoinBalance(chainScanConfig, kv.getKey(), contractAddress, blockChain);
                }
                Optional<CoinBalance> coinBalance = this.lambdaQuery().eq(CoinBalance::getAddress, kv.getKey()).eq(CoinBalance::getContractAddress, contractAddress).eq(CoinBalance::getChainId, chainScanConfig.getChainId()).oneOpt();
                coinBalance.ifPresent(balance -> eventManager.emit(new BalanceEvent(balance)));
            }
        }
    }

    /*
     * 第一次保存，会同步链上余额
     * */
    public void initCoinBalance(ChainScanConfig chainScanConfig, String address, String contractAddress, BlockChain<?> blockChain) throws Exception {
        BlockChain.LastBlockInfo lastBlockInfo = blockChain.getLastBlockInfo();
        BigDecimal balance = StringUtils.isBlank(contractAddress) ? blockChain.getBalance(chainScanConfig, address) : blockChain.getTokenBalance(chainScanConfig, address, contractAddress);
        CoinBalance coinBalance = new CoinBalance();
        coinBalance.setBalance(balance);
        coinBalance.setAddress(address);
        coinBalance.setContractAddress(contractAddress);
        coinBalance.setChainId(blockChain.getChainId());
        SymbolConfig coinConfig = StringUtils.isBlank(contractAddress) ? blockChain.getMainCoinConfig() : blockChain.getTokenConfig(contractAddress);
        coinBalance.setCoin(coinConfig.getTokenSymbol());
        coinBalance.setApiCoin(coinConfig.getSymbol());
        coinBalance.setMtime(new Date());
        coinBalance.setBlockHeight(lastBlockInfo.getBlockNumber());
        coinBalance.setBlockTime(lastBlockInfo.getBlockTime());
        log.info("addCoinBalance :{}", JSON.toJSONString(coinBalance));
        this.save(coinBalance);

    }

    @Transactional
    public void initCoinBalance(String chainId, String address, String contractAddress) throws Exception {
        reentrantLock.lock();
        try {
            BlockChain<?> blockChain = applicationContext.getBean(chainId, BlockChain.class);
            if (blockChain.isValidTronAddress(address) && (StringUtils.isBlank(contractAddress) || blockChain.isValidTronAddress(contractAddress))) {
                contractAddress = StringUtils.isBlank(contractAddress) ? "" : contractAddress;
                Optional<CoinBalance> coinBalanceOptional = this.lambdaQuery().eq(CoinBalance::getAddress, address).eq(CoinBalance::getContractAddress, contractAddress).eq(CoinBalance::getChainId, chainId).oneOpt();
                coinBalanceOptional.ifPresent(coinBalance -> this.removeById(coinBalance.getId()));
                ChainScanConfig chainScanConfig = chainScanConfigService.getByChainId(chainId);
                initCoinBalance(chainScanConfig, address, contractAddress, blockChain);
            } else {
                log.error("非法地址 chain = {},\taddress = {},\tcontractAddress = {}", chainId, address, contractAddress);
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 同步某个地址的所有余额
     *
     * @param chainId
     * @param address
     */
    public void initCoinBalance(String chainId, String address) throws Exception {
        List<SymbolConfig> coinConfigs = symbolConfigService.lambdaQuery().eq(SymbolConfig::getStatus, 1).eq(SymbolConfig::getBaseSymbol, chainId).list();
        for (SymbolConfig coinConfig : coinConfigs) {
            applicationContext.getBean(CoinBalanceService.class).initCoinBalance(chainId, address, coinConfig.getContractAddress());
            Thread.sleep(1000);
        }
    }

    /**
     * 某条链的所有地址，同步链上所有（coin和token）余额
     *
     * @param chainId
     */
    public void initCoinBalance(Integer walletId, String chainId) throws Exception {
        long start = 0;
        int limit = 200;
        do {
            List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery()
                    .eq(WalletAddress::getBaseSymbol, chainId)
                    .eq(WalletAddress::getWalletId, walletId).gt(WalletAddress::getId, start).orderByAsc(WalletAddress::getId).last(" limit " + limit).list();

            if (CollectionUtils.isEmpty(walletAddresses)) {
                break;
            }
            for (WalletAddress walletAddress : walletAddresses) {
                initCoinBalance(chainId, walletAddress.getAddress());
            }
        } while (true);
    }

    public void upsert(CoinBalance coinBalance) {
        this.baseMapper.upsert(coinBalance);
    }

}
