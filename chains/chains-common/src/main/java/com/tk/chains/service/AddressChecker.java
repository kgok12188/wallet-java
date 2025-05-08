package com.tk.chains.service;

import com.google.common.collect.Lists;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.service.AddressService;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class AddressChecker {

    @Resource
    private WalletAddressService walletAddressService;

    @Resource
    private WalletSymbolConfigService walletSymbolConfigService;
    @Autowired
    private SymbolConfigService symbolConfigService;

    @SuppressWarnings("all")
    private BloomFilter<String> bloomFilter;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final HashSet<String> ids = new HashSet<>();

    public AddressChecker(@Value("${chainIds:}") String chainIds) {
        String[] split = chainIds.split(",");
        for (String chain : split) {
            if (StringUtils.isNotBlank(chain)) {
                ids.add(chain);
            }
        }
    }


    @SuppressWarnings("all")
    public boolean owner(String fromAddress, String toAddress, String chainId, String hash) {
        readLock.lock();
        boolean ret;
        try {
            ret = bloomFilter.mightContain(fromAddress) || bloomFilter.mightContain(toAddress);
        } finally {
            readLock.unlock();
        }
        if (ret) {
            List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().in(WalletAddress::getAddress, Lists.newArrayList(fromAddress, toAddress))//
                    .eq(WalletAddress::getBaseSymbol, chainId).list();
            return CollectionUtils.isNotEmpty(walletAddresses);
        } else {
            return false;
        }
    }

    @SuppressWarnings("all")
    public boolean owner(String address, String chainId) {
        readLock.lock();
        boolean ret;
        try {
            ret = bloomFilter.mightContain(address);
        } finally {
            readLock.unlock();
        }
        if (ret) {
            List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, address).eq(WalletAddress::getBaseSymbol, chainId).list();
            return CollectionUtils.isNotEmpty(walletAddresses);
        }
        return false;
    }

    /**
     * 类似 btc 模型 找零
     *
     * @param chainId     链id
     * @param fromAddress 地址
     * @return 对应商户的找零地址
     */
    public String getChangeAddress(String chainId, String fromAddress) {
        WalletAddress walletAddress = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, fromAddress).one();
        if (walletAddress == null) {
            throw new IllegalArgumentException("无法获得商户信息" + chainId + "," + fromAddress);
        }
        SymbolConfig symbolConfig = symbolConfigService.lambdaQuery().eq(SymbolConfig::getBaseSymbol, chainId).one();
        if (symbolConfig == null) {
            throw new IllegalArgumentException("无法获得商户信息" + chainId + "," + fromAddress);
        }
        Integer walletId = walletAddress.getWalletId();
        Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery().eq(WalletSymbolConfig::getWalletId, walletId).eq(WalletSymbolConfig::getSymbolConfigId, symbolConfig.getId()).isNotNull(WalletSymbolConfig::getAggAddress).last(" limit 1").oneOpt();
        if (optionalWalletSymbolConfig.isPresent()) {
            return optionalWalletSymbolConfig.get().getAggAddress();
        }
        return fromAddress;
    }

    @PostConstruct
    @SuppressWarnings("all")
    public void loadAddress() {
        bloomFilter = BloomFilter.create(Funnels.unencodedCharsFunnel(), 10000000, 0.025);
        Thread thread = new Thread(() -> {
            long start = 0;
            while (!Thread.interrupted()) {
                try {
                    List<WalletAddress> list = walletAddressService.lambdaQuery().gt(WalletAddress::getId, start).in(WalletAddress::getBaseSymbol, ids)//
                            .orderByAsc(WalletAddress::getId).last("limit 100").list();
                    if (CollectionUtils.isNotEmpty(list)) {
                        writeLock.lock();
                        try {
                            for (WalletAddress walletAddress : list) {
                                start = walletAddress.getId();
                                bloomFilter.put(walletAddress.getAddress());
                            }
                        } finally {
                            writeLock.unlock();
                        }
                    } else {
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    //
                }
            }
        }, "loadAddress");
        thread.setDaemon(true);
        thread.start();
    }

}
