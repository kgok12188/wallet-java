package com.tk.chains.service;

import com.google.common.collect.Lists;
import com.tk.wallet.common.entity.SymbolConfig;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.service.SymbolConfigService;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
public class AddressChecker {

    @Resource
    private WalletAddressService walletAddressService;

    @Resource
    private WalletSymbolConfigService walletSymbolConfigService;
    @Autowired
    private SymbolConfigService symbolConfigService;


    public boolean owner(String fromAddress, String toAddress, String chainId, String hash) {
        List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().in(WalletAddress::getAddress,
                        Lists.newArrayList(fromAddress, toAddress))
                .eq(WalletAddress::getBaseSymbol, chainId).list();
        return CollectionUtils.isNotEmpty(walletAddresses);
    }

    public boolean owner(String address, String chainId) {
        List<WalletAddress> walletAddresses = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, address).eq(WalletAddress::getBaseSymbol, chainId).list();
        return CollectionUtils.isNotEmpty(walletAddresses);
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
        Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                .eq(WalletSymbolConfig::getWalletId, walletId)
                .eq(WalletSymbolConfig::getSymbolConfigId, symbolConfig.getId())
                .isNotNull(WalletSymbolConfig::getAggAddress).last(" limit 1").oneOpt();
        if (optionalWalletSymbolConfig.isPresent()) {
            return optionalWalletSymbolConfig.get().getAggAddress();
        }
        return fromAddress;
    }

}
