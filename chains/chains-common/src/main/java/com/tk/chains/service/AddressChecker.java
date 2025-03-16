package com.tk.chains.service;

import com.google.common.collect.Lists;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import org.apache.commons.collections.CollectionUtils;
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
     * @param chainId
     * @param fromAddress
     * @return
     */
    public String getChangeAddress(String chainId, String fromAddress) {
        Optional<WalletSymbolConfig> optionalWalletSymbolConfig = walletSymbolConfigService.lambdaQuery()
                .eq(WalletSymbolConfig::getBaseSymbol, chainId)
                .eq(WalletSymbolConfig::getAggPolice, 0)
                .isNotNull(WalletSymbolConfig::getAggAddress).last(" limit 1").oneOpt();
        if (optionalWalletSymbolConfig.isPresent()) {
            return optionalWalletSymbolConfig.get().getAggAddress();
        }
        return fromAddress;
    }
}
